package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
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

class BypassTransaksiUseCaseTest {

    private val transaksiRepo: TransaksiRepository = mockk()
    private val auditRepo: AuditLogRepository = mockk(relaxed = true)
    private val userRepo: UserRepository = mockk()
    private lateinit var useCase: BypassTransaksiUseCase

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
        useCase = BypassTransaksiUseCase(transaksiRepo, auditRepo, userRepo)
        coEvery { userRepo.getUserById(1) } returns arsiparis
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
    fun `WB-UC-24 Bypass proof blank returns error`() = runTest {
        val result = useCase(transaksiId = 2, arsiparisId = 1, buktiPath = "", catatan = "Memo Kasubag")
        assertTrue(result is ResultState.Error)
        assertEquals("Foto bukti bypass wajib diupload", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-25 Bypass notes blank returns error`() = runTest {
        val result = useCase(transaksiId = 2, arsiparisId = 1, buktiPath = "path.jpg", catatan = "")
        assertTrue(result is ResultState.Error)
        assertEquals("Catatan bypass wajib diisi", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-26 Transaction status is not pending approval returns error`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.DIPINJAM)
        coEvery { transaksiRepo.getById(2) } returns tx

        val result = useCase(transaksiId = 2, arsiparisId = 1, buktiPath = "path.jpg", catatan = "Memo Kasubag")
        assertTrue(result is ResultState.Error)
        assertEquals("Hanya transaksi menunggu persetujuan yang dapat di-bypass", (result as ResultState.Error).message)
    }

    @Test
    fun `WB-UC-27 Success path`() = runTest {
        val tx = createBaseTransaksi(TransaksiStatus.MENUNGGU_PERSETUJUAN)
        coEvery { transaksiRepo.getById(2) } returns tx
        coEvery { transaksiRepo.bypass(2, 1, "path.jpg", "Memo Kasubag", any()) } returns ResultState.Success(Unit)

        val result = useCase(transaksiId = 2, arsiparisId = 1, buktiPath = "path.jpg", catatan = "Memo Kasubag")
        assertTrue(result is ResultState.Success)

        coVerify(exactly = 1) {
            transaksiRepo.bypass(2, 1, "path.jpg", "Memo Kasubag", any())
            auditRepo.log(
                transaksiId = 2,
                userId = 1,
                aksi = AuditAction.DISETUJUI_BYPASS,
                detail = "Bypass persetujuan dengan bukti fisik. Catatan: Memo Kasubag"
            )
        }
    }
}
