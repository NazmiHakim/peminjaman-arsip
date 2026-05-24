package com.bpkpad.peminjaman.peminjaman.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.PerpanjanganStatus
import java.time.LocalDate

data class Perpanjangan(
    val id: Int,
    val transaksiId: Int,
    val tanggalKembaliLama: LocalDate,
    val tanggalKembaliBaru: LocalDate,
    val fotoSuratPerpanjanganPath: String,
    val alasan: String,
    val status: PerpanjanganStatus,
    val alasanPenolakan: String?,
    val createdBy: Int,
    val approvedBy: Int?,
    val createdAt: Long
)
