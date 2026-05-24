package com.bpkpad.peminjaman.master.domain.usecase

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import javax.inject.Inject

class CreateInstansiUseCase @Inject constructor(
    private val instansiRepo: InstansiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(instansi: Instansi, userId: Int): ResultState<Instansi> {
        if (instansi.namaInstansi.isBlank())
            return ResultState.Error("Nama instansi tidak boleh kosong")

        val result = instansiRepo.create(instansi)
        if (result is ResultState.Success) {
            // transaksiId = 0 is the convention for master data / non-transaction audit entries
            auditRepo.log(
                transaksiId = 0,
                userId = userId,
                aksi = AuditAction.MASTER_INSTANSI_DITAMBAH,
                detail = "Instansi baru ditambahkan: ${instansi.namaInstansi}"
            )
        }
        return result
    }
}
