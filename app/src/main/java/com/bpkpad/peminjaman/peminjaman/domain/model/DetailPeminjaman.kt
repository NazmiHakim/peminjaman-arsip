package com.bpkpad.peminjaman.peminjaman.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.KondisiPengembalian

data class DetailPeminjaman(
    val id: Int,
    val transaksiId: Int,
    val dokumenId: Int,
    val nomorDokumen: String,
    val perihalDokumen: String?,
    val tahunDokumen: String?,
    val lokasiRak: String?,
    val kondisiPengembalian: KondisiPengembalian?,
    val catatanKondisi: String?
)
