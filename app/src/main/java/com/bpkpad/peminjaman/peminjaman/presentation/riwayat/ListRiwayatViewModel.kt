package com.bpkpad.peminjaman.peminjaman.presentation.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
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
    val isLoading: Boolean = true
)

@HiltViewModel
class ListRiwayatViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val instansiRepo: InstansiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListRiwayatUiState())
    val uiState: StateFlow<ListRiwayatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val instansiMap = mutableMapOf<Int, String>()
            instansiRepo.getAll().firstOrNull()?.forEach { instansiMap[it.id] = it.namaInstansi }

            transaksiRepo.getAll().collect { list ->
                val withNames = list.map { t -> t.copy(namaInstansi = instansiMap[t.instansiId] ?: "Instansi #${t.instansiId}") }
                _uiState.update { it.copy(allList = withNames, isLoading = false) }
                applyFilter()
            }
        }
    }

    fun onSearch(q: String) { _uiState.update { it.copy(searchQuery = q) }; applyFilter() }
    fun onStatusFilter(status: TransaksiStatus?) { _uiState.update { it.copy(selectedStatus = status) }; applyFilter() }

    private fun applyFilter() {
        val s = _uiState.value
        val filtered = s.allList.filter { t ->
            val matchQuery = s.searchQuery.isBlank() || t.namaInstansi.contains(s.searchQuery, true) ||
                t.picNama.contains(s.searchQuery, true) || t.nomorSuratPengantar.contains(s.searchQuery, true)
            val matchStatus = s.selectedStatus == null || t.status == s.selectedStatus
            matchQuery && matchStatus
        }
        _uiState.update { it.copy(filteredList = filtered) }
    }
}
