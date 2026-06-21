package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.InstansiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstansiDao {
    @Query("SELECT * FROM instansi_peminjam ORDER BY nama_instansi ASC")
    fun getAll(): Flow<List<InstansiEntity>>

    @Query("SELECT * FROM instansi_peminjam WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): InstansiEntity?

    @Query("SELECT * FROM instansi_peminjam WHERE lower(nama_instansi) = lower(:name) LIMIT 1")
    suspend fun getByName(name: String): InstansiEntity?

    @Query("UPDATE instansi_peminjam SET remote_id = :remoteId, updated_at = :now WHERE id = :id")
    suspend fun updateRemoteId(id: Int, remoteId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM instansi_peminjam WHERE nama_instansi LIKE '%' || :query || '%' OR kode_instansi LIKE '%' || :query || '%' ORDER BY nama_instansi ASC")
    fun search(query: String): Flow<List<InstansiEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(instansi: InstansiEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(instansiList: List<InstansiEntity>)

    @Update
    suspend fun update(instansi: InstansiEntity)

    @Delete
    suspend fun delete(instansi: InstansiEntity)

    @Query("SELECT COUNT(*) FROM instansi_peminjam")
    suspend fun count(): Int
}
