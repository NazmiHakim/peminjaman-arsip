package com.bpkpad.peminjaman.master.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============== List Dokumen ViewModel ===============
data class ListDokumenUiState(
    val dokumenList: List<MasterDokumen> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ListDokumenViewModel @Inject constructor(
    private val masterDokumenRepo: MasterDokumenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListDokumenUiState())
    val uiState: StateFlow<ListDokumenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            masterDokumenRepo.getAll().collect { list ->
                _uiState.update { it.copy(dokumenList = list, isLoading = false) }
            }
        }
    }

    fun onSearch(q: String) { _uiState.update { it.copy(searchQuery = q) } }
    val filtered get() = uiState.value.let { s ->
        if (s.searchQuery.isBlank()) s.dokumenList
        else s.dokumenList.filter { d -> d.nomorDokumen.contains(s.searchQuery, true) || d.perihal.contains(s.searchQuery, true) }
    }
}

// =============== List Instansi ViewModel ===============
data class ListInstansiUiState(
    val instansiList: List<Instansi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ListInstansiViewModel @Inject constructor(
    private val instansiRepo: InstansiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListInstansiUiState())
    val uiState: StateFlow<ListInstansiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            instansiRepo.getAll().collect { list ->
                _uiState.update { it.copy(instansiList = list, isLoading = false) }
            }
        }
    }
    fun onSearch(q: String) { _uiState.update { it.copy(searchQuery = q) } }
    val filtered get() = uiState.value.let { s ->
        if (s.searchQuery.isBlank()) s.instansiList
        else s.instansiList.filter { i -> i.namaInstansi.contains(s.searchQuery, true) || (i.kodeInstansi?.contains(s.searchQuery, true) == true) }
    }
}
