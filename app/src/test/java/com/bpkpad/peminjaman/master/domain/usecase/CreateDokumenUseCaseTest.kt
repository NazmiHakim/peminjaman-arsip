package com.bpkpad.peminjaman.master.domain.usecase

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateDokumenUseCaseTest {

    private val dokumenRepo: MasterDokumenRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: CreateDokumenUseCase

    @Before
    fun setUp() {
        useCase = CreateDokumenUseCase(dokumenRepo, auditRepo)
    }

    private fun createBaseDokumen() = MasterDokumen(
        id = 0,
        nomorDokumen = "SP2D-2023-001",
        perihal = "SP2D Belanja Modal Dindik",
        nominal = 15000000.0,
        tahun = "2023",
        jenisDokumen = "SP2D",
        status = DokumenStatus.TERSEDIA,
        lokasiRak = "Rak A",
        lokasiBox = "Box 2023-01"
    )

    @Test
    fun `WB-UC-54 Nomor dokumen blank returns error`() = runTest {
        val doc = createBaseDokumen().copy(nomorDokumen = "")
        val result = useCase(doc, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Nomor dokumen tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { dokumenRepo.create(any()) }
    }

    @Test
    fun `WB-UC-55 Perihal blank returns error`() = runTest {
        val doc = createBaseDokumen().copy(perihal = "")
        val result = useCase(doc, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Perihal tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { dokumenRepo.create(any()) }
    }

    @Test
    fun `WB-UC-56 Tahun blank returns error`() = runTest {
        val doc = createBaseDokumen().copy(tahun = "")
        val result = useCase(doc, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Tahun tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { dokumenRepo.create(any()) }
    }

    @Test
    fun `WB-UC-57 Jenis dokumen blank returns error`() = runTest {
        val doc = createBaseDokumen().copy(jenisDokumen = "")
        val result = useCase(doc, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Jenis dokumen tidak boleh kosong", (result as ResultState.Error).message)
        coVerify(exactly = 0) { dokumenRepo.create(any()) }
    }

    @Test
    fun `WB-UC-58 Success path`() = runTest {
        val doc = createBaseDokumen()
        val savedDoc = doc.copy(id = 101)
        coEvery { dokumenRepo.create(doc) } returns ResultState.Success(savedDoc)

        val result = useCase(doc, userId = 1)
        assertTrue(result is ResultState.Success)
        assertEquals(savedDoc, (result as ResultState.Success).data)

        coVerify(exactly = 1) {
            dokumenRepo.create(doc)
            auditRepo.log(
                transaksiId = 0,
                userId = 1,
                aksi = AuditAction.MASTER_DOKUMEN_DITAMBAH,
                detail = "Dokumen baru: SP2D-2023-001 - SP2D Belanja Modal Dindik (2023)"
            )
        }
    }
}
