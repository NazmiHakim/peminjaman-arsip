package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.PerpanjanganEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerpanjanganDao {
    @Query("SELECT * FROM perpanjangan WHERE status = 'pending' ORDER BY created_at ASC")
    fun getPendingAll(): Flow<List<PerpanjanganEntity>>

    @Query("SELECT * FROM perpanjangan WHERE transaksi_id = :transaksiId ORDER BY created_at DESC")
    fun getByTransaksiId(transaksiId: Int): Flow<List<PerpanjanganEntity>>

    @Query("SELECT * FROM perpanjangan WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): PerpanjanganEntity?

    @Query("SELECT COUNT(*) FROM perpanjangan WHERE transaksi_id = :transaksiId AND status = 'pending'")
    suspend fun countPendingByTransaksiId(transaksiId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(perpanjangan: PerpanjanganEntity): Long

    @Transaction
    suspend fun insertIfNoPending(perpanjangan: PerpanjanganEntity): Long {
        if (countPendingByTransaksiId(perpanjangan.transaksiId) > 0) return -1
        return insert(perpanjangan)
    }

    @Update
    suspend fun update(perpanjangan: PerpanjanganEntity)

    @Query("UPDATE perpanjangan SET status = 'approved', approved_by = :approverId WHERE id = :id AND status = 'pending'")
    suspend fun approve(id: Int, approverId: Int): Int

    @Query("UPDATE perpanjangan SET status = 'rejected', approved_by = :approverId, alasan_penolakan = :alasan WHERE id = :id AND status = 'pending'")
    suspend fun reject(id: Int, approverId: Int, alasan: String): Int

    @Query("SELECT COUNT(*) FROM perpanjangan WHERE status = 'pending'")
    suspend fun countPending(): Int

    @Query("SELECT COUNT(*) FROM perpanjangan WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>
}
