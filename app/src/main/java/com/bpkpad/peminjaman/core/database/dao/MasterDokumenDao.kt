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

    @Query("SELECT * FROM master_dokumen WHERE nomor_dokumen = :nomorDokumen LIMIT 1")
    suspend fun getByNomorDokumen(nomorDokumen: String): MasterDokumenEntity?

    @Query("UPDATE master_dokumen SET remote_id = :remoteId, updated_at = :now WHERE id = :id")
    suspend fun updateRemoteId(id: Int, remoteId: String, now: Long = System.currentTimeMillis())

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

    @Transaction
    suspend fun cacheRemoteDocuments(dokumenList: List<MasterDokumenEntity>) {
        dokumenList.forEach { remote ->
            val existing = getByNomorDokumen(remote.nomorDokumen)
            if (existing == null) {
                insert(remote.copy(id = 0))
            } else {
                update(
                    remote.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        remoteId = remote.remoteId
                    )
                )
            }
        }
    }

    @Update
    suspend fun update(dokumen: MasterDokumenEntity)

    @Query("UPDATE master_dokumen SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(dokumen: MasterDokumenEntity)

    @Query("SELECT * FROM master_dokumen WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MasterDokumenEntity?

    @Query("SELECT COUNT(*) FROM master_dokumen")
    suspend fun count(): Int
}
