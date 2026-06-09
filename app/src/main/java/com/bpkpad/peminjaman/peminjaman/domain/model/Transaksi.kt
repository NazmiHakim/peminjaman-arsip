package com.bpkpad.peminjaman.peminjaman.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import java.time.LocalDate

data class Transaksi(
    val id: Int,
    val namaInstansi: String,
    val picNama: String,
    val picNoHp: String,
    val nomorSuratPengantar: String,
    val fotoSuratPengantarPath: String,
    val qrCodeToken: String?,
    val tanggalPinjam: LocalDate,
    val tanggalKembaliRencana: LocalDate,
    val tanggalKembaliAktual: LocalDate?,
    val status: TransaksiStatus,
    val metodePersetujuan: MetodePersetujuan?,
    val buktiBypassPath: String?,
    val catatanBypass: String?,
    val isBypassAcknowledged: Boolean,
    val alasanPenolakan: String?,
    val createdBy: Int,
    val namaCreatedBy: String,
    val approvedBy: Int?,
    val namaApprovedBy: String?,
    val createdAt: Long,
    val details: List<DetailPeminjaman> = emptyList(),
    val perpanjangan: List<Perpanjangan> = emptyList()
) {
    val isOverdue: Boolean
        get() = status == TransaksiStatus.DIPINJAM && tanggalKembaliRencana.isBefore(LocalDate.now())

    val daysOverdue: Long
        get() = if (isOverdue) {
            java.time.temporal.ChronoUnit.DAYS.between(tanggalKembaliRencana, LocalDate.now())
        } else 0L

    val canBeApproved: Boolean get() = status == TransaksiStatus.MENUNGGU_PERSETUJUAN
    val canBeReturned: Boolean get() = status == TransaksiStatus.DIPINJAM
    val canBeExtended: Boolean get() = status == TransaksiStatus.DIPINJAM
    val canBeBypassed: Boolean get() = status == TransaksiStatus.MENUNGGU_PERSETUJUAN
    val canBeCancelled: Boolean
        get() = status == TransaksiStatus.MENUNGGU_PERSETUJUAN || status == TransaksiStatus.DISETUJUI
    val needsBypassAcknowledge: Boolean
        get() = metodePersetujuan == MetodePersetujuan.BYPASS && !isBypassAcknowledged
}
