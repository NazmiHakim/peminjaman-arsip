package com.bpkpad.peminjaman.auth.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-bound offline credential verifier.
 *
 * The password is never stored. A salted PBKDF2 verifier and timestamp are
 * encrypted using a non-exportable Android Keystore key.
 */
@Singleton
class OfflineAuthStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun saveSuccessfulOnlineLogin(username: String, password: String) {
        val salt = ByteArray(SALT_SIZE).also(secureRandom::nextBytes)
        val verifier = deriveVerifier(password, salt)
        val payload = listOf(
            normalizeUsername(username),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(verifier, Base64.NO_WRAP),
            System.currentTimeMillis().toString()
        ).joinToString(SEPARATOR)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_PAYLOAD, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun verify(username: String, password: String): Boolean = runCatching {
        val iv = preferences.getString(KEY_IV, null) ?: return false
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: return false
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP))
        )
        val parts = cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP))
            .toString(StandardCharsets.UTF_8)
            .split(SEPARATOR)
        if (parts.size != 4) return false

        val salt = Base64.decode(parts[1], Base64.NO_WRAP)
        val expectedVerifier = Base64.decode(parts[2], Base64.NO_WRAP)
        val authenticatedAt = parts[3].toLong()
        val notExpired = System.currentTimeMillis() - authenticatedAt <= MAX_OFFLINE_AGE_MILLIS
        val usernameMatches = parts[0] == normalizeUsername(username)
        val verifierMatches = MessageDigest.isEqual(
            expectedVerifier,
            deriveVerifier(password, salt)
        )
        notExpired && usernameMatches && verifierMatches
    }.getOrElse {
        clear()
        false
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun deriveVerifier(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = runCatching {
                SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            }.getOrElse {
                // Android 7 devices may not expose the SHA-256 PBKDF2 provider.
                SecretKeyFactory.getInstance(PBKDF2_FALLBACK_ALGORITHM)
            }
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun normalizeUsername(username: String): String =
        username.trim().substringBefore('@').lowercase()

    private companion object {
        const val PREFERENCES_NAME = "offline_auth"
        const val KEY_IV = "iv"
        const val KEY_PAYLOAD = "payload"
        const val KEY_ALIAS = "bpkpad_offline_auth_key"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1"
        const val PBKDF2_ITERATIONS = 210_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_SIZE = 32
        const val GCM_TAG_LENGTH = 128
        const val SEPARATOR = "|"
        const val MAX_OFFLINE_AGE_MILLIS = 72L * 60L * 60L * 1000L
    }
}
