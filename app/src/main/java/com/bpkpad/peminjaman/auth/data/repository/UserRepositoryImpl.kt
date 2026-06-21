package com.bpkpad.peminjaman.auth.data.repository

import com.bpkpad.peminjaman.auth.data.security.OfflineAuthStore
import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.UserDao
import com.bpkpad.peminjaman.core.database.entity.UserEntity
import com.bpkpad.peminjaman.core.network.NetworkMonitor
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val supabase: SupabaseClient,
    private val offlineAuthStore: OfflineAuthStore,
    private val networkMonitor: NetworkMonitor
) : UserRepository {

    override suspend fun login(username: String, password: String): ResultState<User> {
        if (!networkMonitor.isOnline()) {
            return loginOffline(username, password)
        }
        return loginWithSupabase(username, password)
    }

    override suspend fun getAuthenticatedUser(): User? {
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return null
        return runCatching {
            val profile = supabase.from("loan_profiles")
                .select {
                    filter { eq("id", authUserId) }
                }
                .decodeSingle<ProfileDto>()
            if (!profile.isActive) {
                supabase.auth.signOut()
                return null
            }
            profile.cacheLocally()
            profile.toDomain()
        }.getOrNull()
    }

    override suspend fun getUserById(id: Int): User? {
        runCatching {
            return supabase.from("loan_profiles")
                .select {
                    filter { eq("legacy_id", id) }
                }
                .decodeSingle<ProfileDto>()
                .toDomain()
        }
        return userDao.getById(id)?.toDomain()
    }

    override suspend fun logout() {
        runCatching { supabase.auth.signOut() }
        offlineAuthStore.clear()
    }

    override suspend fun getAllUsers(): List<User> {
        return runCatching {
            supabase.from("loan_profiles")
                .select()
                .decodeList<ProfileDto>()
                .map(ProfileDto::toDomain)
        }.getOrDefault(emptyList())
    }

    private suspend fun loginWithSupabase(
        username: String,
        password: String
    ): ResultState<User> {
        return try {
            val email = username.toLoginEmail()
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val authUserId = supabase.auth.currentUserOrNull()?.id
                ?: return ResultState.Error("Sesi Supabase tidak tersedia")

            val profile = supabase.from("loan_profiles")
                .select {
                    filter { eq("id", authUserId) }
                }
                .decodeSingle<ProfileDto>()

            if (!profile.isActive) {
                supabase.auth.signOut()
                return ResultState.Error("Akun tidak aktif. Hubungi administrator.")
            }

            profile.cacheLocally()
            offlineAuthStore.saveSuccessfulOnlineLogin(username, password)
            ResultState.Success(profile.toDomain())
        } catch (e: Exception) {
            runCatching { supabase.auth.signOut() }
            if (e.isNetworkFailure() && offlineAuthStore.verify(username, password)) {
                return loginOffline(username, password)
            }
            ResultState.Error(e.toFriendlyLoginMessage(), e)
        }
    }

    private fun String.toLoginEmail(): String =
        if ('@' in this) trim().lowercase()
        else "${trim().lowercase()}@bpkpad-balangan.go.id"

    private suspend fun loginOffline(username: String, password: String): ResultState<User> {
        if (!offlineAuthStore.verify(username, password)) {
            return ResultState.Error(
                "Login offline tidak tersedia. Hubungkan internet dan login online terlebih dahulu."
            )
        }
        val cachedUser = userDao.getByUsername(username.substringBefore('@'))
            ?: return ResultState.Error("Profil offline tidak ditemukan")
        if (!cachedUser.isActive) return ResultState.Error("Akun offline tidak aktif")
        return ResultState.Success(cachedUser.toDomain())
    }

    private fun Throwable.toFriendlyLoginMessage(): String {
        val rawMessage = message.orEmpty().lowercase()
        return when {
            "email_provider_disabled" in rawMessage ->
                "Login email belum diaktifkan pada Supabase Auth."
            "invalid_credentials" in rawMessage ->
                "Username atau password salah."
            "email_not_confirmed" in rawMessage ->
                "Email akun belum dikonfirmasi oleh administrator."
            "user_banned" in rawMessage ->
                "Akun dinonaktifkan. Hubungi administrator."
            else ->
                "Login gagal. Periksa koneksi dan konfigurasi akun, lalu coba lagi."
        }
    }

    private fun Throwable.isNetworkFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException) return true
            current = current.cause
        }
        val rawMessage = message.orEmpty().lowercase()
        return listOf(
            "unable to resolve host",
            "failed to connect",
            "network is unreachable",
            "timeout",
            "connection reset"
        ).any(rawMessage::contains)
    }

    @Serializable
    private data class ProfileDto(
        @SerialName("legacy_id") val legacyId: Long,
        val username: String,
        @SerialName("nama_lengkap") val namaLengkap: String,
        val nip: String? = null,
        val role: String,
        @SerialName("no_hp") val noHp: String? = null,
        @SerialName("is_active") val isActive: Boolean
    ) {
        fun toDomain() = User(
            id = legacyId.toInt(),
            username = username,
            namaLengkap = namaLengkap,
            nip = nip,
            role = when (role) {
                "arsiparis" -> UserRole.ARSIPARIS
                "kasubag" -> UserRole.KASUBAG
                else -> error("Role pengguna tidak dikenali")
            },
            noHp = noHp,
            isActive = isActive
        )
    }

    private suspend fun ProfileDto.cacheLocally() {
        userDao.upsert(
            UserEntity(
                id = legacyId.toInt(),
                username = username,
                passwordHash = "",
                namaLengkap = namaLengkap,
                nip = nip,
                role = role,
                noHp = noHp,
                isActive = isActive
            )
        )
    }

    private fun UserEntity.toDomain() = User(
        id = id,
        username = username,
        namaLengkap = namaLengkap,
        nip = nip,
        role = if (role == "kasubag") UserRole.KASUBAG else UserRole.ARSIPARIS,
        noHp = noHp,
        isActive = isActive
    )
}
