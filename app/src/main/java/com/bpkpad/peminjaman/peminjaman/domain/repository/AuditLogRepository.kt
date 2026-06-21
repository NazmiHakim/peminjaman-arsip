package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.peminjaman.domain.model.AuditLog
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    suspend fun log(
        transaksiId: Int?,
        userId: Int,
        aksi: AuditAction,
        detail: String? = null,
        catatan: String? = null
    )
    fun getByTransaksiId(transaksiId: Int): Flow<List<AuditLog>>
    fun getAll(): Flow<List<AuditLog>>
}
