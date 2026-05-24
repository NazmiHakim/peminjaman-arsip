package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class BypassTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(
        transaksiId: Int,
        arsiparisId: Int,
        buktiPath: String,
        catatan: String
    ): ResultState<Unit> {
        if (buktiPath.isBlank())
            return ResultState.Error("Foto bukti bypass wajib diupload")
        if (catatan.isBlank())
            return ResultState.Error("Catatan bypass wajib diisi")

        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeBypassed)
            return ResultState.Error("Hanya transaksi menunggu persetujuan yang dapat di-bypass")

        val result = transaksiRepo.bypass(transaksiId, arsiparisId, buktiPath, catatan)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = arsiparisId,
                aksi = AuditAction.DISETUJUI_BYPASS,
                detail = "Bypass persetujuan dengan bukti fisik. Catatan: $catatan"
            )
        }
        return result
    }
}
