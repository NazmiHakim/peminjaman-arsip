package com.bpkpad.peminjaman.laporan.domain.model

data class ExportedReport(
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val totalRows: Int
)
