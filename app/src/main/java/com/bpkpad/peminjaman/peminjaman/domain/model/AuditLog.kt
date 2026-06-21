package com.bpkpad.peminjaman.peminjaman.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction

data class AuditLog(
    val id: Int,
    val transaksiId: Int?,
    val userId: Int,
    val namaUser: String,
    val aksi: AuditAction,
    val detail: String?,
    val catatan: String?,
    val timestamp: Long
)
