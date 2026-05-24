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
        if (transaksi.instansiId <= 0)
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

        val result = transaksiRepo.create(transaksi, dokumenIds)
        if (result is ResultState.Success) {
            val created = result.data
            auditRepo.log(
                transaksiId = created.id,
                userId = transaksi.createdBy,
                aksi = AuditAction.TRANSAKSI_DIBUAT,
                detail = "Transaksi baru dibuat untuk ${transaksi.namaInstansi}"
            )
            auditRepo.log(
                transaksiId = created.id,
                userId = transaksi.createdBy,
                aksi = AuditAction.PENGAJUAN_DIKIRIM,
                detail = "Pengajuan dikirim ke Kasubag untuk persetujuan"
            )
        }
        return result
    }
}
