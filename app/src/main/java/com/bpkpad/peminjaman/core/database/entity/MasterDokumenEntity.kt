package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*

@Entity(
    tableName = "master_dokumen",
    indices = [
        Index(value = ["nomor_dokumen"], unique = true),
        Index(value = ["status"]),
        Index(value = ["tahun"]),
        Index(value = ["jenis_dokumen"])
    ]
)
data class MasterDokumenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nomor_dokumen") val nomorDokumen: String,
    @ColumnInfo(name = "perihal") val perihal: String,
    @ColumnInfo(name = "nominal") val nominal: Double = 0.0,
    @ColumnInfo(name = "tahun") val tahun: String,
    @ColumnInfo(name = "jenis_dokumen") val jenisDokumen: String,
    @ColumnInfo(name = "status") val status: String = "tersedia", // tersedia|dipinjam|rusak|hilang
    @ColumnInfo(name = "lokasi_rak") val lokasiRak: String?,
    @ColumnInfo(name = "lokasi_box") val lokasiBox: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_id") val remoteId: String? = null
)
