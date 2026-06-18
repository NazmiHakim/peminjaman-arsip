package com.bpkpad.peminjaman.peminjaman.presentation.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListRiwayatUiState(
    val allList: List<Transaksi> = emptyList(),
    val filteredList: List<Transaksi> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: TransaksiStatus? = null,
    val isOverdueOnly: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ListRiwayatViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListRiwayatUiState())
    val uiState: StateFlow<ListRiwayatUiState> = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            transaksiRepo.getAll().collect { list ->
                _uiState.update { it.copy(allList = list, isLoading = false) }
                applyFilter()
            }
        }
    }

    // Fungsi untuk menangkap filter awal dari Dashboard
    fun setInitialFilter(statusString: String?) {
        if (statusString.isNullOrBlank() || statusString == "ALL") {
            _uiState.update { it.copy(selectedStatus = null, isOverdueOnly = false) }
        } else if (statusString == "TERLAMBAT") {
            _uiState.update { it.copy(selectedStatus = TransaksiStatus.DIPINJAM, isOverdueOnly = true) }
        } else {
            try {
                val status = TransaksiStatus.valueOf(statusString)
                _uiState.update { it.copy(selectedStatus = status, isOverdueOnly = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isOverdueOnly = false) }
            }
        }
        applyFilter()
    }

    fun onSearch(q: String) { _uiState.update { it.copy(searchQuery = q) }; applyFilter() }

    fun onStatusFilter(status: TransaksiStatus?) {
        _uiState.update { it.copy(selectedStatus = status, isOverdueOnly = false) }
        applyFilter()
    }

    fun onOverdueToggle(active: Boolean) {
        _uiState.update { it.copy(isOverdueOnly = active) }
        applyFilter()
    }

    private fun applyFilter() {
        val s = _uiState.value
        val filtered = s.allList.filter { t ->
            val matchQuery = s.searchQuery.isBlank() || t.namaInstansi.contains(s.searchQuery, true) ||
                    t.picNama.contains(s.searchQuery, true) || t.nomorSuratPengantar.contains(s.searchQuery, true)

            val matchStatus = if (s.isOverdueOnly) {
                t.status == TransaksiStatus.DIPINJAM && t.isOverdue // Filter khusus terlambat
            } else {
                s.selectedStatus == null || t.status == s.selectedStatus
            }

            matchQuery && matchStatus
        }
        _uiState.update { it.copy(filteredList = filtered) }
    }
}