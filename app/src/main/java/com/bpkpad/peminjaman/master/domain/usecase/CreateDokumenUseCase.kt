package com.bpkpad.peminjaman.master.domain.usecase

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import javax.inject.Inject

class CreateDokumenUseCase @Inject constructor(
    private val dokumenRepo: MasterDokumenRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(dokumen: MasterDokumen, userId: Int): ResultState<MasterDokumen> {
        if (dokumen.nomorDokumen.isBlank())
            return ResultState.Error("Nomor dokumen tidak boleh kosong")
        if (dokumen.perihal.isBlank())
            return ResultState.Error("Perihal tidak boleh kosong")
        if (dokumen.tahun.isBlank())
            return ResultState.Error("Tahun tidak boleh kosong")
        if (dokumen.jenisDokumen.isBlank())
            return ResultState.Error("Jenis dokumen tidak boleh kosong")

        val result = dokumenRepo.create(dokumen)
        if (result is ResultState.Success) {
            // transaksiId = 0 is the convention for master data / non-transaction audit entries
            auditRepo.log(
                transaksiId = 0,
                userId = userId,
                aksi = AuditAction.MASTER_DOKUMEN_DITAMBAH,
                detail = "Dokumen baru: ${dokumen.nomorDokumen} - ${dokumen.perihal} (${dokumen.tahun})"
            )
        }
        return result
    }
}
