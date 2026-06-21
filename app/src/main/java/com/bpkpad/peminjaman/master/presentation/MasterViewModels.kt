package com.bpkpad.peminjaman.master.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.master.domain.usecase.CreateInstansiUseCase
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
// =============== List Instansi ViewModel ===============
data class ListInstansiUiState(
    val instansiList: List<Instansi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ListInstansiViewModel @Inject constructor(
    private val instansiRepo: InstansiRepository,
    private val createInstansiUseCase: com.bpkpad.peminjaman.master.domain.usecase.CreateInstansiUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    // 1. Simpan apa yang diketik user di kolom pencarian
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()
    // -------------------------------------------------------

    val uiState: StateFlow<ListInstansiUiState> = combine(
        instansiRepo.getAll(),
        _searchQuery
    ) { listDariDb, ketikan ->
        val hasilFilter = if (ketikan.isBlank()) {
            listDariDb
        } else {
            listDariDb.filter {
                it.namaInstansi.contains(ketikan, ignoreCase = true) ||
                        (it.kodeInstansi?.contains(ketikan, ignoreCase = true) == true)
            }
        }

        ListInstansiUiState(
            instansiList = hasilFilter,
            searchQuery = ketikan,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListInstansiUiState(isLoading = true)
    )

    fun onSearch(q: String) {
        _searchQuery.value = InputRules.filterAgencyName(q)
    }

    fun addInstansi(nama: String, kode: String, alamat: String) {
        viewModelScope.launch {
            val userId = sessionManager.session.firstOrNull()?.userId ?: run {
                _errorMessage.emit("Sesi tidak valid")
                return@launch
            }
            val newInstansi = Instansi(
                id = 0,
                namaInstansi = InputRules.filterAgencyName(nama),
                alamat = InputRules.filterAgencyAddress(alamat).ifBlank { null },
                kodeInstansi = InputRules.filterAgencyCode(kode).ifBlank { null }
            )
            val result = createInstansiUseCase(newInstansi, userId)
            if (result is ResultState.Error) _errorMessage.emit(result.message)
        }
    }
    fun updateInstansi(id: Int, nama: String, kode: String, alamat: String) {
        viewModelScope.launch {
            val updatedInstansi = Instansi(
                id = id, // Menggunakan ID yang sudah ada supaya data lamanya tertimpa
                namaInstansi = nama,
                alamat = alamat.ifBlank { null },
                kodeInstansi = kode.ifBlank { null }
            )
            instansiRepo.update(updatedInstansi)
        }
    }
    // Fungsi Delete Instansi
    fun deleteInstansi(id: Int) {
        viewModelScope.launch {
            val result = instansiRepo.delete(id)
            // Cek apakah hasilnya Error (misal karena dipakai di Peminjaman)
            if (result is com.bpkpad.peminjaman.core.common.ResultState.Error) {
                _errorMessage.emit("Gagal: Instansi ini tidak bisa dihapus karena sudah dipakai dalam riwayat peminjaman.")
            }
        }
    }
}
