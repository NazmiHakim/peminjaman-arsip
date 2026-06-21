package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

class BypassTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(
        transaksiId: Int,
        arsiparisId: Int,
        buktiPath: String,
        catatan: String
    ): ResultState<Unit> {
        if (buktiPath.isBlank())
            return ResultState.Error("Foto bukti bypass wajib diupload")
        InputRules.validateBypassNote(catatan)?.let {
            return ResultState.Error(it)
        }

        val actor = userRepo.getUserById(arsiparisId)
            ?: return ResultState.Error("Pengguna tidak ditemukan")
        if (!actor.isActive || actor.role != UserRole.ARSIPARIS)
            return ResultState.Error("Hanya Arsiparis aktif yang dapat melakukan bypass")

        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeBypassed)
            return ResultState.Error("Hanya transaksi menunggu persetujuan yang dapat di-bypass")

        val qrToken = UUID.randomUUID().toString()
        val result = transaksiRepo.bypass(
            transaksiId,
            arsiparisId,
            buktiPath,
            catatan.trim(),
            qrToken
        )
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
