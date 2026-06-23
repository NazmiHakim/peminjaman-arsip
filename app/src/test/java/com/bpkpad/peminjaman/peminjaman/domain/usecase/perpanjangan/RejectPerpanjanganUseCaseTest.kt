package com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RejectPerpanjanganUseCaseTest {

    private val perpanjanganRepo: PerpanjanganRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: RejectPerpanjanganUseCase

    private val kasubag = User(
        id = 2,
        username = "siti",
        namaLengkap = "Siti Aminah",
        nip = "654321",
        role = UserRole.KASUBAG,
        noHp = "081298765432",
        isActive = true
    )

    @Before
    fun setUp() {
        useCase = RejectPerpanjanganUseCase(perpanjanganRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(2) } returns kasubag
    }

    @Test
    fun `WB-UC-52 Reason blank returns error`() = runTest {
        val result = useCase(perpanjanganId = 1, kasubagId = 2, transaksiId = 7, alasan = "")
        assertTrue(result is ResultState.Error)
        assertEquals("Alasan penolakan wajib diisi", (result as ResultState.Error).message)
        coVerify(exactly = 0) { perpanjanganRepo.reject(any(), any(), any()) }
    }

    @Test
    fun `WB-UC-53 Success path`() = runTest {
        coEvery { perpanjanganRepo.reject(1, 2, "Dokumen masih dibutuhkan segera") } returns ResultState.Success(Unit)

        val result = useCase(
            perpanjanganId = 1,
            kasubagId = 2,
            transaksiId = 7,
            alasan = "Dokumen masih dibutuhkan segera"
        )
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            perpanjanganRepo.reject(1, 2, "Dokumen masih dibutuhkan segera")
            auditRepo.log(
                transaksiId = 7,
                userId = 2,
                aksi = AuditAction.PERPANJANGAN_DITOLAK,
                detail = "Perpanjangan ditolak: Dokumen masih dibutuhkan segera. Tenggat tidak berubah."
            )
        }
    }
}
