package com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import java.time.LocalDate
import javax.inject.Inject

class CreatePerpanjanganUseCase @Inject constructor(
    private val perpanjanganRepo: PerpanjanganRepository,
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(perpanjangan: Perpanjangan): ResultState<Perpanjangan> {
        // --- Input Validation ---
        if (perpanjangan.fotoSuratPerpanjanganPath.isBlank())
            return ResultState.Error("Foto surat perpanjangan wajib diupload")
        if (perpanjangan.alasan.isBlank())
            return ResultState.Error("Alasan perpanjangan wajib diisi")
        if (!perpanjangan.tanggalKembaliBaru.isAfter(LocalDate.now()))
            return ResultState.Error("Tanggal kembali baru harus lebih dari hari ini")

        val transaksi = transaksiRepo.getById(perpanjangan.transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (transaksi.status != TransaksiStatus.DIPINJAM)
            return ResultState.Error("Perpanjangan hanya untuk transaksi yang sedang dipinjam")

        val result = perpanjanganRepo.create(perpanjangan)
        if (result is ResultState.Success) {
            auditRepo.log(
                transaksiId = perpanjangan.transaksiId,
                userId = perpanjangan.createdBy,
                aksi = AuditAction.PERPANJANGAN_DIAJUKAN,
                detail = "Perpanjangan diajukan. Tenggat baru: ${perpanjangan.tanggalKembaliBaru}"
            )
        }
        return result
    }
}
