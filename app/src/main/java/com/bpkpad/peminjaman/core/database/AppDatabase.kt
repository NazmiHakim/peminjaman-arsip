package com.bpkpad.peminjaman.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bpkpad.peminjaman.core.database.dao.*
import com.bpkpad.peminjaman.core.database.entity.*

@Database(
    entities = [
        UserEntity::class,
        InstansiEntity::class,
        MasterDokumenEntity::class,
        TransaksiEntity::class,
        DetailPeminjamanEntity::class,
        PerpanjanganEntity::class,
        AuditLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun instansiDao(): InstansiDao
    abstract fun masterDokumenDao(): MasterDokumenDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun detailPeminjamanDao(): DetailPeminjamanDao
    abstract fun perpanjanganDao(): PerpanjanganDao
    abstract fun auditLogDao(): AuditLogDao
}
