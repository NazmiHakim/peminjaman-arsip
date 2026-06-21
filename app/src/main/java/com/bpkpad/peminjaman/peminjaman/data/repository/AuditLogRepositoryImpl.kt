package com.bpkpad.peminjaman.peminjaman.data.repository

import com.bpkpad.peminjaman.core.database.dao.AuditLogDao
import com.bpkpad.peminjaman.core.database.dao.UserDao
import com.bpkpad.peminjaman.core.database.entity.AuditLogEntity
import com.bpkpad.peminjaman.peminjaman.domain.model.AuditLog
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao
) : AuditLogRepository {

    override suspend fun log(
        transaksiId: Int?,
        userId: Int,
        aksi: AuditAction,
        detail: String?,
        catatan: String?
    ) {
        try {
            auditLogDao.insert(
                AuditLogEntity(
                    transaksiId = transaksiId,
                    userId = userId,
                    aksi = aksi.code,
                    detail = detail,
                    catatan = catatan,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Audit log failure should never crash the app, but should be logged
            android.util.Log.e("AUDIT", "Failed to write audit log: ${e.message}")
        }
    }

    override fun getByTransaksiId(transaksiId: Int): Flow<List<AuditLog>> {
        return auditLogDao.getAuditTrail(transaksiId).map { list ->
            list.map { entity ->
                val user = userDao.getById(entity.userId)
                entity.toDomain(user?.namaLengkap ?: "Unknown")
            }
        }
    }

    override fun getAll(): Flow<List<AuditLog>> {
        return auditLogDao.getRecent(100).map { list ->
            list.map { entity ->
                val user = userDao.getById(entity.userId)
                entity.toDomain(user?.namaLengkap ?: "Unknown")
            }
        }
    }

    private fun AuditLogEntity.toDomain(namaUser: String) = AuditLog(
        id = id,
        transaksiId = transaksiId,
        userId = userId,
        namaUser = namaUser,
        aksi = AuditAction.fromCode(aksi),
        detail = detail,
        catatan = catatan,
        timestamp = timestamp
    )
}
