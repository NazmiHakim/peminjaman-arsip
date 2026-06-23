package com.bpkpad.peminjaman.auth.domain.usecase

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val auditLogRepository: AuditLogRepository = mockk(relaxed = true)
    private val loginUseCase = LoginUseCase(userRepository, sessionManager, auditLogRepository)

    @Test
    fun `WB-UC-01 Username blank returns error`() = runTest {
        val result = loginUseCase(username = "", password = "password123")
        assertTrue(result is ResultState.Error)
        assertEquals("Username tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { userRepository.login(any(), any()) }
    }

    @Test
    fun `WB-UC-02 Password blank returns error`() = runTest {
        val result = loginUseCase(username = "budi", password = "")
        assertTrue(result is ResultState.Error)
        assertEquals("Password tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { userRepository.login(any(), any()) }
    }

    @Test
    fun `WB-UC-03 Incorrect credentials returns error`() = runTest {
        coEvery { userRepository.login("budi", "wrong_password") } returns ResultState.Error("Username atau password salah")

        val result = loginUseCase(username = "budi", password = "wrong_password")
        assertTrue(result is ResultState.Error)
        assertEquals("Username atau password salah", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-04 Inactive account returns error`() = runTest {
        val inactiveUser = User(
            id = 1,
            username = "budi",
            namaLengkap = "Budi Santoso",
            nip = null,
            role = UserRole.ARSIPARIS,
            noHp = null,
            isActive = false
        )
        coEvery { userRepository.login("budi", "budi123") } returns ResultState.Success(inactiveUser)

        val result = loginUseCase(username = "budi", password = "budi123")
        assertTrue(result is ResultState.Error)
        assertEquals("Akun tidak aktif. Hubungi administrator.", (result as ResultState.Error).message)
        coVerify(exactly = 0) { sessionManager.saveSession(any()) }
    }

    @Test
    fun `WB-UC-05 Login success - role ARSIPARIS`() = runTest {
        val activeUser = User(
            id = 1,
            username = "budi",
            namaLengkap = "Budi Santoso",
            nip = "123456",
            role = UserRole.ARSIPARIS,
            noHp = "081234567890",
            isActive = true
        )
        coEvery { userRepository.login("budi", "budi123") } returns ResultState.Success(activeUser)

        val result = loginUseCase(username = "budi", password = "budi123")
        assertTrue(result is ResultState.Success)
        assertEquals(activeUser, (result as ResultState.Success).data)

        val expectedSession = SessionObject(
            userId = activeUser.id,
            username = activeUser.username,
            namaLengkap = activeUser.namaLengkap,
            role = activeUser.role,
            noHp = activeUser.noHp
        )
        coVerify(exactly = 1) { sessionManager.saveSession(expectedSession) }
        coVerify(exactly = 1) {
            auditLogRepository.log(
                transaksiId = null,
                userId = activeUser.id,
                aksi = AuditAction.LOGIN,
                detail = any()
            )
        }
    }

    @Test
    fun `WB-UC-06 Login success - role KASUBAG`() = runTest {
        val activeUser = User(
            id = 2,
            username = "siti",
            namaLengkap = "Siti Aminah",
            nip = "654321",
            role = UserRole.KASUBAG,
            noHp = "081298765432",
            isActive = true
        )
        coEvery { userRepository.login("siti", "siti123") } returns ResultState.Success(activeUser)

        val result = loginUseCase(username = "siti", password = "siti123")
        assertTrue(result is ResultState.Success)
        assertEquals(activeUser, (result as ResultState.Success).data)
        assertEquals(UserRole.KASUBAG, result.data.role)
    }
}
