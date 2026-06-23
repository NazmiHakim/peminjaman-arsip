package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FindTransaksiByQrTokenUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private lateinit var useCase: FindTransaksiByQrTokenUseCase

    @Before
    fun setUp() {
        useCase = FindTransaksiByQrTokenUseCase(transaksiRepo)
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
    fun `WB-UC-35 Token blank returns error`() = runTest {
        val result = useCase(token = "")
        assertTrue(result is ResultState.Error)
        assertEquals("Token QR tidak valid", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-36 Unknown token returns error`() = runTest {
        coEvery { transaksiRepo.findByQrToken("unknown_token") } returns null

        val result = useCase(token = "unknown_token")
        assertTrue(result is ResultState.Error)
        assertEquals("Tidak ditemukan transaksi dengan QR token ini", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-37 Transaction status DIKEMBALIKAN returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIKEMBALIKAN)
        coEvery { transaksiRepo.findByQrToken("valid_token") } returns tx

        val result = useCase(token = "valid_token")
        assertTrue(result is ResultState.Error)
        assertEquals("QR token sudah tidak aktif karena transaksi sudah dikembalikan", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-38 Transaction status DIBATALKAN returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIBATALKAN)
        coEvery { transaksiRepo.findByQrToken("valid_token") } returns tx

        val result = useCase(token = "valid_token")
        assertTrue(result is ResultState.Error)
        assertEquals("QR token tidak aktif untuk transaksi terminal", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-39 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.findByQrToken("valid_token") } returns tx

        val result = useCase(token = "valid_token")
        assertTrue(result is ResultState.Success)
        assertEquals(tx, (result as ResultState.Success).data)
    }
}
