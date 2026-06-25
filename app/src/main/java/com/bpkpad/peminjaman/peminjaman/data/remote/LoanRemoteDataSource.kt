package com.bpkpad.peminjaman.peminjaman.data.remote

import com.bpkpad.peminjaman.core.database.entity.DetailPeminjamanEntity
import com.bpkpad.peminjaman.core.database.entity.MasterDokumenEntity
import com.bpkpad.peminjaman.core.database.entity.TransaksiEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun findArchiveDocumentId(documentNumber: String): String? =
        supabase.from("archive_documents")
            .select {
                filter { eq("document_number", documentNumber) }
                limit(1)
            }
            .decodeList<RemoteIdDto>()
            .firstOrNull()
            ?.id

    suspend fun ensureTransaction(
        transaction: TransaksiEntity,
        details: List<DetailPeminjamanEntity>,
        documents: Map<Int, MasterDokumenEntity>
    ): String {
        val actorId = requireNotNull(supabase.auth.currentUserOrNull()?.id) {
            "Sesi Supabase tidak tersedia untuk sinkronisasi"
        }
        val agencyId = upsertAgency(transaction.namaInstansi, null, null)
        val remoteTransaction = findTransactionBySyncKey(transaction.syncKey)
            ?: supabase.from(TRANSACTIONS)
                .insert(
                    RemoteTransactionInsert(
                        clientReference = transaction.syncKey,
                        borrowerAgencyId = agencyId,
                        picNama = transaction.picNama,
                        picNoHp = transaction.picNoHp,
                        nomorSuratPengantar = transaction.nomorSuratPengantar,
                        fotoSuratPengantarPath = transaction.fotoSuratPengantarPath,
                        tanggalPinjam = transaction.tanggalPinjam.toString(),
                        tanggalKembaliRencana = transaction.tanggalKembaliRencana.toString(),
                        createdBy = actorId
                    )
                ) { select() }
                .decodeSingle<RemoteIdDto>()

        ensureItemsAndLocks(remoteTransaction.id, actorId, details, documents)
        syncState(remoteTransaction.id, transaction, details, documents)
        return remoteTransaction.id
    }

    suspend fun syncState(
        remoteId: String,
        transaction: TransaksiEntity,
        details: List<DetailPeminjamanEntity>,
        documents: Map<Int, MasterDokumenEntity>
    ) {
        var remoteState = getTransactionState(remoteId)
        when (transaction.status) {
            "menunggu_persetujuan" -> Unit
            "disetujui" -> {
                if (remoteState.status == "menunggu_persetujuan") {
                    syncApproval(remoteId, transaction)
                    remoteState = getTransactionState(remoteId)
                }
                syncBypassAcknowledgement(remoteId, transaction, remoteState)
            }
            "ditolak" -> if (remoteState.status == "menunggu_persetujuan") {
                updateTransaction(
                    remoteId,
                    buildJsonObject {
                        put("status", "ditolak")
                        put("alasan_penolakan", transaction.alasanPenolakan.orEmpty())
                    }
                )
            }
            "dipinjam" -> {
                if (remoteState.status == "menunggu_persetujuan") {
                    syncApproval(remoteId, transaction)
                    remoteState = getTransactionState(remoteId)
                }
                syncBypassAcknowledgement(remoteId, transaction, remoteState)
                if (remoteState.status == "disetujui") {
                    updateTransaction(remoteId, buildJsonObject { put("status", "dipinjam") })
                }
            }
            "dikembalikan" -> {
                if (remoteState.status == "menunggu_persetujuan") {
                    syncApproval(remoteId, transaction)
                    remoteState = getTransactionState(remoteId)
                }
                if (remoteState.status == "disetujui") {
                    updateTransaction(remoteId, buildJsonObject { put("status", "dipinjam") })
                    remoteState = getTransactionState(remoteId)
                }
                syncReturnConditions(remoteId, details, documents)
                if (remoteState.status == "dipinjam") {
                    updateTransaction(
                        remoteId,
                        buildJsonObject {
                            put("status", "dikembalikan")
                            put("tanggal_kembali_aktual", transaction.tanggalKembaliAktual.toString())
                        }
                    )
                }
            }
            "dibatalkan" -> if (remoteState.status in setOf("menunggu_persetujuan", "disetujui")) {
                updateTransaction(remoteId, buildJsonObject { put("status", "dibatalkan") })
            }
        }
    }

    private suspend fun syncApproval(remoteId: String, transaction: TransaksiEntity) {
        val method = transaction.metodePersetujuan ?: "online"
        updateTransaction(
            remoteId,
            buildJsonObject {
                put("status", "disetujui")
                put("metode_persetujuan", method)
                transaction.qrCodeToken?.let { put("qr_code_token", it) }
                if (method == "bypass") {
                    put("bukti_bypass_path", transaction.buktiBypassPath.orEmpty())
                    put("catatan_bypass", transaction.catatanBypass.orEmpty())
                }
            }
        )
    }

    private suspend fun syncBypassAcknowledgement(
        remoteId: String,
        transaction: TransaksiEntity,
        remoteState: RemoteTransactionState
    ) {
        if (
            transaction.metodePersetujuan == "bypass" &&
            transaction.isBypassAcknowledged &&
            !remoteState.isBypassAcknowledged
        ) {
            updateTransaction(
                remoteId,
                buildJsonObject { put("is_bypass_acknowledged", true) }
            )
        }
    }

    private suspend fun updateTransaction(
        remoteId: String,
        values: kotlinx.serialization.json.JsonObject
    ) {
        supabase.from(TRANSACTIONS).update(values) {
            filter { eq("id", remoteId) }
        }
    }

    suspend fun upsertAgency(name: String, address: String?, code: String?): String {
        val actorId = requireNotNull(supabase.auth.currentUserOrNull()?.id) {
            "Sesi Supabase tidak tersedia untuk sinkronisasi"
        }
        val existing = supabase.from(AGENCIES)
            .select {
                filter { eq("nama_instansi", name) }
                limit(1)
            }
            .decodeList<RemoteIdDto>()
            .firstOrNull()
        if (existing != null) {
            if (address != null || code != null) {
                supabase.from(AGENCIES).update(
                    buildJsonObject {
                        address?.let { put("alamat", it) }
                        code?.let { put("kode_instansi", it) }
                        put("updated_by", actorId)
                    }
                ) {
                    filter { eq("id", existing.id) }
                }
            }
            return existing.id
        }

        return supabase.from(AGENCIES)
            .insert(
                RemoteAgencyInsert(
                    namaInstansi = name,
                    alamat = address,
                    kodeInstansi = code,
                    createdBy = actorId
                )
            ) { select() }
            .decodeSingle<RemoteIdDto>()
            .id
    }

    private suspend fun findTransactionBySyncKey(syncKey: String): RemoteIdDto? =
        supabase.from(TRANSACTIONS)
            .select {
                filter { eq("client_reference", syncKey) }
                limit(1)
            }
            .decodeList<RemoteIdDto>()
            .firstOrNull()

    private suspend fun getTransactionState(remoteId: String): RemoteTransactionState =
        supabase.from(TRANSACTIONS)
            .select {
                filter { eq("id", remoteId) }
                limit(1)
            }
            .decodeSingle<RemoteTransactionState>()

    private suspend fun ensureItemsAndLocks(
        remoteTransactionId: String,
        actorId: String,
        details: List<DetailPeminjamanEntity>,
        documents: Map<Int, MasterDokumenEntity>
    ) {
        val existingDocumentIds = supabase.from(ITEMS)
            .select {
                filter { eq("loan_transaction_id", remoteTransactionId) }
            }
            .decodeList<RemoteLoanItemReference>()
            .map(RemoteLoanItemReference::archiveDocumentId)
            .toSet()

        val missingItems = details.mapNotNull { detail ->
            val document = documents[detail.dokumenId] ?: return@mapNotNull null
            val archiveId = document.remoteId
                ?: error("Dokumen ${document.nomorDokumen} belum memiliki UUID Supabase")
            if (archiveId in existingDocumentIds) return@mapNotNull null
            RemoteLoanItemInsert(
                loanTransactionId = remoteTransactionId,
                archiveDocumentId = archiveId,
                documentNumberSnapshot = detail.nomorDokumen,
                titleSnapshot = detail.perihalDokumen.orEmpty(),
                yearSnapshot = detail.tahunDokumen?.toIntOrNull(),
                locationSnapshot = detail.lokasiRak
            )
        }
        if (missingItems.isNotEmpty()) supabase.from(ITEMS).insert(missingItems)

        val activeLockIds = supabase.from(LOCKS)
            .select {
                filter { eq("loan_transaction_id", remoteTransactionId) }
            }
            .decodeList<RemoteLockReference>()
            .map(RemoteLockReference::archiveDocumentId)
            .toSet()
        val missingLocks = details.mapNotNull { detail ->
            val archiveId = documents[detail.dokumenId]?.remoteId ?: return@mapNotNull null
            if (archiveId in activeLockIds) return@mapNotNull null
            RemoteLockInsert(remoteTransactionId, archiveId, actorId)
        }
        if (missingLocks.isNotEmpty()) supabase.from(LOCKS).insert(missingLocks)
    }

    private suspend fun syncReturnConditions(
        remoteTransactionId: String,
        details: List<DetailPeminjamanEntity>,
        documents: Map<Int, MasterDokumenEntity>
    ) {
        details.forEach { detail ->
            val archiveId = documents[detail.dokumenId]?.remoteId ?: return@forEach
            supabase.from(ITEMS).update(
                buildJsonObject {
                    detail.kondisiPengembalian?.let { put("return_condition", it) }
                    detail.catatanKondisi?.let { put("condition_note", it) }
                }
            ) {
                filter {
                    eq("loan_transaction_id", remoteTransactionId)
                    eq("archive_document_id", archiveId)
                }
            }
        }
    }

    @Serializable
    private data class RemoteIdDto(val id: String)

    @Serializable
    private data class RemoteTransactionState(
        val status: String,
        @SerialName("is_bypass_acknowledged") val isBypassAcknowledged: Boolean
    )

    @Serializable
    private data class RemoteAgencyInsert(
        @SerialName("nama_instansi") val namaInstansi: String,
        val alamat: String?,
        @SerialName("kode_instansi") val kodeInstansi: String?,
        @SerialName("created_by") val createdBy: String
    )

    @Serializable
    private data class RemoteTransactionInsert(
        @SerialName("client_reference") val clientReference: String,
        @SerialName("borrower_agency_id") val borrowerAgencyId: String,
        @SerialName("pic_nama") val picNama: String,
        @SerialName("pic_no_hp") val picNoHp: String,
        @SerialName("nomor_surat_pengantar") val nomorSuratPengantar: String,
        @SerialName("foto_surat_pengantar_path") val fotoSuratPengantarPath: String,
        @SerialName("tanggal_pinjam") val tanggalPinjam: String,
        @SerialName("tanggal_kembali_rencana") val tanggalKembaliRencana: String,
        @SerialName("created_by") val createdBy: String
    )

    @Serializable
    private data class RemoteLoanItemInsert(
        @SerialName("loan_transaction_id") val loanTransactionId: String,
        @SerialName("archive_document_id") val archiveDocumentId: String,
        @SerialName("document_number_snapshot") val documentNumberSnapshot: String,
        @SerialName("title_snapshot") val titleSnapshot: String,
        @SerialName("year_snapshot") val yearSnapshot: Int?,
        @SerialName("location_snapshot") val locationSnapshot: String?
    )

    @Serializable
    private data class RemoteLoanItemReference(
        @SerialName("archive_document_id") val archiveDocumentId: String
    )

    @Serializable
    private data class RemoteLockInsert(
        @SerialName("loan_transaction_id") val loanTransactionId: String,
        @SerialName("archive_document_id") val archiveDocumentId: String,
        @SerialName("locked_by") val lockedBy: String
    )

    @Serializable
    data class RemoteProfileDto(
        val id: String,
        @SerialName("legacy_id") val legacyId: Long,
        val username: String,
        @SerialName("nama_lengkap") val namaLengkap: String,
        val nip: String? = null,
        val role: String,
        @SerialName("no_hp") val noHp: String? = null,
        @SerialName("is_active") val isActive: Boolean
    )

    @Serializable
    data class RemoteLoanItem(
        @SerialName("loan_transaction_id") val loanTransactionId: String,
        @SerialName("archive_document_id") val archiveDocumentId: String,
        @SerialName("document_number_snapshot") val documentNumberSnapshot: String,
        @SerialName("title_snapshot") val titleSnapshot: String,
        @SerialName("year_snapshot") val yearSnapshot: Int?,
        @SerialName("location_snapshot") val locationSnapshot: String?,
        @SerialName("return_condition") val returnCondition: String? = null,
        @SerialName("condition_note") val conditionNote: String? = null
    )

    @Serializable
    data class RemoteAgencyDto(
        @SerialName("nama_instansi") val namaInstansi: String
    )

    @Serializable
    data class RemoteTransactionResponse(
        val id: String,
        @SerialName("client_reference") val clientReference: String?,
        @SerialName("borrower_agency_id") val borrowerAgencyId: String,
        @SerialName("pic_nama") val picNama: String,
        @SerialName("pic_no_hp") val picNoHp: String,
        @SerialName("nomor_surat_pengantar") val nomorSuratPengantar: String,
        @SerialName("foto_surat_pengantar_path") val fotoSuratPengantarPath: String,
        @SerialName("qr_code_token") val qrCodeToken: String?,
        @SerialName("tanggal_pinjam") val tanggalPinjam: String,
        @SerialName("tanggal_kembali_rencana") val tanggalKembaliRencana: String,
        @SerialName("tanggal_kembali_aktual") val tanggalKembaliAktual: String?,
        val status: String,
        @SerialName("metode_persetujuan") val metodePersetujuan: String?,
        @SerialName("bukti_bypass_path") val buktiBypassPath: String?,
        @SerialName("catatan_bypass") val catatanBypass: String?,
        @SerialName("is_bypass_acknowledged") val isBypassAcknowledged: Boolean,
        @SerialName("alasan_penolakan") val alasanPenolakan: String?,
        @SerialName("created_by") val createdBy: String,
        @SerialName("approved_by") val approvedBy: String?,
        @SerialName("loan_borrower_agencies") val agency: RemoteAgencyDto? = null,
        @SerialName("loan_items") val items: List<RemoteLoanItem> = emptyList()
    )

    suspend fun getAllProfiles(): List<RemoteProfileDto> =
        supabase.from("loan_profiles")
            .select()
            .decodeList<RemoteProfileDto>()

    suspend fun getAllTransactions(): List<RemoteTransactionResponse> =
        supabase.from(TRANSACTIONS)
            .select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                    """
                    id,
                    client_reference,
                    borrower_agency_id,
                    pic_nama,
                    pic_no_hp,
                    nomor_surat_pengantar,
                    foto_surat_pengantar_path,
                    qr_code_token,
                    tanggal_pinjam,
                    tanggal_kembali_rencana,
                    tanggal_kembali_aktual,
                    status,
                    metode_persetujuan,
                    bukti_bypass_path,
                    catatan_bypass,
                    is_bypass_acknowledged,
                    alasan_penolakan,
                    created_by,
                    approved_by,
                    loan_borrower_agencies(nama_instansi),
                    loan_items(*)
                    """.trimIndent()
                )
            )
            .decodeList<RemoteTransactionResponse>()

    @Serializable
    private data class RemoteLockReference(
        @SerialName("archive_document_id") val archiveDocumentId: String
    )

    private companion object {
        const val AGENCIES = "loan_borrower_agencies"
        const val TRANSACTIONS = "loan_transactions"
        const val ITEMS = "loan_items"
        const val LOCKS = "loan_document_locks"
    }
}
