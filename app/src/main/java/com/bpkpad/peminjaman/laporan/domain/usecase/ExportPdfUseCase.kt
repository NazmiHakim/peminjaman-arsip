package com.bpkpad.peminjaman.laporan.domain.usecase

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import com.bpkpad.peminjaman.laporan.domain.repository.LaporanExportRepository
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import java.time.LocalDate
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val laporanExportRepository: LaporanExportRepository
) {
    suspend operator fun invoke(
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ): ResultState<ExportedReport> {
        return if (transaksi.isEmpty()) {
            ResultState.Error("Tidak ada data transaksi untuk diekspor")
        } else {
            laporanExportRepository.exportPdf(transaksi, periodeMulai, periodeSampai)
        }
    }
}
