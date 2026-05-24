package com.bpkpad.peminjaman.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.SESSION_DATASTORE
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_ID = intPreferencesKey("user_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_NAMA = stringPreferencesKey("nama_lengkap")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_NO_HP = stringPreferencesKey("no_hp")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val session: Flow<SessionObject?> = dataStore.data.map { prefs ->
        val isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false
        if (!isLoggedIn) return@map null
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val username = prefs[KEY_USERNAME] ?: return@map null
        val nama = prefs[KEY_NAMA] ?: return@map null
        val roleStr = prefs[KEY_ROLE] ?: return@map null
        val role = try { UserRole.valueOf(roleStr.uppercase()) } catch (e: Exception) { return@map null }
        SessionObject(
            userId = userId,
            username = username,
            namaLengkap = nama,
            role = role,
            noHp = prefs[KEY_NO_HP]
        )
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    suspend fun saveSession(session: SessionObject) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_NAMA] = session.namaLengkap
            prefs[KEY_ROLE] = session.role.name
            session.noHp?.let { prefs[KEY_NO_HP] = it }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}
