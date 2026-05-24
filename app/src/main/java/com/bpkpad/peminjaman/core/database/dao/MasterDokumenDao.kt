package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.MasterDokumenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDokumenDao {
    @Query("SELECT * FROM master_dokumen ORDER BY nomor_dokumen ASC")
    fun getAll(): Flow<List<MasterDokumenEntity>>

    @Query("SELECT * FROM master_dokumen WHERE status = 'tersedia' ORDER BY nomor_dokumen ASC")
    fun getAvailable(): Flow<List<MasterDokumenEntity>>

    @Query("SELECT * FROM master_dokumen WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): MasterDokumenEntity?

    @Query("""
        SELECT * FROM master_dokumen 
        WHERE nomor_dokumen LIKE '%' || :query || '%' 
           OR perihal LIKE '%' || :query || '%'
           OR tahun LIKE '%' || :query || '%'
           OR jenis_dokumen LIKE '%' || :query || '%'
        ORDER BY nomor_dokumen ASC
    """)
    fun search(query: String): Flow<List<MasterDokumenEntity>>

    @Query("SELECT * FROM master_dokumen WHERE status = :status ORDER BY nomor_dokumen ASC")
    fun getByStatus(status: String): Flow<List<MasterDokumenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dokumen: MasterDokumenEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dokumenList: List<MasterDokumenEntity>)

    @Update
    suspend fun update(dokumen: MasterDokumenEntity)

    @Query("UPDATE master_dokumen SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(dokumen: MasterDokumenEntity)

    @Query("SELECT COUNT(*) FROM master_dokumen")
    suspend fun count(): Int
}
