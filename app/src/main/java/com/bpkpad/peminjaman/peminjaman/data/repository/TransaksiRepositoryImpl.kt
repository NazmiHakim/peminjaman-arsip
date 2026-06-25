package com.bpkpad.peminjaman.peminjaman.data.repository

import android.util.Log
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.*
import com.bpkpad.peminjaman.core.database.entity.*
import com.bpkpad.peminjaman.core.database.dao.*
import com.bpkpad.peminjaman.core.database.entity.*
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.*
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import com.bpkpad.peminjaman.peminjaman.data.remote.LoanRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransaksiRepositoryImpl @Inject constructor(
    private val transaksiDao: TransaksiDao,
    private val detailDao: DetailPeminjamanDao,
    private val instansiDao: InstansiDao,
    private val userDao: UserDao,
    private val masterDokumenDao: MasterDokumenDao,
    private val loanRemoteDataSource: LoanRemoteDataSource
) : TransaksiRepository {

    override fun getAll(): Flow<List<Transaksi>> = flow {
        refreshFromRemote()
        emitAll(transaksiDao.getAll().map { list -> list.map { buildDomainWithDetails(it) } })
    }

    override fun getByStatus(status: TransaksiStatus): Flow<List<Transaksi>> = flow {
        refreshFromRemote()
        emitAll(transaksiDao.getByStatus(status.name.lowercase()).map { list -> list.map { buildDomainWithDetails(it) } })
    }

    override fun getOverdue(): Flow<List<Transaksi>> = flow {
        refreshFromRemote()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        emitAll(transaksiDao.getOverdue(today).map { list -> list.map { buildDomainWithDetails(it) } })
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
            val entity = transaksi.toEntity().copy(syncKey = UUID.randomUUID().toString())
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
            syncTransaction(transaksiId)

            val created = transaksiDao.getById(transaksiId) ?: return ResultState.Error("Gagal membaca data yang baru dibuat")
            ResultState.Success(buildDomainWithDetails(created))
        } catch (e: Exception) {
            ResultState.Error("Gagal membuat transaksi: ${e.message}", e)
        }
    }

    override suspend fun update(transaksi: Transaksi): ResultState<Transaksi> {
        return try {
            val existing = transaksiDao.getById(transaksi.id)
                ?: return ResultState.Error("Transaksi tidak ditemukan")
            transaksiDao.update(
                transaksi.toEntity().copy(
                    remoteId = existing.remoteId,
                    syncKey = existing.syncKey,
                    syncState = "pending",
                    lastSyncError = null,
                    createdAt = existing.createdAt
                )
            )
            syncTransaction(transaksi.id)
            ResultState.Success(transaksi)
        } catch (e: Exception) {
            ResultState.Error("Gagal mengupdate transaksi: ${e.message}", e)
        }
    }

    override suspend fun approve(transaksiId: Int, approverId: Int, qrToken: String): ResultState<Unit> {
        return try {
            transaksiDao.approve(transaksiId, approverId, qrToken)
            syncTransaction(transaksiId)
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
            syncTransaction(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menolak transaksi: ${e.message}", e)
        }
    }

    override suspend fun bypass(
        transaksiId: Int,
        arsiparisId: Int,
        buktiPath: String,
        catatan: String,
        qrToken: String
    ): ResultState<Unit> {
        return try {
            val updatedRows = transaksiDao.bypass(
                transaksiId,
                arsiparisId,
                buktiPath,
                catatan,
                qrToken
            )
            if (updatedRows == 1) {
                syncTransaction(transaksiId)
                ResultState.Success(Unit)
            } else {
                ResultState.Error(
                    "Bypass gagal karena transaksi sudah berubah atau tidak lagi menunggu persetujuan"
                )
            }
        } catch (e: Exception) {
            ResultState.Error("Gagal bypass persetujuan: ${e.message}", e)
        }
    }

    override suspend fun acknowledgeBypass(transaksiId: Int, kasubagId: Int): ResultState<Unit> {
        return try {
            transaksiDao.acknowledgeBypass(transaksiId)
            syncTransaction(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal acknowledge bypass: ${e.message}", e)
        }
    }

    override suspend fun confirmHandover(transaksiId: Int, arsiparisId: Int): ResultState<Unit> {
        return try {
            transaksiDao.confirmHandover(transaksiId)
            syncTransaction(transaksiId)
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
            syncTransaction(transaksiId)
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
            syncTransaction(transaksiId)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal membatalkan transaksi: ${e.message}", e)
        }
    }

    override suspend fun syncPending() {
        transaksiDao.getPendingSync().forEach { transaction ->
            syncTransaction(transaction.id)
        }
    }

    override fun getDashboardStats(): Flow<Map<String, Int>> = flow {
        refreshFromRemote()
        emitAll(transaksiDao.getAll().map { list ->
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
        })
    }

    private suspend fun buildDomainWithDetails(entity: TransaksiEntity): Transaksi {
        val details = detailDao.getByTransaksiIdSync(entity.id)
        return buildDomain(entity, details.map { it.toDomain() })
    }

    private suspend fun syncTransaction(transaksiId: Int) {
        val transaction = transaksiDao.getById(transaksiId) ?: return
        runCatching {
            withTimeout(SYNC_TIMEOUT_MILLIS) {
                val details = detailDao.getByTransaksiIdSync(transaksiId)
                val documents = details.mapNotNull { detail ->
                    val localDocument = masterDokumenDao.getById(detail.dokumenId)
                        ?: return@mapNotNull null
                    val resolvedDocument = if (localDocument.remoteId == null) {
                        loanRemoteDataSource.findArchiveDocumentId(localDocument.nomorDokumen)
                            ?.also { remoteId ->
                                masterDokumenDao.updateRemoteId(localDocument.id, remoteId)
                            }
                            ?.let { remoteId -> localDocument.copy(remoteId = remoteId) }
                            ?: localDocument
                    } else {
                        localDocument
                    }
                    detail.dokumenId to resolvedDocument
                }.toMap()
                val remoteId = loanRemoteDataSource.ensureTransaction(
                    transaction = transaction,
                    details = details,
                    documents = documents
                )
                transaksiDao.markSynced(transaksiId, remoteId)
            }
        }.onFailure { error ->
            transaksiDao.markSyncPending(
                transaksiId,
                error.message?.take(MAX_SYNC_ERROR_LENGTH)
            )
            Log.w(TAG, "Transaksi #$transaksiId tersimpan lokal dan menunggu sinkronisasi", error)
        }
    }

    private suspend fun refreshFromRemote() {
        runCatching {
            // 1. Sync remote user profiles to local SQLite DB
            val remoteProfiles = try {
                loanRemoteDataSource.getAllProfiles()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat profil pengguna dari remote: ${e.message}", e)
                emptyList()
            }
            remoteProfiles.forEach { profile ->
                userDao.upsert(
                    UserEntity(
                        id = profile.legacyId.toInt(),
                        username = profile.username,
                        passwordHash = "",
                        namaLengkap = profile.namaLengkap,
                        nip = profile.nip,
                        role = profile.role,
                        noHp = profile.noHp,
                        isActive = profile.isActive
                    )
                )
            }
            val uuidToLegacyIdMap = remoteProfiles.associate { it.id to it.legacyId.toInt() }

            // 2. Fetch all transactions (with agency name and items embedded)
            val remoteTransactions = loanRemoteDataSource.getAllTransactions()

            // 3. Process each transaction
            remoteTransactions.forEach { remoteTx ->
                val localTx = remoteTx.id.let { transaksiDao.getByRemoteId(it) }
                    ?: remoteTx.clientReference?.let { transaksiDao.getBySyncKey(it) }

                val createdByLegacyId = uuidToLegacyIdMap[remoteTx.createdBy] ?: 1
                val approvedByLegacyId = remoteTx.approvedBy?.let { uuidToLegacyIdMap[it] }

                val entity = TransaksiEntity(
                    id = localTx?.id ?: 0,
                    namaInstansi = remoteTx.agency?.namaInstansi ?: remoteTx.picNama,
                    picNama = remoteTx.picNama,
                    picNoHp = remoteTx.picNoHp,
                    nomorSuratPengantar = remoteTx.nomorSuratPengantar,
                    fotoSuratPengantarPath = remoteTx.fotoSuratPengantarPath,
                    qrCodeToken = remoteTx.qrCodeToken,
                    tanggalPinjam = LocalDate.parse(remoteTx.tanggalPinjam),
                    tanggalKembaliRencana = LocalDate.parse(remoteTx.tanggalKembaliRencana),
                    tanggalKembaliAktual = remoteTx.tanggalKembaliAktual?.let { LocalDate.parse(it) },
                    status = remoteTx.status,
                    metodePersetujuan = remoteTx.metodePersetujuan,
                    buktiBypassPath = remoteTx.buktiBypassPath,
                    catatanBypass = remoteTx.catatanBypass,
                    isBypassAcknowledged = remoteTx.isBypassAcknowledged,
                    alasanPenolakan = remoteTx.alasanPenolakan,
                    createdBy = createdByLegacyId,
                    approvedBy = approvedByLegacyId,
                    remoteId = remoteTx.id,
                    syncKey = remoteTx.clientReference ?: UUID.randomUUID().toString(),
                    syncState = "synced"
                )
                val txId = transaksiDao.insert(entity).toInt()

                // Sync details (items)
                detailDao.deleteByTransaksiId(txId)
                val detailEntities = remoteTx.items.map { item ->
                    val localDoc = masterDokumenDao.getByRemoteId(item.archiveDocumentId)
                    val docId = localDoc?.id ?: 0
                    DetailPeminjamanEntity(
                        transaksiId = txId,
                        dokumenId = docId,
                        nomorDokumen = item.documentNumberSnapshot,
                        perihalDokumen = item.titleSnapshot,
                        tahunDokumen = item.yearSnapshot?.toString(),
                        lokasiRak = item.locationSnapshot,
                        kondisiPengembalian = item.returnCondition,
                        catatanKondisi = item.conditionNote
                    )
                }
                detailDao.insertAll(detailEntities)
            }
        }.onFailure { error ->
            Log.e(TAG, "Gagal sinkronisasi transaksi dari remote", error)
        }
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

    private companion object {
        const val TAG = "TransaksiSync"
        const val SYNC_TIMEOUT_MILLIS = 8_000L
        const val MAX_SYNC_ERROR_LENGTH = 500
    }
}
