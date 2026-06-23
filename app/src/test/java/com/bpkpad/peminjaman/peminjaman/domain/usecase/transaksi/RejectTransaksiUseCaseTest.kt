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

class RejectTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: RejectTransaksiUseCase

    @Before
    fun setUp() {
        useCase = RejectTransaksiUseCase(transaksiRepo, auditRepo)
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
    fun `WB-UC-21 Reason blank returns error`() = runTest {
        val result = useCase(transaksiId = 2, approverId = 2, alasan = "")
        assertTrue(result is ResultState.Error)
        assertEquals("Alasan penolakan wajib diisi", (result as ResultState.Error).message)
        coVerify(exactly = 0) { transaksiRepo.getById(any()) }
    }

    @Test
    fun `WB-UC-22 Transaction status not pending approval returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, approverId = 2, alasan = "Surat buram")
        assertTrue(result is ResultState.Error)
        assertEquals("Hanya transaksi menunggu persetujuan yang dapat ditolak", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-23 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.MENUNGGU_PERSETUJUAN)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.reject(2, 2, "Surat buram") } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, approverId = 2, alasan = "Surat buram")
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.reject(2, 2, "Surat buram")
            auditRepo.log(
                transaksiId = 2,
                userId = 2,
                aksi = AuditAction.DITOLAK,
                detail = "Pengajuan ditolak: Surat buram"
            )
        }
    }
}
