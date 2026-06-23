package com.bpkpad.peminjaman.master.domain.usecase

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateInstansiUseCaseTest {

    private val instansiRepo: InstansiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: CreateInstansiUseCase

    private val arsiparis = User(
        id = 1,
        username = "budi",
        namaLengkap = "Budi Santoso",
        nip = "123",
        role = UserRole.ARSIPARIS,
        noHp = "081234567890",
        isActive = true
    )

    @Before
    fun setUp() {
        useCase = CreateInstansiUseCase(instansiRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(1) } returns arsiparis
    }

    private fun createBaseInstansi() = Instansi(
        id = 0,
        namaInstansi = "Dinas Sosial",
        alamat = "Jl. Merdeka No. 10",
        kodeInstansi = "DINSOS"
    )

    @Test
    fun `WB-UC-59 Nama instansi blank returns error`() = runTest {
        val instansi = createBaseInstansi().copy(namaInstansi = "")
        val result = useCase(instansi, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Nama instansi wajib diisi", (result as ResultState.Error).message)
        coVerify(exactly = 0) { instansiRepo.create(any()) }
    }

    @Test
    fun `WB-UC-60 Success path`() = runTest {
        val instansi = createBaseInstansi()
        val normalized = instansi.copy(
            namaInstansi = "Dinas Sosial",
            kodeInstansi = "DINSOS",
            alamat = "Jl. Merdeka No. 10"
        )
        val savedInstansi = normalized.copy(id = 1)
        coEvery { instansiRepo.create(normalized) } returns ResultState.Success(savedInstansi)

        val result = useCase(instansi, userId = 1)
        assertTrue(result is ResultState.Success)
        assertEquals(savedInstansi, (result as ResultState.Success).data)

        coVerify(exactly = 1) {
            instansiRepo.create(normalized)
            auditRepo.log(
                transaksiId = null,
                userId = 1,
                aksi = AuditAction.MASTER_INSTANSI_DITAMBAH,
                detail = "Instansi baru ditambahkan: Dinas Sosial"
            )
        }
    }
}
