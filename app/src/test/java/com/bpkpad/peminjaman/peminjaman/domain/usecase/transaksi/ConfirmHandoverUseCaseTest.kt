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

class ConfirmHandoverUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: ConfirmHandoverUseCase

    @Before
    fun setUp() {
        useCase = ConfirmHandoverUseCase(transaksiRepo, auditRepo)
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
    fun `WB-UC-30 Transaction status is not approved returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, arsiparisId = 1)
        assertTrue(result is ResultState.Error)
        assertEquals("Transaksi harus berstatus Disetujui untuk konfirmasi serah", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-31 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DISETUJUI)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.confirmHandover(2, 1) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, arsiparisId = 1)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.confirmHandover(2, 1)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DOKUMEN_DISERAHKAN,
                detail = "Dokumen diserahkan ke peminjam. Status berubah: disetujui → dipinjam"
            )
        }
    }
}
