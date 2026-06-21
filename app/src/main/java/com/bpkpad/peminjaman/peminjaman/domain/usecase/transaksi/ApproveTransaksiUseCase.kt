package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import java.util.UUID
import javax.inject.Inject

class ApproveTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, approverId: Int, catatan: String?): ResultState<Unit> {
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeApproved)
            return ResultState.Error("Status transaksi tidak valid untuk disetujui")

        val qrToken = UUID.randomUUID().toString()
        val result = transaksiRepo.approve(transaksiId, approverId, qrToken)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = approverId,
                aksi = AuditAction.DISETUJUI_ONLINE,
                detail = "Status berubah: menunggu_persetujuan → disetujui. QR Token: $qrToken",
                catatan = catatan
            )
        }
        return result
    }
}
