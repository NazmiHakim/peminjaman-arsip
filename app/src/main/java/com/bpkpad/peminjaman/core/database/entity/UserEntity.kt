package com.bpkpad.peminjaman.core.database.entity

import androidx.room.*
import java.time.LocalDate

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "nama_lengkap") val namaLengkap: String,
    @ColumnInfo(name = "nip") val nip: String?,
    @ColumnInfo(name = "role") val role: String, // "arsiparis" | "kasubag"
    @ColumnInfo(name = "no_hp") val noHp: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
