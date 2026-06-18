package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class CreateTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksi: Transaksi, dokumenIds: List<Int>): ResultState<Transaksi> {
        // --- Input Validation ---
        if (transaksi.namaInstansi.isBlank())
            return ResultState.Error("Instansi tidak valid")
        if (transaksi.picNama.isBlank())
            return ResultState.Error("Nama PIC tidak boleh kosong")
        if (transaksi.picNoHp.filter { it.isDigit() }.length < 10)
            return ResultState.Error("No. HP PIC minimal 10 digit")
        if (transaksi.nomorSuratPengantar.isBlank())
            return ResultState.Error("Nomor surat pengantar tidak boleh kosong")
        if (transaksi.fotoSuratPengantarPath.isBlank())
            return ResultState.Error("Foto surat pengantar wajib diambil")
        if (!transaksi.tanggalKembaliRencana.isAfter(transaksi.tanggalPinjam))
            return ResultState.Error("Tanggal kembali harus lebih dari tanggal pinjam")
        if (dokumenIds.isEmpty())
            return ResultState.Error("Pilih minimal 1 dokumen")

        if (transaksi.metodePersetujuan == com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan.BYPASS) {
            if (transaksi.buktiBypassPath.isNullOrBlank())
                return ResultState.Error("Foto bukti bypass wajib diupload")
            if (transaksi.catatanBypass.isNullOrBlank())
                return ResultState.Error("Catatan bypass wajib diisi")
        }

        // Generate QR code token if bypassed directly at form
        val qrToken = if (transaksi.metodePersetujuan == com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan.BYPASS) {
            "QR-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
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
