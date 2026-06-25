package com.bpkpad.peminjaman.core.database.dao

import androidx.room.*
import com.bpkpad.peminjaman.core.database.entity.TransaksiEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransaksiDao {
    @Query("SELECT * FROM transaksi_peminjaman ORDER BY id DESC")
    fun getAll(): Flow<List<TransaksiEntity>>

    @Query("SELECT * FROM transaksi_peminjaman WHERE status = :status ORDER BY id DESC")
    fun getByStatus(status: String): Flow<List<TransaksiEntity>>

    @Query("""
        SELECT * FROM transaksi_peminjaman 
        WHERE status = 'dipinjam' AND tanggal_kembali_rencana < :today 
        ORDER BY tanggal_kembali_rencana ASC
    """)
    fun getOverdue(today: String): Flow<List<TransaksiEntity>>

    @Query("""
        SELECT * FROM transaksi_peminjaman 
        WHERE status = 'dipinjam' AND tanggal_kembali_rencana < :today 
        ORDER BY tanggal_kembali_rencana ASC
    """)
    suspend fun getOverdueSync(today: String): List<TransaksiEntity>

    @Query("SELECT * FROM transaksi_peminjaman WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TransaksiEntity?

    @Query("SELECT * FROM transaksi_peminjaman WHERE sync_state = 'pending' ORDER BY id ASC")
    suspend fun getPendingSync(): List<TransaksiEntity>

    @Query("""
        UPDATE transaksi_peminjaman
        SET remote_id = :remoteId,
            sync_state = 'synced',
            last_sync_error = NULL,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun markSynced(
        id: Int,
        remoteId: String,
        now: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE transaksi_peminjaman
        SET sync_state = 'pending',
            last_sync_error = :error,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun markSyncPending(
        id: Int,
        error: String?,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM transaksi_peminjaman WHERE qr_code_token = :token LIMIT 1")
    suspend fun findByQrToken(token: String): TransaksiEntity?

    @Query("""
        SELECT * FROM transaksi_peminjaman 
        WHERE status = 'disetujui' AND metode_persetujuan = 'bypass' AND is_bypass_acknowledged = 0
        ORDER BY created_at DESC
    """)
    fun getBypassPendingAcknowledge(): Flow<List<TransaksiEntity>>

    @Query("""
        SELECT * FROM transaksi_peminjaman 
        WHERE nama_instansi = :namaInstansi 
        ORDER BY created_at DESC
    """)
    fun getByInstansi(namaInstansi: String): Flow<List<TransaksiEntity>>

    @Query("""
        SELECT COUNT(*) FROM transaksi_peminjaman WHERE status = :status
    """)
    suspend fun countByStatus(status: String): Int

    @Query("""
        SELECT COUNT(*) FROM transaksi_peminjaman 
        WHERE status = 'dipinjam' AND tanggal_kembali_rencana < :today
    """)
    suspend fun countOverdue(today: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaksi: TransaksiEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transaksiList: List<TransaksiEntity>)

    @Update
    suspend fun update(transaksi: TransaksiEntity)

    @Query("UPDATE transaksi_peminjaman SET status = :status, qr_code_token = :qrToken, approved_by = :approverId, metode_persetujuan = 'online', updated_at = :now WHERE id = :id")
    suspend fun approve(id: Int, approverId: Int, qrToken: String, status: String = "disetujui", now: Long = System.currentTimeMillis())

    @Query("UPDATE transaksi_peminjaman SET status = 'ditolak', alasan_penolakan = :alasan, approved_by = :approverId, updated_at = :now WHERE id = :id")
    suspend fun reject(id: Int, approverId: Int, alasan: String, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE transaksi_peminjaman
        SET status = 'disetujui',
            metode_persetujuan = 'bypass',
            bukti_bypass_path = :buktiPath,
            catatan_bypass = :catatan,
            qr_code_token = :qrToken,
            approved_by = :arsiparisId,
            is_bypass_acknowledged = 0,
            updated_at = :now
        WHERE id = :id
          AND status = 'menunggu_persetujuan'
    """)
    suspend fun bypass(
        id: Int,
        arsiparisId: Int,
        buktiPath: String,
        catatan: String,
        qrToken: String,
        now: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE transaksi_peminjaman SET is_bypass_acknowledged = 1, updated_at = :now WHERE id = :id")
    suspend fun acknowledgeBypass(id: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE transaksi_peminjaman SET status = 'dipinjam', updated_at = :now WHERE id = :id")
    suspend fun confirmHandover(id: Int, now: Long = System.currentTimeMillis())

    // --- TAMBAHAN TASK 9: qr_code_token = NULL ---
    @Query("UPDATE transaksi_peminjaman SET status = 'dikembalikan', tanggal_kembali_aktual = :actualDate, qr_code_token = NULL, updated_at = :now WHERE id = :id")
    suspend fun returnTransaksi(id: Int, actualDate: LocalDate, now: Long = System.currentTimeMillis())

    @Query("UPDATE transaksi_peminjaman SET status = 'dibatalkan', updated_at = :now WHERE id = :id")
    suspend fun cancel(id: Int, now: Long = System.currentTimeMillis())
    @Query("UPDATE transaksi_peminjaman SET tanggal_kembali_rencana = :newDate, updated_at = :now WHERE id = :id AND status = 'dipinjam'")
    suspend fun updateTanggalKembali(id: Int, newDate: LocalDate, now: Long = System.currentTimeMillis()): Int

    @Delete
    suspend fun delete(transaksi: TransaksiEntity)

    @Query("SELECT * FROM transaksi_peminjaman WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TransaksiEntity?

    @Query("SELECT * FROM transaksi_peminjaman WHERE sync_key = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): TransaksiEntity?

    @Query("SELECT COUNT(*) FROM transaksi_peminjaman")
    suspend fun count(): Int
}
