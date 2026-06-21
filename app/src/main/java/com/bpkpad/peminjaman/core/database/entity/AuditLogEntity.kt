package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*

@Entity(
    tableName = "audit_log",
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
            childColumns = ["user_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transaksi_id"]),
        Index(value = ["user_id"]),
        Index(value = ["aksi"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Int?,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "aksi") val aksi: String,
    @ColumnInfo(name = "detail") val detail: String?,
    @ColumnInfo(name = "catatan") val catatan: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
