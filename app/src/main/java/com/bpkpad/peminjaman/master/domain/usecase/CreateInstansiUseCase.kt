package com.bpkpad.peminjaman.master.domain.usecase

import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import javax.inject.Inject

class CreateInstansiUseCase @Inject constructor(
    private val instansiRepo: InstansiRepository,
    private val auditRepo: AuditLogRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(instansi: Instansi, userId: Int): ResultState<Instansi> {
        InputRules.validateAgencyName(instansi.namaInstansi)?.let {
            return ResultState.Error(it)
        }
        InputRules.validateAgencyCode(instansi.kodeInstansi.orEmpty())?.let {
            return ResultState.Error(it)
        }
        InputRules.validateAgencyAddress(instansi.alamat.orEmpty())?.let {
            return ResultState.Error(it)
        }
        val actor = userRepo.getUserById(userId)
            ?: return ResultState.Error("Pengguna tidak ditemukan")
        if (!actor.isActive || actor.role != UserRole.ARSIPARIS) {
            return ResultState.Error("Hanya Arsiparis aktif yang dapat menambah instansi")
        }

        val normalized = instansi.copy(
            namaInstansi = instansi.namaInstansi.trim(),
            kodeInstansi = instansi.kodeInstansi?.trim()?.uppercase()?.ifBlank { null },
            alamat = instansi.alamat?.trim()?.ifBlank { null }
        )
        val result = instansiRepo.create(normalized)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = null,
                userId = userId,
                aksi = AuditAction.MASTER_INSTANSI_DITAMBAH,
                detail = "Instansi baru ditambahkan: ${instansi.namaInstansi}"
            )
        }
        return result
    }
}
