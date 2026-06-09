package com.bpkpad.peminjaman.peminjaman.data.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.*
import com.bpkpad.peminjaman.core.database.entity.*
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.*
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransaksiRepositoryImpl @Inject constructor(
    private val transaksiDao: TransaksiDao,
    private val detailDao: DetailPeminjamanDao,
    private val instansiDao: InstansiDao,
    private val userDao: UserDao,
    private val masterDokumenDao: MasterDokumenDao
) : TransaksiRepository {

    override fun getAll(): Flow<List<Transaksi>> =
        transaksiDao.getAll().map { list -> list.map { buildDomainWithDetails(it) } }

    override fun getByStatus(status: TransaksiStatus): Flow<List<Transaksi>> =
        transaksiDao.getByStatus(status.name.lowercase()).map { list -> list.map { buildDomainWithDetails(it) } }

    override fun getOverdue(): Flow<List<Transaksi>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return transaksiDao.getOverdue(today).map { list -> list.map { buildDomainWithDetails(it) } }
    }

    override suspend fun getById(id: Int): Transaksi? {
        val entity = transaksiDao.getById(id) ?: return null
        return buildDomainWithDetails(entity)
    }

    override suspend fun findByQrToken(token: String): Transaksi? {
        val entity = transaksiDao.findByQrToken(token) ?: return null
        return buildDomainWithDetails(entity)
    }

    override suspend fun create(transaksi: Transaksi, dokumenIds: List<Int>): ResultState<Transaksi> {
        return try {
            for (dokumenId in dokumenIds) {
                val dok = masterDokumenDao.getById(dokumenId)
                if (dok == null) return ResultState.Error("Dokumen tidak ditemukan di database.")

                // Pastikan dokumen statusnya 'tersedia'
                if (dok.status.lowercase() != "tersedia") {
                    return ResultState.Error("PENGAJUAN DITOLAK: Dokumen ${dok.nomorDokumen} saat ini sedang dipinjam atau diproses oleh instansi lain. Silakan ajukan kembali setelah dokumen dikembalikan.")
                }
            }
            // Insert transaksi
            val entity = transaksi.toEntity()
            val transaksiId = transaksiDao.insert(entity).toInt()

            // Insert detail peminjaman for each dokumen
            val details = dokumenIds.mapNotNull { dokumenId ->
                val dok = masterDokumenDao.getById(dokumenId) ?: return@mapNotNull null
                masterDokumenDao.updateStatus(dokumenId, "dipinjam")
                DetailPeminjamanEntity(
                    transaksiId = transaksiId,
                    dokumenId = dokumenId,
                    nomorDokumen = dok.nomorDokumen,
                    perihalDokumen = dok.perihal,
                    tahunDokumen = dok.tahun,
                    lokasiRak = dok.lokasiRak?.let { "${it} - ${dok.lokasiBox ?: ""}" },
                    kondisiPengembalian = null,
                    catatanKondisi = null
                )
            }
            detailDao.insertAll(details)

            val created = transaksiDao.getById(transaksiId) ?: return ResultState.Error("Gagal membaca data yang baru dibuat")
            ResultState.Success(buildDomainWithDetails(created))
        } catch (e: Exception) {
            ResultState.Error("Gagal membuat transaksi: ${e.message}", e)
        }
    }

    override suspend fun update(transaksi: Transaksi): ResultState<Transaksi> {
        return try {
            transaksiDao.update(transaksi.toEntity())
            ResultState.Success(transaksi)
        } catch (e: Exception) {
            ResultState.Error("Gagal mengupdate transaksi: ${e.message}", e)
        }
    }

    override suspend fun approve(transaksiId: Int, approverId: Int, qrToken: String): ResultState<Unit> {
        return try {
            transaksiDao.approve(transaksiId, approverId, qrToken)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menyetujui transaksi: ${e.message}", e)
        }
    }

    override suspend fun reject(transaksiId: Int, approverId: Int, alasan: String): ResultState<Unit> {
        return try {
            // --- TAMBAHAN TASK 10: Membuka kembali gembok dokumen jika ditolak Kasubag ---
            val details = detailDao.getByTransaksiIdSync(transaksiId)
            details.forEach { detail ->
                masterDokumenDao.updateStatus(detail.dokumenId, "tersedia")
            }
            // -----------------------------------------------------------------------------

            transaksiDao.reject(transaksiId, approverId, alasan)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menolak transaksi: ${e.message}", e)
        }
    }

    override suspend fun bypass(transaksiId: Int, arsiparisId: Int, buktiPath: String, catatan: String): ResultState<Unit> {
        return try {
            transaksiDao.bypass(transaksiId, arsiparisId, buktiPath, catatan)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal bypass persetujuan: ${e.message}", e)
        }
    }

    override suspend fun acknowledgeBypass(transaksiId: Int, kasubagId: Int): ResultState<Unit> {
        return try {
            transaksiDao.acknowledgeBypass(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal acknowledge bypass: ${e.message}", e)
        }
    }

    override suspend fun confirmHandover(transaksiId: Int, arsiparisId: Int): ResultState<Unit> {
        return try {
            transaksiDao.confirmHandover(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal konfirmasi serah: ${e.message}", e)
        }
    }

    override suspend fun returnTransaksi(
        transaksiId: Int,
        arsiparisId: Int,
        kondisiMap: Map<Int, Pair<String, String?>>
    ): ResultState<Unit> {
        return try {
            // Update each detail's kondisi
            for ((detailId, pair) in kondisiMap) {
                val (kondisi, catatan) = pair
                detailDao.updateKondisi(detailId, kondisi, catatan)

                // Update master dokumen status
                val detail = detailDao.getById(detailId)
                if (detail != null) {
                    val masterStatus = when (kondisi.lowercase()) {
                        "rusak" -> "rusak"
                        "hilang" -> "hilang"
                        else -> "tersedia"
                    }
                    masterDokumenDao.updateStatus(detail.dokumenId, masterStatus)
                }
            }

            // Update transaksi status
            transaksiDao.returnTransaksi(transaksiId, LocalDate.now())
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menyelesaikan pengembalian: ${e.message}", e)
        }
    }

    override suspend fun cancel(transaksiId: Int, userId: Int): ResultState<Unit> {
        return try {
            // Unlock master dokumen if already dipinjam
            val details = detailDao.getByTransaksiIdSync(transaksiId)
            details.forEach { detail ->
                masterDokumenDao.updateStatus(detail.dokumenId, "tersedia")
            }
            transaksiDao.cancel(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal membatalkan transaksi: ${e.message}", e)
        }
    }

    override fun getDashboardStats(): Flow<Map<String, Int>> {
        return transaksiDao.getAll().map { list ->
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            mapOf(
                "total" to list.size,
                "menunggu" to list.count { it.status == "menunggu_persetujuan" },
                "dipinjam" to list.count { it.status == "dipinjam" },
                "overdue" to list.count { it.status == "dipinjam" && it.tanggalKembaliRencana.isBefore(LocalDate.now()) },
                "dikembalikan" to list.count { it.status == "dikembalikan" },
                "ditolak" to list.count { it.status == "ditolak" },
                "dibatalkan" to list.count { it.status == "dibatalkan" },
                "bypass_pending_acknowledge" to list.count { it.metodePersetujuan == "bypass" && !it.isBypassAcknowledged },
                "disetujui" to list.count { it.status == "disetujui" }
            )
        }
    }

    private suspend fun buildDomainWithDetails(entity: TransaksiEntity): Transaksi {
        val details = detailDao.getByTransaksiIdSync(entity.id)
        return buildDomain(entity, details.map { it.toDomain() })
    }

    private suspend fun buildDomain(entity: TransaksiEntity, details: List<DetailPeminjaman> = emptyList()): Transaksi {
        val createdBy = userDao.getById(entity.createdBy)
        val approvedBy = entity.approvedBy?.let { userDao.getById(it) }
        return Transaksi(
            id = entity.id,
            namaInstansi = entity.namaInstansi,
            picNama = entity.picNama,
            picNoHp = entity.picNoHp,
            nomorSuratPengantar = entity.nomorSuratPengantar,
            fotoSuratPengantarPath = entity.fotoSuratPengantarPath,
            qrCodeToken = entity.qrCodeToken,
            tanggalPinjam = entity.tanggalPinjam,
            tanggalKembaliRencana = entity.tanggalKembaliRencana,
            tanggalKembaliAktual = entity.tanggalKembaliAktual,
            status = TransaksiStatus.fromString(entity.status),
            metodePersetujuan = MetodePersetujuan.fromString(entity.metodePersetujuan),
            buktiBypassPath = entity.buktiBypassPath,
            catatanBypass = entity.catatanBypass,
            isBypassAcknowledged = entity.isBypassAcknowledged,
            alasanPenolakan = entity.alasanPenolakan,
            createdBy = entity.createdBy,
            namaCreatedBy = createdBy?.namaLengkap.orEmpty(),
            approvedBy = entity.approvedBy,
            namaApprovedBy = approvedBy?.namaLengkap,
            createdAt = entity.createdAt,
            details = details
        )
    }

    private fun Transaksi.toEntity() = TransaksiEntity(
        id = id,
        namaInstansi = namaInstansi,
        picNama = picNama,
        picNoHp = picNoHp,
        nomorSuratPengantar = nomorSuratPengantar,
        fotoSuratPengantarPath = fotoSuratPengantarPath,
        qrCodeToken = qrCodeToken,
        tanggalPinjam = tanggalPinjam,
        tanggalKembaliRencana = tanggalKembaliRencana,
        tanggalKembaliAktual = tanggalKembaliAktual,
        status = status.name.lowercase(),
        metodePersetujuan = metodePersetujuan?.name?.lowercase(),
        buktiBypassPath = buktiBypassPath,
        catatanBypass = catatanBypass,
        isBypassAcknowledged = isBypassAcknowledged,
        alasanPenolakan = alasanPenolakan,
        createdBy = createdBy,
        approvedBy = approvedBy
    )

    private fun DetailPeminjamanEntity.toDomain() = DetailPeminjaman(
        id = id,
        transaksiId = transaksiId,
        dokumenId = dokumenId,
        nomorDokumen = nomorDokumen,
        perihalDokumen = perihalDokumen,
        tahunDokumen = tahunDokumen,
        lokasiRak = lokasiRak,
        kondisiPengembalian = KondisiPengembalian.fromString(kondisiPengembalian),
        catatanKondisi = catatanKondisi
    )
}
