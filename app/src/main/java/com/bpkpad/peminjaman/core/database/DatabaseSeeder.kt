package com.bpkpad.peminjaman.core.database

import android.util.Log
import com.bpkpad.peminjaman.core.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseSeeder
 * Populates the database with test data on first install (debug only).
 * Seeds all 7 test scenarios per AGENTS.md Section 14.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: AppDatabase
) {
    companion object {
        private const val TAG = "SEEDER"

        private fun hashPw(plain: String): String = "\$2a\$10\$${plain.hashCode()}dummyhash"
    }

    fun seedIfEmpty(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val userCount = db.userDao().count()
                if (userCount == 0) {
                    Log.d(TAG, "Database empty. Seeding all data...")
                    seedAll()
                    Log.d(TAG, "Seeding complete!")
                } else {
                    Log.d(TAG, "Database already seeded. Skipping. ($userCount users)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Seeding failed: ${e.message}", e)
            }
        }
    }

    private suspend fun seedAll() {
        seedUsers()
        seedInstansi()
        seedMasterDokumen()
        seedTransaksi()
        seedAuditLog()
    }

    private suspend fun seedUsers() {
        db.userDao().insertAll(listOf(
            UserEntity(1, "budi",   hashPw("budi123"), "Budi Santoso",         "198501012010011001", "arsiparis", "081234567890", true),
            UserEntity(2, "siti",   hashPw("siti123"), "Siti Aminah, S.AP",    "198002022005012002", "kasubag",   "081298765432", true),
            UserEntity(3, "andi",   hashPw("andi123"), "Andi Wijaya",          "199003032015011003", "arsiparis", "081355566677", true)
        ))
        Log.d(TAG, "Users seeded: 3 users")
    }

    private suspend fun seedInstansi() {
        db.instansiDao().insertAll(listOf(
            InstansiEntity(1, "Dinas Pendidikan Balangan",   "Jl. Merdeka No. 1, Paringin",   "DINDIK"),
            InstansiEntity(2, "Dinas Kesehatan Balangan",    "Jl. Sehat No. 2, Paringin",     "DINKES"),
            InstansiEntity(3, "Badan Perencanaan Daerah",    "Jl. Pembangunan No. 3",         "BAPPEDA"),
            InstansiEntity(4, "Inspektorat Daerah",          "Jl. Pengawasan No. 4",          "INSPEKTORAT"),
            InstansiEntity(5, "Dinas Pekerjaan Umum",        "Jl. Karya No. 5",               "DPU")
        ))
        Log.d(TAG, "Instansi seeded: 5 records")
    }

    private suspend fun seedMasterDokumen() {
        db.masterDokumenDao().insertAll(listOf(
            // DIPINJAM (Transaksi #1 Overdue)
            MasterDokumenEntity(101, "SP2D-2023-001", "SP2D Belanja Modal Dindik",          15000000.0, "2023", "SP2D",           "dipinjam",  "Rak A", "Box 2023-01"),
            // TERSEDIA
            MasterDokumenEntity(102, "SP2D-2023-002", "SP2D Belanja Jasa Dinkes",           25000000.0, "2023", "SP2D",           "tersedia",  "Rak A", "Box 2023-02"),
            MasterDokumenEntity(103, "SP-2024-055",   "Surat Perintah Tugas Bappeda",       0.0,        "2024", "Surat Perintah", "tersedia",  "Rak B", "Box 2024-01"),
            MasterDokumenEntity(104, "SPJ-2024-112",  "SPJ Perjalanan Dinas Inspektorat",   8500000.0,  "2024", "SPJ",            "tersedia",  "Rak C", "Box 2024-05"),
            MasterDokumenEntity(105, "SP2D-2024-088", "SP2D Belanja Pegawai DPU",           120000000.0,"2024", "SP2D",           "tersedia",  "Rak A", "Box 2024-08"),
            // DIPINJAM (Transaksi #4 Normal)
            MasterDokumenEntity(106, "SP2D-2025-012", "SP2D Belanja Barang Dinkes",         7500000.0,  "2025", "SP2D",           "dipinjam",  "Rak A", "Box 2025-01"),
            // RUSAK (riwayat)
            MasterDokumenEntity(107, "SP-2022-033",   "Surat Perintah Lama",                0.0,        "2022", "Surat Perintah", "rusak",     "Rak D", "Box 2022-03"),
            // HILANG (riwayat)
            MasterDokumenEntity(108, "SPJ-2021-007",  "SPJ Kegiatan 2021",                  3000000.0,  "2021", "SPJ",            "hilang",    "-",     "-"),
            // TERSEDIA
            MasterDokumenEntity(109, "SP2D-2025-025", "SP2D Honorarium Guru",               45000000.0, "2025", "SP2D",           "tersedia",  "Rak A", "Box 2025-02"),
            MasterDokumenEntity(110, "SP2D-2025-030", "SP2D Belanja Modal Jalan",           250000000.0,"2025", "SP2D",           "tersedia",  "Rak A", "Box 2025-03")
        ))
        Log.d(TAG, "Master Dokumen seeded: 10 records")
    }

    private suspend fun seedTransaksi() {
        val today = LocalDate.now()

        db.transaksiDao().insertAll(listOf(
            // SKENARIO 1: OVERDUE (4 hari) - trigger WorkManager
            TransaksiEntity(
                id = 1, instansiPeminjamId = 1,
                picNama = "Pak Joko Widodo", picNoHp = "628111222333",
                nomorSuratPengantar = "005/DINDIK/V/2026",
                fotoSuratPengantarPath = "surat_1.jpg",
                qrCodeToken = "QR-OVERDUE-001",
                tanggalPinjam = today.minusDays(10),
                tanggalKembaliRencana = today.minusDays(4),
                tanggalKembaliAktual = null,
                status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 1, approvedBy = 2
            ),
            // SKENARIO 2: PENDING - antrean Kasubag
            TransaksiEntity(
                id = 2, instansiPeminjamId = 2,
                picNama = "Dr. Andi Rahman", picNoHp = "628222333444",
                nomorSuratPengantar = "010/DINKES/V/2026",
                fotoSuratPengantarPath = "surat_2.jpg",
                qrCodeToken = null,
                tanggalPinjam = today,
                tanggalKembaliRencana = today.plusDays(7),
                tanggalKembaliAktual = null,
                status = "menunggu_persetujuan", metodePersetujuan = null,
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 1, approvedBy = null
            ),
            // SKENARIO 3: BYPASS - perlu acknowledge Kasubag
            TransaksiEntity(
                id = 3, instansiPeminjamId = 3,
                picNama = "Ibu Rina Susanti", picNoHp = "628333444555",
                nomorSuratPengantar = "015/BAPPEDA/V/2026",
                fotoSuratPengantarPath = "surat_3.jpg",
                qrCodeToken = "QR-BYPASS-003",
                tanggalPinjam = today,
                tanggalKembaliRencana = today.plusDays(5),
                tanggalKembaliAktual = null,
                status = "disetujui", metodePersetujuan = "bypass",
                buktiBypassPath = "memo_bypass.jpg",
                catatanBypass = "Kasubag ACC via telepon karena dinas luar",
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 1, approvedBy = 1
            ),
            // SKENARIO 4: NORMAL BORROWED - test pengembalian / perpanjangan
            TransaksiEntity(
                id = 4, instansiPeminjamId = 2,
                picNama = "Ibu Sari Dewi", picNoHp = "628444555666",
                nomorSuratPengantar = "020/DINKES/V/2026",
                fotoSuratPengantarPath = "surat_4.jpg",
                qrCodeToken = "QR-NORMAL-004",
                tanggalPinjam = today.minusDays(3),
                tanggalKembaliRencana = today.plusDays(4),
                tanggalKembaliAktual = null,
                status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 3, approvedBy = 2
            ),
            // SKENARIO 5: DITOLAK - dead-end
            TransaksiEntity(
                id = 5, instansiPeminjamId = 4,
                picNama = "Pak Harto", picNoHp = "628555666777",
                nomorSuratPengantar = "025/INSPEKTORAT/V/2026",
                fotoSuratPengantarPath = "surat_5.jpg",
                qrCodeToken = null,
                tanggalPinjam = today.minusDays(2),
                tanggalKembaliRencana = today.plusDays(5),
                tanggalKembaliAktual = null,
                status = "ditolak", metodePersetujuan = null,
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false,
                alasanPenolakan = "Surat pengantar tidak ditandatangani kepala dinas",
                createdBy = 1, approvedBy = 2
            ),
            // SKENARIO 6: DIKEMBALIKAN (BAIK) - riwayat sukses
            TransaksiEntity(
                id = 6, instansiPeminjamId = 5,
                picNama = "Pak Bambang", picNoHp = "628666777888",
                nomorSuratPengantar = "030/DPU/IV/2026",
                fotoSuratPengantarPath = "surat_6.jpg",
                qrCodeToken = "QR-RETURNED-006",
                tanggalPinjam = today.minusDays(15),
                tanggalKembaliRencana = today.minusDays(8),
                tanggalKembaliAktual = today.minusDays(9),
                status = "dikembalikan", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 1, approvedBy = 2
            ),
            // SKENARIO 7: DIPINJAM + Perpanjangan Pending
            TransaksiEntity(
                id = 7, instansiPeminjamId = 1,
                picNama = "Ibu Wulan", picNoHp = "628777888999",
                nomorSuratPengantar = "035/DINDIK/V/2026",
                fotoSuratPengantarPath = "surat_7.jpg",
                qrCodeToken = "QR-PERPANJANG-007",
                tanggalPinjam = today.minusDays(7),
                tanggalKembaliRencana = today.plusDays(1),
                tanggalKembaliAktual = null,
                status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = 1, approvedBy = 2
            )
        ))

        db.detailPeminjamanDao().insertAll(listOf(
            DetailPeminjamanEntity(1, 1, 101, "SP2D-2023-001", "SP2D Belanja Modal Dindik",        "2023", "Rak A - Box 2023-01", null,   null),
            DetailPeminjamanEntity(2, 4, 106, "SP2D-2025-012", "SP2D Belanja Barang Dinkes",       "2025", "Rak A - Box 2025-01", null,   null),
            DetailPeminjamanEntity(3, 6, 102, "SP2D-2023-002", "SP2D Belanja Jasa Dinkes",         "2023", "Rak A - Box 2023-02", "baik", null),
            DetailPeminjamanEntity(4, 7, 109, "SP2D-2025-025", "SP2D Honorarium Guru",             "2025", "Rak A - Box 2025-02", null,   null),
            DetailPeminjamanEntity(5, 7, 110, "SP2D-2025-030", "SP2D Belanja Modal Jalan",         "2025", "Rak A - Box 2025-03", null,   null)
        ))

        db.perpanjanganDao().insert(
            PerpanjanganEntity(
                id = 1, transaksiId = 7,
                tanggalKembaliLama = today.plusDays(1),
                tanggalKembaliBaru = today.plusDays(8),
                fotoSuratPerpanjanganPath = "surat_perpanjangan.jpg",
                alasan = "Audit BPK masih berlangsung, dokumen diperlukan",
                status = "pending", alasanPenolakan = null,
                createdBy = 1, approvedBy = null
            )
        )
        Log.d(TAG, "Transaksi seeded: 7 records, Details: 5, Perpanjangan: 1")
    }

    private suspend fun seedAuditLog() {
        val now = System.currentTimeMillis()
        db.auditLogDao().insertAll(listOf(
            AuditLogEntity(1, 1, 1, "TRANSAKSI_DIBUAT",    "Transaksi baru untuk Dindik",          null,       now - 864_000_000L),
            AuditLogEntity(2, 1, 1, "PENGAJUAN_DIKIRIM",   "Dikirim ke Kasubag",                   null,       now - 860_000_000L),
            AuditLogEntity(3, 1, 2, "DISETUJUI_ONLINE",    "Disetujui Kasubag",                    "OK",       now - 800_000_000L),
            AuditLogEntity(4, 1, 1, "DOKUMEN_DISERAHKAN",  "Diserahkan ke Pak Joko",               null,       now - 700_000_000L),
            AuditLogEntity(5, 6, 3, "TRANSAKSI_DIBUAT",    "Transaksi baru",                       null,       now - 1_300_000_000L),
            AuditLogEntity(6, 6, 2, "DISETUJUI_ONLINE",    "Disetujui",                            null,       now - 1_250_000_000L),
            AuditLogEntity(7, 6, 3, "DOKUMEN_DISERAHKAN",  "Diserahkan",                           null,       now - 1_200_000_000L),
            AuditLogEntity(8, 6, 3, "DIKEMBALIKAN_BAIK",   "Dokumen kembali baik",                 "Lengkap",  now - 800_000_000L),
            AuditLogEntity(9, 3, 1, "TRANSAKSI_DIBUAT",    "Transaksi bypass Bappeda",             null,       now - 500_000_000L),
            AuditLogEntity(10,3, 1, "DISETUJUI_BYPASS",    "Bypass via ACC telepon Kasubag",       null,       now - 490_000_000L),
            AuditLogEntity(11,7, 1, "TRANSAKSI_DIBUAT",    "Transaksi Dindik - Ibu Wulan",         null,       now - 604_800_000L),
            AuditLogEntity(12,7, 2, "DISETUJUI_ONLINE",    "Disetujui Kasubag",                    null,       now - 600_000_000L),
            AuditLogEntity(13,7, 1, "DOKUMEN_DISERAHKAN",  "Diserahkan ke Ibu Wulan",              null,       now - 590_000_000L),
            AuditLogEntity(14,7, 1, "PERPANJANGAN_DIAJUKAN","Perpanjangan diajukan. Audit BPK",    null,       now - 100_000_000L)
        ))
        Log.d(TAG, "AuditLog seeded: 14 records")
    }
}
