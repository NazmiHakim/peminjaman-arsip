package com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import javax.inject.Inject

class ApprovePerpanjanganUseCase @Inject constructor(
    private val perpanjanganRepo: PerpanjanganRepository,
    private val auditRepo: AuditLogRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(
        perpanjanganId: Int,
        kasubagId: Int,
        transaksiId: Int
    ): ResultState<Unit> {
        val actor = userRepo.getUserById(kasubagId)
            ?: return ResultState.Error("Pengguna tidak ditemukan")
        if (!actor.isActive || actor.role != UserRole.KASUBAG)
            return ResultState.Error("Hanya Kasubag aktif yang dapat menyetujui perpanjangan")

        val result = perpanjanganRepo.approve(perpanjanganId, kasubagId)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = transaksiId,
                userId = kasubagId,
                aksi = AuditAction.PERPANJANGAN_DISETUJUI,
                detail = "Perpanjangan masa pinjam disetujui. Tenggat diperbarui."
            )
        }
        return result
    }
}
