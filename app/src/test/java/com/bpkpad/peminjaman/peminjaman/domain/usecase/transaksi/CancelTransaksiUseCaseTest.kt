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

class CancelTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: CancelTransaksiUseCase

    @Before
    fun setUp() {
        useCase = CancelTransaksiUseCase(transaksiRepo, auditRepo)
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
    fun `WB-UC-32 Transaction status cannot be cancelled returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, userId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Transaksi tidak dapat dibatalkan pada status ini", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-33 Cancel success from MENUNGGU_PERSETUJUAN`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.MENUNGGU_PERSETUJUAN)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.cancel(2, 1) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, userId = 1)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.cancel(2, 1)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DIBATALKAN,
                detail = "Transaksi dibatalkan. Status sebelumnya: Menunggu Persetujuan"
            )
        }
    }

    @Test
    fun `WB-UC-34 Cancel success from DISETUJUI`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DISETUJUI)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.cancel(2, 1) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, userId = 1)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.cancel(2, 1)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DIBATALKAN,
                detail = "Transaksi dibatalkan. Status sebelumnya: Disetujui"
            )
        }
    }
}
