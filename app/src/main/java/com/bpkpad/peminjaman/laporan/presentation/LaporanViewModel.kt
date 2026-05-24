package com.bpkpad.peminjaman.laporan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaporanUiState(
    val dateFrom: String = "",
    val dateTo: String = "",
    val totalAll: Int = 0,
    val totalDipinjam: Int = 0,
    val totalOverdue: Int = 0,
    val isExportingPdf: Boolean = false,
    val isExportingExcel: Boolean = false,
    val exportMessage: String? = null
)

@HiltViewModel
class LaporanViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaporanUiState())
    val uiState: StateFlow<LaporanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transaksiRepo.getAll(),
                transaksiRepo.getOverdue()
            ) { all, overdue ->
                _uiState.update {
                    it.copy(
                        totalAll      = all.size,
                        totalDipinjam = all.count { t -> t.status == TransaksiStatus.DIPINJAM },
                        totalOverdue  = overdue.size
                    )
                }
            }.collect()
        }
    }

    fun onFilterChange(from: String, to: String) { _uiState.update { it.copy(dateFrom = from, dateTo = to) } }

    fun exportPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            // TODO: Implement PdfDocument export using ExportPdfUseCase
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(isExportingPdf = false, exportMessage = "Export PDF berhasil (fitur dalam pengembangan)") }
        }
    }

    fun exportExcel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingExcel = true) }
            // TODO: Implement Apache POI Excel export using ExportExcelUseCase
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(isExportingExcel = false, exportMessage = "Export Excel berhasil (fitur dalam pengembangan)") }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(exportMessage = null) } }
}
