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

class ReturnTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private lateinit var useCase: ReturnTransaksiUseCase

    @Before
    fun setUp() {
        useCase = ReturnTransaksiUseCase(transaksiRepo, auditRepo)
    }

    private fun createBaseTransaksi(status: TransaksiStatus) = Transaksi(
        id = 2,
        namaInstansi = "Dinas Pendidikan Balangan",
        picNama = "Pak Joko",
        picNoHp = "081298765432",
        nomorSuratPengantar = "400/Disdik/2026",
        fotoSuratPengantarPath = "path/to/foto.jpg",
        qrCodeToken = "valid_token",
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
    fun `WB-UC-40 Transaction status is not borrowed returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DISETUJUI)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = emptyMap())
        assertTrue(result is ResultState.Error)
        assertEquals("Hanya transaksi dipinjam yang dapat dikembalikan", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-41 Return with RUSAK condition without notes returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val kondisiMap = mapOf(1 to Pair("rusak", ""))
        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = kondisiMap)
        assertTrue(result is ResultState.Error)
        assertEquals("Catatan kondisi wajib diisi untuk dokumen rusak/hilang", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-42 Return with HILANG condition without notes returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val kondisiMap = mapOf(1 to Pair("hilang", null))
        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = kondisiMap)
        assertTrue(result is ResultState.Error)
        assertEquals("Catatan kondisi wajib diisi untuk dokumen rusak/hilang", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-43 Success with all conditions BAIK triggers BAIK audit`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx
        val kondisiMap = mapOf(
            1 to Pair("baik", null as String?),
            2 to Pair("baik", null as String?)
        )
        coEvery { transaksiRepo.returnTransaksi(2, 1, kondisiMap) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = kondisiMap)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.returnTransaksi(2, 1, kondisiMap)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DIKEMBALIKAN_BAIK,
                detail = "Dokumen dikembalikan. 2 dokumen diproses.",
                catatan = null
            )
        }
    }

    @Test
    fun `WB-UC-44 Success with one RUSAK triggers RUSAK audit`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx
        val kondisiMap = mapOf(
            1 to Pair("baik", null),
            2 to Pair("rusak", "Halaman robek")
        )
        coEvery { transaksiRepo.returnTransaksi(2, 1, kondisiMap) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = kondisiMap)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.returnTransaksi(2, 1, kondisiMap)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DIKEMBALIKAN_RUSAK,
                detail = "Dokumen dikembalikan. 2 dokumen diproses.",
                catatan = "Rusak: Halaman robek"
            )
        }
    }

    @Test
    fun `WB-UC-45 Success with HILANG and RUSAK prioritizes HILANG audit`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx
        val kondisiMap = mapOf(
            1 to Pair("rusak", "Sampul rusak"),
            2 to Pair("hilang", "Dokumen hilang")
        )
        coEvery { transaksiRepo.returnTransaksi(2, 1, kondisiMap) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, arsiparisId = 1, kondisiMap = kondisiMap)
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.returnTransaksi(2, 1, kondisiMap)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DIKEMBALIKAN_HILANG,
                detail = "Dokumen dikembalikan. 2 dokumen diproses.",
                catatan = "Rusak: Sampul rusak\nHilang: Dokumen hilang"
            )
        }
    }
}
