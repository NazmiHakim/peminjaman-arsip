package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*

@Entity(
    tableName = "detail_peminjaman",
    foreignKeys = [
        ForeignKey(
            entity = TransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaksi_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transaksi_id"]),
        Index(value = ["dokumen_id"])
    ]
)
data class DetailPeminjamanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Int,
    @ColumnInfo(name = "dokumen_id") val dokumenId: Int,
    @ColumnInfo(name = "nomor_dokumen") val nomorDokumen: String,
    @ColumnInfo(name = "perihal_dokumen") val perihalDokumen: String?,
    @ColumnInfo(name = "tahun_dokumen") val tahunDokumen: String?,
    @ColumnInfo(name = "lokasi_rak") val lokasiRak: String?,
    @ColumnInfo(name = "kondisi_pengembalian") val kondisiPengembalian: String?, // baik|rusak|hilang
    @ColumnInfo(name = "catatan_kondisi") val catatanKondisi: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
