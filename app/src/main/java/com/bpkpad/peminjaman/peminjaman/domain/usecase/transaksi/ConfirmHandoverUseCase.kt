package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class ConfirmHandoverUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, arsiparisId: Int): ResultState<Unit> {
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (transaksi.status != TransaksiStatus.DISETUJUI)
            return ResultState.Error("Transaksi harus berstatus Disetujui untuk konfirmasi serah")

        val result = transaksiRepo.confirmHandover(transaksiId, arsiparisId)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = arsiparisId,
                aksi = AuditAction.DOKUMEN_DISERAHKAN,
                detail = "Dokumen diserahkan ke peminjam. Status berubah: disetujui → dipinjam"
            )
        }
        return result
    }
}
