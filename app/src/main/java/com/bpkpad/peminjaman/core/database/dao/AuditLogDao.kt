package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT al.*, u.nama_lengkap as nama_user 
        FROM audit_log al 
        INNER JOIN users u ON al.user_id = u.id 
        WHERE al.transaksi_id = :transaksiId 
        ORDER BY al.timestamp ASC
    """)
    fun getByTransaksiId(transaksiId: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log WHERE transaksi_id = :transaksiId ORDER BY timestamp ASC")
    fun getAuditTrail(transaksiId: Int): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Query("SELECT COUNT(*) FROM audit_log WHERE transaksi_id = :transaksiId")
    suspend fun countByTransaksiId(transaksiId: Int): Int
}
