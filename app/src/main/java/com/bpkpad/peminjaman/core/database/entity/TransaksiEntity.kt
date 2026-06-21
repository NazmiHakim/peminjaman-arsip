package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "transaksi_peminjaman",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["tanggal_kembali_rencana"]),
        Index(value = ["created_by"]),
        Index(value = ["qr_code_token"], unique = true),
        Index(value = ["tanggal_pinjam"]),
        Index(value = ["sync_key"], unique = true)
    ]
)
data class TransaksiEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nama_instansi") val namaInstansi: String,
    @ColumnInfo(name = "pic_nama") val picNama: String,
    @ColumnInfo(name = "pic_no_hp") val picNoHp: String,
    @ColumnInfo(name = "nomor_surat_pengantar") val nomorSuratPengantar: String,
    @ColumnInfo(name = "foto_surat_pengantar_path") val fotoSuratPengantarPath: String,
    @ColumnInfo(name = "qr_code_token") val qrCodeToken: String?,
    @ColumnInfo(name = "tanggal_pinjam") val tanggalPinjam: LocalDate,
    @ColumnInfo(name = "tanggal_kembali_rencana") val tanggalKembaliRencana: LocalDate,
    @ColumnInfo(name = "tanggal_kembali_aktual") val tanggalKembaliAktual: LocalDate?,
    @ColumnInfo(name = "status") val status: String = "menunggu_persetujuan",
    @ColumnInfo(name = "metode_persetujuan") val metodePersetujuan: String?,
    @ColumnInfo(name = "bukti_bypass_path") val buktiBypassPath: String?,
    @ColumnInfo(name = "catatan_bypass") val catatanBypass: String?,
    @ColumnInfo(name = "is_bypass_acknowledged") val isBypassAcknowledged: Boolean = false,
    @ColumnInfo(name = "alasan_penolakan") val alasanPenolakan: String?,
    @ColumnInfo(name = "created_by") val createdBy: Int,
    @ColumnInfo(name = "approved_by") val approvedBy: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "sync_key", defaultValue = "''")
    val syncKey: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "sync_state", defaultValue = "'pending'") val syncState: String = "pending",
    @ColumnInfo(name = "last_sync_error") val lastSyncError: String? = null
)
