package com.bpkpad.peminjaman.laporan.domain.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import java.time.LocalDate

interface LaporanExportRepository {
    suspend fun exportPdf(
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ): ResultState<ExportedReport>

    suspend fun exportExcel(
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ): ResultState<ExportedReport>
}
