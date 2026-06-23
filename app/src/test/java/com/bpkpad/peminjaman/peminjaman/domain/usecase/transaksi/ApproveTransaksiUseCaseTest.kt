package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ApproveTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: ApproveTransaksiUseCase

    @Before
    fun setUp() {
        useCase = ApproveTransaksiUseCase(transaksiRepo, auditRepo)
    }

    private fun createBaseTransaksi(status: TransaksiStatus) = Transaksi(
        id = 2,
        namaInstansi = "Dinas Pendidikan Balangan",
        picNama = "Pak Joko",
        picNoHp = "081298765432",
        nomorSuratPengantar = "400/Disdik/2026",
        fotoSuratPengantarPath = "path/to/foto.jpg",
        qrCodeToken = null,
        tanggalPinjam = LocalDate.of(2026, 6, 23),
        tanggalKembaliRencana = LocalDate.of(2026, 6, 30),
        tanggalKembaliAktual = null,
        status = status,
        metodePersetujuan = null,
        buktiBypassPath = null,
        catatanBypass = null,
        isBypassAcknowledged = false,
        alasanPenolakan = null,
        createdBy = 1,
        namaCreatedBy = "Budi Santoso",
        approvedBy = null,
        namaApprovedBy = null,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `WB-UC-18 Transaction not found returns error`() = runTest {
        coEvery { transaksiRepo.getById(99) } returns null

        val result = useCase(transaksiId = 99, approverId = 2, catatan = "ACC")
        assertTrue(result is ResultState.Error)
        assertEquals("Transaksi tidak ditemukan", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-19 Transaction status is not pending approval returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, approverId = 2, catatan = "ACC")
        assertTrue(result is ResultState.Error)
        assertEquals("Status transaksi tidak valid untuk disetujui", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-20 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.MENUNGGU_PERSETUJUAN)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.approve(2, 2, any()) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, approverId = 2, catatan = "ACC")
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.approve(2, 2, any())
            auditRepo.log(
                transaksiId = 2,
                userId = 2,
                aksi = AuditAction.DISETUJUI_ONLINE,
                detail = any(),
                catatan = "ACC"
            )
        }
    }
}
