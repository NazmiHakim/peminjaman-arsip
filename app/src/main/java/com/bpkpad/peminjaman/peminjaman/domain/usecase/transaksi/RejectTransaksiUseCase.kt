package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class RejectTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, approverId: Int, alasan: String): ResultState<Unit> {
        if (alasan.isBlank())
            return ResultState.Error("Alasan penolakan wajib diisi")
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeApproved)
            return ResultState.Error("Hanya transaksi menunggu persetujuan yang dapat ditolak")

        val result = transaksiRepo.reject(transaksiId, approverId, alasan)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = approverId,
                aksi = AuditAction.DITOLAK,
                detail = "Pengajuan ditolak: $alasan"
            )
        }
        return result
    }
}
