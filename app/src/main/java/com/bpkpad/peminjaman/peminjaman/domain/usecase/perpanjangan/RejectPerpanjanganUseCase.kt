package com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import javax.inject.Inject

class RejectPerpanjanganUseCase @Inject constructor(
    private val perpanjanganRepo: PerpanjanganRepository,
    private val auditRepo: AuditLogRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(
        perpanjanganId: Int,
        kasubagId: Int,
        transaksiId: Int,
        alasan: String
    ): ResultState<Unit> {
        if (alasan.isBlank())
            return ResultState.Error("Alasan penolakan wajib diisi")
        if (alasan.length > 500)
            return ResultState.Error("Alasan penolakan maksimal 500 karakter")

        val actor = userRepo.getUserById(kasubagId)
            ?: return ResultState.Error("Pengguna tidak ditemukan")
        if (!actor.isActive || actor.role != UserRole.KASUBAG)
            return ResultState.Error("Hanya Kasubag aktif yang dapat menolak perpanjangan")

        val result = perpanjanganRepo.reject(perpanjanganId, kasubagId, alasan)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = kasubagId,
                aksi = AuditAction.PERPANJANGAN_DITOLAK,
                detail = "Perpanjangan ditolak: $alasan. Tenggat tidak berubah."
            )
        }
        return result
    }
}
