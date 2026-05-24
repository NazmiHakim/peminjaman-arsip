package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class AcknowledgeBypassUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, kasubagId: Int): ResultState<Unit> {
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.needsBypassAcknowledge)
            return ResultState.Error("Transaksi ini tidak perlu verifikasi bypass")

        val result = transaksiRepo.acknowledgeBypass(transaksiId, kasubagId)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = kasubagId,
                aksi = AuditAction.BYPASS_DIAKUI_KASUBAG,
                detail = "Kasubag mengakui bypass persetujuan fisik"
            )
        }
        return result
    }
}
