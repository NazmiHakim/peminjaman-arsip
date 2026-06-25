package com.bpkpad.peminjaman.core.database

import android.util.Log
import com.bpkpad.peminjaman.core.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseSeeder
 * Populates the database with test master data on first install (debug only).
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
                    Log.d(TAG, "Database empty. Seeding master data...")
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
            MasterDokumenEntity(101, "SP2D-2023-001", "SP2D Belanja Modal Dindik",          15000000.0, "2023", "SP2D",           "tersedia",  "Rak A", "Box 2023-01"),
            MasterDokumenEntity(102, "SP2D-2023-002", "SP2D Belanja Jasa Dinkes",           25000000.0, "2023", "SP2D",           "tersedia",  "Rak A", "Box 2023-02"),
            MasterDokumenEntity(103, "SP-2024-055",   "Surat Perintah Tugas Bappeda",       0.0,        "2024", "Surat Perintah", "tersedia",  "Rak B", "Box 2024-01"),
            MasterDokumenEntity(104, "SPJ-2024-112",  "SPJ Perjalanan Dinas Inspektorat",   8500000.0,  "2024", "SPJ",            "tersedia",  "Rak C", "Box 2024-05"),
            MasterDokumenEntity(105, "SP2D-2024-088", "SP2D Belanja Pegawai DPU",           120000000.0,"2024", "SP2D",           "tersedia",  "Rak A", "Box 2024-08"),
            MasterDokumenEntity(106, "SP2D-2025-012", "SP2D Belanja Barang Dinkes",         7500000.0,  "2025", "SP2D",           "tersedia",  "Rak A", "Box 2025-01"),
            MasterDokumenEntity(107, "SP-2022-033",   "Surat Perintah Lama",                0.0,        "2022", "Surat Perintah", "tersedia",  "Rak D", "Box 2022-03"),
            MasterDokumenEntity(108, "SPJ-2021-007",  "SPJ Kegiatan 2021",                  3000000.0,  "2021", "SPJ",            "tersedia",  "-",     "-"),
            MasterDokumenEntity(109, "SP2D-2025-025", "SP2D Honorarium Guru",               45000000.0, "2025", "SP2D",           "tersedia",  "Rak A", "Box 2025-02"),
            MasterDokumenEntity(110, "SP2D-2025-030", "SP2D Belanja Modal Jalan",           250000000.0,"2025", "SP2D",           "tersedia",  "Rak A", "Box 2025-03")
        ))
        Log.d(TAG, "Master Dokumen seeded: 10 records")
    }
}
