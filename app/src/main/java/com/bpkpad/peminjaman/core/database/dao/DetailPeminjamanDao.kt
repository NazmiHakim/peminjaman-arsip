package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.DetailPeminjamanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailPeminjamanDao {
    @Query("SELECT * FROM detail_peminjaman WHERE transaksi_id = :transaksiId ORDER BY id ASC")
    fun getByTransaksiId(transaksiId: Int): Flow<List<DetailPeminjamanEntity>>

    @Query("SELECT * FROM detail_peminjaman WHERE transaksi_id = :transaksiId ORDER BY id ASC")
    suspend fun getByTransaksiIdSync(transaksiId: Int): List<DetailPeminjamanEntity>

    @Query("SELECT * FROM detail_peminjaman WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): DetailPeminjamanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: DetailPeminjamanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<DetailPeminjamanEntity>)

    @Update
    suspend fun update(detail: DetailPeminjamanEntity)

    @Query("UPDATE detail_peminjaman SET kondisi_pengembalian = :kondisi, catatan_kondisi = :catatan WHERE id = :id")
    suspend fun updateKondisi(id: Int, kondisi: String, catatan: String?)

    @Delete
    suspend fun delete(detail: DetailPeminjamanEntity)

    @Query("DELETE FROM detail_peminjaman WHERE transaksi_id = :transaksiId")
    suspend fun deleteByTransaksiId(transaksiId: Int)
}
