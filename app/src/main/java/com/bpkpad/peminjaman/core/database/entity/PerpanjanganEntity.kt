package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*
import java.time.LocalDate

@Entity(
    tableName = "perpanjangan",
    foreignKeys = [
        ForeignKey(
            entity = TransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaksi_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transaksi_id"]),
        Index(value = ["status"]),
        Index(value = ["created_by"])
    ]
)
data class PerpanjanganEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Int,
    @ColumnInfo(name = "tanggal_kembali_lama") val tanggalKembaliLama: LocalDate,
    @ColumnInfo(name = "tanggal_kembali_baru") val tanggalKembaliBaru: LocalDate,
    @ColumnInfo(name = "foto_surat_perpanjangan_path") val fotoSuratPerpanjanganPath: String,
    @ColumnInfo(name = "alasan") val alasan: String,
    @ColumnInfo(name = "status") val status: String = "pending", // pending|approved|rejected
    @ColumnInfo(name = "alasan_penolakan") val alasanPenolakan: String?,
    @ColumnInfo(name = "created_by") val createdBy: Int,
    @ColumnInfo(name = "approved_by") val approvedBy: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
