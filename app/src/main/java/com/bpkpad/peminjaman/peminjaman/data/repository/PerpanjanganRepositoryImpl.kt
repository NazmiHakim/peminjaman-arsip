package com.bpkpad.peminjaman.peminjaman.data.repository

import androidx.room.withTransaction
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.AppDatabase
import com.bpkpad.peminjaman.core.database.dao.PerpanjanganDao
import com.bpkpad.peminjaman.core.database.dao.TransaksiDao
import com.bpkpad.peminjaman.core.database.entity.PerpanjanganEntity
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.PerpanjanganStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerpanjanganRepositoryImpl @Inject constructor(
    private val perpanjanganDao: PerpanjanganDao,
    private val transaksiDao: TransaksiDao,
    private val database: AppDatabase
) : PerpanjanganRepository {

    override fun getPendingAll(): Flow<List<Perpanjangan>> =
        perpanjanganDao.getPendingAll().map { list -> list.map { it.toDomain() } }

    override fun getByTransaksiId(transaksiId: Int): Flow<List<Perpanjangan>> =
        perpanjanganDao.getByTransaksiId(transaksiId).map { list -> list.map { it.toDomain() } }

    override suspend fun create(perpanjangan: Perpanjangan): ResultState<Perpanjangan> {
        return try {
            val id = perpanjanganDao.insertIfNoPending(perpanjangan.toEntity()).toInt()
            if (id < 0) {
                return ResultState.Error("Masih ada pengajuan perpanjangan yang menunggu persetujuan")
            }
            ResultState.Success(perpanjangan.copy(id = id))
        } catch (e: Exception) {
            ResultState.Error("Gagal membuat perpanjangan: ${e.message}", e)
        }
    }

    override suspend fun approve(perpanjanganId: Int, approverId: Int): ResultState<Unit> {
        return try {
            val perpanjangan = perpanjanganDao.getById(perpanjanganId)
                ?: return ResultState.Error("Perpanjangan tidak ditemukan")
            database.withTransaction {
                val updatedExtension = perpanjanganDao.approve(perpanjanganId, approverId)
                if (updatedExtension != 1) {
                    error("Perpanjangan sudah diproses")
                }
                val updatedTransaction = transaksiDao.updateTanggalKembali(
                    perpanjangan.transaksiId,
                    perpanjangan.tanggalKembaliBaru
                )
                if (updatedTransaction != 1) {
                    error("Transaksi tidak lagi berstatus dipinjam")
                }
            }
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menyetujui perpanjangan: ${e.message}", e)
        }
    }

    override suspend fun reject(perpanjanganId: Int, approverId: Int, alasan: String): ResultState<Unit> {
        return try {
            if (perpanjanganDao.reject(perpanjanganId, approverId, alasan) == 1) {
                ResultState.Success(Unit)
            } else {
                ResultState.Error("Perpanjangan sudah diproses")
            }
        } catch (e: Exception) {
            ResultState.Error("Gagal menolak perpanjangan: ${e.message}", e)
        }
    }

    private fun PerpanjanganEntity.toDomain() = Perpanjangan(
        id = id,
        transaksiId = transaksiId,
        tanggalKembaliLama = tanggalKembaliLama,
        tanggalKembaliBaru = tanggalKembaliBaru,
        fotoSuratPerpanjanganPath = fotoSuratPerpanjanganPath,
        alasan = alasan,
        status = PerpanjanganStatus.fromString(status),
        alasanPenolakan = alasanPenolakan,
        createdBy = createdBy,
        approvedBy = approvedBy,
        createdAt = createdAt
    )

    private fun Perpanjangan.toEntity() = PerpanjanganEntity(
        id = id,
        transaksiId = transaksiId,
        tanggalKembaliLama = tanggalKembaliLama,
        tanggalKembaliBaru = tanggalKembaliBaru,
        fotoSuratPerpanjanganPath = fotoSuratPerpanjanganPath,
        alasan = alasan,
        status = status.name.lowercase(),
        alasanPenolakan = alasanPenolakan,
        createdBy = createdBy,
        approvedBy = approvedBy
    )
}
