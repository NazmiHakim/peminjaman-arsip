package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import javax.inject.Inject

class CreateTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(transaksi: Transaksi, dokumenIds: List<Int>): ResultState<Transaksi> {
        val actor = userRepo.getUserById(transaksi.createdBy)
            ?: return ResultState.Error("Pengguna tidak ditemukan")
        if (!actor.isActive || actor.role != UserRole.ARSIPARIS)
            return ResultState.Error("Hanya Arsiparis aktif yang dapat membuat pengajuan")

        // --- Input Validation ---
        InputRules.validateWorkUnit(transaksi.namaInstansi)?.let {
            return ResultState.Error(it)
        }
        InputRules.validateApplicantName(transaksi.picNama)?.let {
            return ResultState.Error(it)
        }
        InputRules.validatePhone(transaksi.picNoHp)?.let {
            return ResultState.Error(it)
        }
        InputRules.validateLetterNumber(transaksi.nomorSuratPengantar)?.let {
            return ResultState.Error(it)
        }
        if (transaksi.fotoSuratPengantarPath.isBlank())
            return ResultState.Error("Foto surat pengantar wajib diambil")
        if (!transaksi.tanggalKembaliRencana.isAfter(transaksi.tanggalPinjam))
            return ResultState.Error("Tanggal kembali harus lebih dari tanggal pinjam")
        if (dokumenIds.isEmpty())
            return ResultState.Error("Pilih minimal 1 dokumen")

        if (transaksi.metodePersetujuan == com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan.BYPASS) {
            if (transaksi.buktiBypassPath.isNullOrBlank())
                return ResultState.Error("Foto bukti bypass wajib diupload")
            InputRules.validateBypassNote(transaksi.catatanBypass.orEmpty())?.let {
                return ResultState.Error(it)
            }
        }

        // Generate QR code token if bypassed directly at form
        val qrToken = if (transaksi.metodePersetujuan == com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan.BYPASS) {
            java.util.UUID.randomUUID().toString()
        } else {
            null
        }
        val transaksiToSave = transaksi.copy(qrCodeToken = qrToken)

        val result = transaksiRepo.create(transaksiToSave, dokumenIds)
        if (result is ResultState.Success) {
            val created = result.data
            auditRepo.log(
                transaksiId = created.id,
                userId = transaksi.createdBy,
                aksi = AuditAction.TRANSAKSI_DIBUAT,
                detail = "Transaksi baru dibuat untuk ${transaksi.namaInstansi}"
            )
            if (transaksi.metodePersetujuan == com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan.BYPASS) {
                auditRepo.log(
                    transaksiId = created.id,
                    userId = transaksi.createdBy,
                    aksi = AuditAction.DISETUJUI_BYPASS,
                    detail = "Bypass persetujuan dengan bukti fisik. Catatan: ${transaksi.catatanBypass}"
                )
            } else {
                auditRepo.log(
                    transaksiId = created.id,
                    userId = transaksi.createdBy,
                    aksi = AuditAction.PENGAJUAN_DIKIRIM,
                    detail = "Pengajuan dikirim ke Kasubag untuk persetujuan"
                )
            }
        }
        return result
    }
}
