package com.bpkpad.peminjaman.auth.data.repository

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.bpkpad.peminjaman.auth.data.security.OfflineAuthStore
import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.database.dao.UserDao
import com.bpkpad.peminjaman.core.database.entity.UserEntity
import com.bpkpad.peminjaman.core.network.NetworkMonitor
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.OptIn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import io.github.jan.supabase.auth.user.UserSession
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private val userDao: UserDao = mockk(relaxed = true)
    private val offlineAuthStore: OfflineAuthStore = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk()

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
        Dispatchers.setMain(UnconfinedTestDispatcher())
        clearAllMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun `WB-REPO-01 Supabase login success`() = runTest {
        // Setup real SupabaseClient with MockEngine
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "dummy-key"
        ) {
            install(Auth) {
                sessionManager = object : SessionManager {
                    override suspend fun loadSession(): UserSession? = null
                    override suspend fun saveSession(session: UserSession) {}
                    override suspend fun deleteSession() {}
                }
                codeVerifierCache = object : CodeVerifierCache {
                    override suspend fun saveCodeVerifier(codeVerifier: String) {}
                    override suspend fun loadCodeVerifier(): String? = null
                    override suspend fun deleteCodeVerifier() {}
                }
            }
            install(Postgrest)
            httpEngine = MockEngine { request ->
                val path = request.url.encodedPath
                println("WB-REPO-01 Ktor Request Path: $path")
                when {
                    "token" in path -> {
                        respond(
                            content = """
                                {
                                    "access_token": "dummy_access_token",
                                    "refresh_token": "dummy_refresh_token",
                                    "expires_in": 3600,
                                    "token_type": "bearer",
                                    "user": {
                                        "id": "uuid-123",
                                        "aud": "authenticated",
                                        "email": "budi@bpkpad-balangan.go.id"
                                    }
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
                        )
                    }
                    "loan_profiles" in path -> {
                        respond(
                            content = """
                                [
                                    {
                                        "legacy_id": 1,
                                        "username": "budi",
                                        "nama_lengkap": "Budi Santoso",
                                        "nip": "123",
                                        "role": "arsiparis",
                                        "no_hp": "081234567890",
                                        "is_active": true
                                    }
                                ]
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
                        )
                    }
                    else -> respond("{}", HttpStatusCode.NotFound)
                }
            }
        }

        val repository = UserRepositoryImpl(userDao, supabase, offlineAuthStore, networkMonitor)
        every { networkMonitor.isOnline() } returns true

        // Run
        val result = repository.login("budi", "password123")

        // Assert
        if (result is ResultState.Error) {
            println("ERROR CAUSE: ${result.cause}")
            println("ERROR MESSAGE: ${result.message}")
        }
        assertTrue(result is ResultState.Success)
        val user = (result as ResultState.Success).data
        assertEquals(1, user.id)
        assertEquals("budi", user.username)
        assertEquals(UserRole.ARSIPARIS, user.role)

        coVerify(exactly = 1) {
            userDao.upsert(any())
            offlineAuthStore.saveSuccessfulOnlineLogin("budi", "password123")
        }
    }

    @Test
    fun `WB-REPO-02 Supabase login failed`() = runTest {
        // Setup real SupabaseClient with MockEngine returning error
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "dummy-key"
        ) {
            install(Auth) {
                sessionManager = object : SessionManager {
                    override suspend fun loadSession(): UserSession? = null
                    override suspend fun saveSession(session: UserSession) {}
                    override suspend fun deleteSession() {}
                }
                codeVerifierCache = object : CodeVerifierCache {
                    override suspend fun saveCodeVerifier(codeVerifier: String) {}
                    override suspend fun loadCodeVerifier(): String? = null
                    override suspend fun deleteCodeVerifier() {}
                }
            }
            install(Postgrest)
            httpEngine = MockEngine { request ->
                val path = request.url.encodedPath
                println("WB-REPO-02 Ktor Request Path: $path")
                when {
                    "token" in path -> {
                        respond(
                            content = """
                                {
                                    "error": "email_provider_disabled",
                                    "error_description": "email_provider_disabled"
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.BadRequest,
                            headers = headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
                        )
                    }
                    else -> respond("{}", HttpStatusCode.NotFound)
                }
            }
        }

        val repository = UserRepositoryImpl(userDao, supabase, offlineAuthStore, networkMonitor)
        every { networkMonitor.isOnline() } returns true

        // Run
        val result = repository.login("budi", "password123")

        // Assert
        if (result is ResultState.Error) {
            println("WB-REPO-02 CAUSE: ${result.cause}")
            println("WB-REPO-02 MESSAGE: ${result.message}")
        }
        assertTrue(result is ResultState.Error)
        val errMsg = (result as ResultState.Error).message
        assertEquals("Login email belum diaktifkan pada Supabase Auth.", errMsg)
    }
}
