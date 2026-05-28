package com.bpkpad.peminjaman.laporan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import com.bpkpad.peminjaman.laporan.domain.usecase.ExportExcelUseCase
import com.bpkpad.peminjaman.laporan.domain.usecase.ExportPdfUseCase
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LaporanUiState(
    val dateFrom: String = "",
    val dateTo: String = "",
    val totalAll: Int = 0,
    val totalFiltered: Int = 0,
    val totalMenunggu: Int = 0,
    val totalDipinjam: Int = 0,
    val totalOverdue: Int = 0,
    val totalDikembalikan: Int = 0,
    val filterError: String? = null,
    val isExportingPdf: Boolean = false,
    val isExportingExcel: Boolean = false,
    val exportMessage: String? = null,
    val exportedReport: ExportedReport? = null
)

@HiltViewModel
class LaporanViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val exportPdfUseCase: ExportPdfUseCase,
    private val exportExcelUseCase: ExportExcelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaporanUiState())
    val uiState: StateFlow<LaporanUiState> = _uiState.asStateFlow()
    private var allTransaksi: List<Transaksi> = emptyList()
    private var filteredTransaksi: List<Transaksi> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                transaksiRepo.getAll(),
                transaksiRepo.getOverdue()
            ) { all, overdue ->
                allTransaksi = all
                recalculateState(overdue)
            }.collect()
        }
    }

    fun onFilterChange(from: String, to: String) {
        _uiState.update { it.copy(dateFrom = from, dateTo = to, exportedReport = null) }
        recalculateState()
    }

    fun exportPdf() {
        val period = parsePeriodOrUpdateError() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExportingPdf = true, exportMessage = null, exportedReport = null) }
            when (val result = exportPdfUseCase(filteredTransaksi, period.first, period.second)) {
                is ResultState.Success -> _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        exportedReport = result.data,
                        exportMessage = "PDF berhasil dibuat: ${result.data.fileName}"
                    )
                }
                is ResultState.Error -> _uiState.update {
                    it.copy(isExportingPdf = false, exportMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun exportExcel() {
        val period = parsePeriodOrUpdateError() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExportingExcel = true, exportMessage = null, exportedReport = null) }
            when (val result = exportExcelUseCase(filteredTransaksi, period.first, period.second)) {
                is ResultState.Success -> _uiState.update {
                    it.copy(
                        isExportingExcel = false,
                        exportedReport = result.data,
                        exportMessage = "Excel berhasil dibuat: ${result.data.fileName}"
                    )
                }
                is ResultState.Error -> _uiState.update {
                    it.copy(isExportingExcel = false, exportMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(exportMessage = null) } }
    fun clearExportedReport() { _uiState.update { it.copy(exportedReport = null) } }

    private fun recalculateState(overdueSnapshot: List<Transaksi>? = null) {
        val current = _uiState.value
        val period = parsePeriod(current.dateFrom, current.dateTo)
        val filtered = if (period.error == null) {
            allTransaksi.filterByPeriod(period.from, period.to)
        } else {
            allTransaksi
        }
        filteredTransaksi = filtered
        val overdue = overdueSnapshot?.filterByPeriod(period.from, period.to)
            ?: filtered.filter { it.isOverdue }
        _uiState.update {
            it.copy(
                totalAll = allTransaksi.size,
                totalFiltered = filtered.size,
                totalMenunggu = filtered.count { t -> t.status == TransaksiStatus.MENUNGGU_PERSETUJUAN },
                totalDipinjam = filtered.count { t -> t.status == TransaksiStatus.DIPINJAM },
                totalOverdue = overdue.size,
                totalDikembalikan = filtered.count { t -> t.status == TransaksiStatus.DIKEMBALIKAN },
                filterError = period.error
            )
        }
    }

    private fun parsePeriodOrUpdateError(): Pair<LocalDate?, LocalDate?>? {
        val current = _uiState.value
        val period = parsePeriod(current.dateFrom, current.dateTo)
        if (period.error != null) {
            _uiState.update { it.copy(filterError = period.error, exportMessage = period.error) }
            return null
        }
        return period.from to period.to
    }

    private fun parsePeriod(from: String, to: String): ParsedPeriod {
        val start = if (from.isBlank()) null else try {
            LocalDate.parse(from.trim())
        } catch (e: Exception) {
            return ParsedPeriod(error = "Format tanggal Dari tidak valid. Gunakan YYYY-MM-DD.")
        }
        val end = if (to.isBlank()) null else try {
            LocalDate.parse(to.trim())
        } catch (e: Exception) {
            return ParsedPeriod(error = "Format tanggal Sampai tidak valid. Gunakan YYYY-MM-DD.")
        }
        if (start != null && end != null && start.isAfter(end)) {
            return ParsedPeriod(error = "Tanggal Dari tidak boleh lebih besar dari Sampai.")
        }
        return ParsedPeriod(from = start, to = end)
    }

    private fun List<Transaksi>.filterByPeriod(from: LocalDate?, to: LocalDate?): List<Transaksi> {
        return filter { transaksi ->
            val afterStart = from == null || !transaksi.tanggalPinjam.isBefore(from)
            val beforeEnd = to == null || !transaksi.tanggalPinjam.isAfter(to)
            afterStart && beforeEnd
        }
    }

    private data class ParsedPeriod(
        val from: LocalDate? = null,
        val to: LocalDate? = null,
        val error: String? = null
    )
}
