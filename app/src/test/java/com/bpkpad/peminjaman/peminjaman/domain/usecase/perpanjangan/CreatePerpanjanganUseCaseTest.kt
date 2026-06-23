package com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.PerpanjanganStatus
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
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

class CreatePerpanjanganUseCaseTest {

    private val perpanjanganRepo: PerpanjanganRepository = mockk()
    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: CreatePerpanjanganUseCase

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
        useCase = CreatePerpanjanganUseCase(perpanjanganRepo, transaksiRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(1) } returns arsiparis
    }

    private fun createBasePerpanjangan() = Perpanjangan(
        id = 0,
        transaksiId = 2,
        tanggalKembaliLama = LocalDate.of(2026, 6, 30),
        tanggalKembaliBaru = LocalDate.of(2026, 7, 7),
        fotoSuratPerpanjanganPath = "path/to/surat.jpg",
        alasan = "Pemeriksaan diperpanjang",
        status = PerpanjanganStatus.PENDING,
        alasanPenolakan = null,
        createdBy = 1,
        approvedBy = null,
        createdAt = System.currentTimeMillis()
    )

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
    fun `WB-UC-46 Photo path blank returns error`() = runTest {
        val extension = createBasePerpanjangan().copy(fotoSuratPerpanjanganPath = "")
        val result = useCase(extension)
        assertTrue(result is ResultState.Error)
        assertEquals("Foto surat perpanjangan wajib diupload", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-47 Reason blank returns error`() = runTest {
        val extension = createBasePerpanjangan().copy(alasan = "")
        val result = useCase(extension)
        assertTrue(result is ResultState.Error)
        assertEquals("Alasan perpanjangan wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-48 New return date is not after current due date returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val extension = createBasePerpanjangan().copy(tanggalKembaliBaru = LocalDate.of(2026, 6, 30))
        val result = useCase(extension)
        assertTrue(result is ResultState.Error)
        assertEquals("Tanggal kembali baru harus setelah tenggat saat ini", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-49 Transaction status is not DIPINJAM returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DISETUJUI)
        coEvery { transaksiRepo.getById(2) } returns tx

        val extension = createBasePerpanjangan()
        val result = useCase(extension)
        assertTrue(result is ResultState.Error)
        assertEquals("Perpanjangan hanya untuk transaksi yang sedang dipinjam", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-50 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        val extension = createBasePerpanjangan()
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { perpanjanganRepo.create(extension) } returns ResultState.Success(extension.copy(id = 1))

        val result = useCase(extension)
        assertTrue(result is ResultState.Success)
        assertEquals(1, (result as ResultState.Success).data.id)

        coVerify(exactly = 1) {
            perpanjanganRepo.create(extension)
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.PERPANJANGAN_DIAJUKAN,
                detail = "Perpanjangan diajukan. Tenggat baru: 2026-07-07"
            )
        }
    }
}
