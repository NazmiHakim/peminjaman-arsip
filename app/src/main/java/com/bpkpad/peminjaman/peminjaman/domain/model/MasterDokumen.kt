package com.bpkpad.peminjaman.peminjaman.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus

data class MasterDokumen(
    val id: Int,
    val nomorDokumen: String,
    val perihal: String,
    val nominal: Double,
    val tahun: String,
    val jenisDokumen: String,
    val status: DokumenStatus,
    val lokasiRak: String?,
    val lokasiBox: String?
)
