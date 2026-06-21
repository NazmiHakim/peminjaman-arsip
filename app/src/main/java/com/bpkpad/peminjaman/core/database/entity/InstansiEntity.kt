package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*

@Entity(
    tableName = "instansi_peminjam",
    indices = [Index(value = ["nama_instansi"]), Index(value = ["kode_instansi"], unique = true)]
)
data class InstansiEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nama_instansi") val namaInstansi: String,
    @ColumnInfo(name = "alamat") val alamat: String?,
    @ColumnInfo(name = "kode_instansi") val kodeInstansi: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_id") val remoteId: String? = null
)
