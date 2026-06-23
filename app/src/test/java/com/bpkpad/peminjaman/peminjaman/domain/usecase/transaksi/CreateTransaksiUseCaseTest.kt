package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CreateTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: CreateTransaksiUseCase

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
        useCase = CreateTransaksiUseCase(transaksiRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(1) } returns arsiparis
    }

    private fun createBaseTransaksi() = Transaksi(
        id = 0,
        namaInstansi = "Dinas Pendidikan Balangan",
        picNama = "Pak Joko",
        picNoHp = "081298765432",
        nomorSuratPengantar = "400/Disdik/2026",
        fotoSuratPengantarPath = "path/to/foto.jpg",
        qrCodeToken = null,
        tanggalPinjam = LocalDate.of(2026, 6, 23),
        tanggalKembaliRencana = LocalDate.of(2026, 6, 30),
        tanggalKembaliAktual = null,
        status = TransaksiStatus.MENUNGGU_PERSETUJUAN,
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
    fun `WB-UC-07 Instansi blank returns error`() = runTest {
        val tx = createBaseTransaksi().copy(namaInstansi = "")
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Unit kerja wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-08 PIC blank returns error`() = runTest {
        val tx = createBaseTransaksi().copy(picNama = "")
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Nama pemohon wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-09 Phone under ten digits returns error`() = runTest {
        val tx = createBaseTransaksi().copy(picNoHp = "08123")
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Nomor telepon harus terdiri dari 10–15 angka", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-10 Letter number blank returns error`() = runTest {
        val tx = createBaseTransaksi().copy(nomorSuratPengantar = "")
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Nomor surat wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-11 Cover photo path blank returns error`() = runTest {
        val tx = createBaseTransaksi().copy(fotoSuratPengantarPath = "")
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Foto surat pengantar wajib diambil", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-12 Return date is equal to or before loan date returns error`() = runTest {
        val tx = createBaseTransaksi().copy(
            tanggalPinjam = LocalDate.of(2026, 6, 23),
            tanggalKembaliRencana = LocalDate.of(2026, 6, 23)
        )
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Tanggal kembali harus lebih dari tanggal pinjam", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-13 No document selected returns error`() = runTest {
        val tx = createBaseTransaksi()
        val result = useCase(tx, emptyList())
        assertTrue(result is ResultState.Error)
        assertEquals("Pilih minimal 1 dokumen", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-14 BYPASS method with blank proof path returns error`() = runTest {
        val tx = createBaseTransaksi().copy(
            metodePersetujuan = MetodePersetujuan.BYPASS,
            buktiBypassPath = null,
            catatanBypass = "Memo Kasubag"
        )
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Foto bukti bypass wajib diupload", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-15 BYPASS method with blank notes returns error`() = runTest {
        val tx = createBaseTransaksi().copy(
            metodePersetujuan = MetodePersetujuan.BYPASS,
            buktiBypassPath = "path/to/proof.jpg",
            catatanBypass = ""
        )
        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Error)
        assertEquals("Catatan bypass wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-16 ONLINE method success path`() = runTest {
        val tx = createBaseTransaksi()
        val savedTx = tx.copy(id = 2)
        coEvery { transaksiRepo.create(tx, listOf(101)) } returns ResultState.Success(savedTx)

        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Success)
        assertEquals(savedTx, (result as ResultState.Success).data)

        coVerify(exactly = 1) {
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.TRANSAKSI_DIBUAT,
                detail = any()
            )
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.PENGAJUAN_DIKIRIM,
                detail = any()
            )
        }
    }

    @Test
    fun `WB-UC-17 BYPASS method success path`() = runTest {
        val tx = createBaseTransaksi().copy(
            metodePersetujuan = MetodePersetujuan.BYPASS,
            buktiBypassPath = "path/to/proof.jpg",
            catatanBypass = "Memo Kasubag"
        )
        val savedTx = tx.copy(id = 3)
        coEvery { transaksiRepo.create(any(), listOf(101)) } returns ResultState.Success(savedTx)

        val result = useCase(tx, listOf(101))
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            auditRepo.log(
                transaksiId = 3,
                userId = 1,
                aksi = AuditAction.TRANSAKSI_DIBUAT,
                detail = any()
            )
            auditRepo.log(
                transaksiId = 3,
                userId = 1,
                aksi = AuditAction.DISETUJUI_BYPASS,
                detail = any()
            )
        }
    }
}
