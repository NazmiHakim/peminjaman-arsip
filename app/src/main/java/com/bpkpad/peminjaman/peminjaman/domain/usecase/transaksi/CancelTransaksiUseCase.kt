package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class CancelTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, userId: Int): ResultState<Unit> {
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeCancelled)
            return ResultState.Error("Transaksi tidak dapat dibatalkan pada status ini")

        val result = transaksiRepo.cancel(transaksiId, userId)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = userId,
                aksi = AuditAction.DIBATALKAN,
                detail = "Transaksi dibatalkan. Status sebelumnya: ${transaksi.status.displayName}"
            )
        }
        return result
    }
}
