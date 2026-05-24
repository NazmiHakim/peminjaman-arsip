package com.bpkpad.peminjaman.peminjaman.presentation.pengembalian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.FindTransaksiByQrTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanQrUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundTransaksiId: Int? = null
)

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val findTransaksiByQrTokenUseCase: FindTransaksiByQrTokenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanQrUiState())
    val uiState: StateFlow<ScanQrUiState> = _uiState.asStateFlow()

    fun onQrDetected(raw: String) { findByToken(raw) }

    fun findByToken(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = findTransaksiByQrTokenUseCase(token.trim())) {
                is ResultState.Success -> _uiState.update { it.copy(isLoading = false, foundTransaksiId = r.data.id) }
                is ResultState.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }

    fun clearFound() = _uiState.update { it.copy(foundTransaksiId = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
