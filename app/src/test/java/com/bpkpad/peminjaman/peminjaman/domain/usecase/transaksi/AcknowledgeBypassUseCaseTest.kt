package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan
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

class AcknowledgeBypassUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: AcknowledgeBypassUseCase

    @Before
    fun setUp() {
        useCase = AcknowledgeBypassUseCase(transaksiRepo, auditRepo)
    }

    private fun createBaseTransaksi(metode: MetodePersetujuan?, isAcknowledged: Boolean) = Transaksi(
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
        status = TransaksiStatus.DISETUJUI,
        metodePersetujuan = metode,
        buktiBypassPath = null,
        catatanBypass = null,
        isBypassAcknowledged = isAcknowledged,
        alasanPenolakan = null,
        createdBy = 1,
        namaCreatedBy = "Budi Santoso",
        approvedBy = null,
        namaApprovedBy = null,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `WB-UC-28 Transaction does not need bypass acknowledge returns error`() = runTest {
        val tx = createBaseTransaksi(metode = MetodePersetujuan.ONLINE, isAcknowledged = false)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, kasubagId = 2)
        assertTrue(result is ResultState.Error)
        assertEquals("Transaksi ini tidak perlu verifikasi bypass", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-29 Success path`() = runTest {
        val tx = createBaseTransaksi(metode = MetodePersetujuan.BYPASS, isAcknowledged = false)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.acknowledgeBypass(2, 2) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, kasubagId = 2)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.acknowledgeBypass(2, 2)
            auditRepo.log(
                transaksiId = 2,
                userId = 2,
                aksi = AuditAction.BYPASS_DIAKUI_KASUBAG,
                detail = "Kasubag mengakui bypass persetujuan fisik"
            )
        }
    }
}
