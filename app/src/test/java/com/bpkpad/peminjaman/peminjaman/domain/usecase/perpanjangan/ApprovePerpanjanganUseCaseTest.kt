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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApprovePerpanjanganUseCaseTest {

    private val perpanjanganRepo: PerpanjanganRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: ApprovePerpanjanganUseCase

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
        useCase = ApprovePerpanjanganUseCase(perpanjanganRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(2) } returns kasubag
    }

    @Test
    fun `WB-UC-51 Success path`() = runTest {
        coEvery { perpanjanganRepo.approve(1, 2) } returns ResultState.Success(Unit)

        val result = useCase(perpanjanganId = 1, kasubagId = 2, transaksiId = 7)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            perpanjanganRepo.approve(1, 2)
            auditRepo.log(
                transaksiId = 7,
                userId = 2,
                aksi = AuditAction.PERPANJANGAN_DISETUJUI,
                detail = "Perpanjangan masa pinjam disetujui. Tenggat diperbarui."
            )
        }
    }
}
