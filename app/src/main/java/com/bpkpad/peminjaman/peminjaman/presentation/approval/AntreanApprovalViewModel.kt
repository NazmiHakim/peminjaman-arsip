package com.bpkpad.peminjaman.peminjaman.presentation.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan.ApprovePerpanjanganUseCase
import com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan.RejectPerpanjanganUseCase
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.ApproveTransaksiUseCase
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.RejectTransaksiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AntreanApprovalUiState(
    val pendingTransaksi: List<Transaksi> = emptyList(),
    val pendingPerpanjangan: List<Perpanjangan> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AntreanApprovalViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val perpanjanganRepo: PerpanjanganRepository,
    private val sessionManager: SessionManager,
    private val approveTransaksiUseCase: ApproveTransaksiUseCase,
    private val rejectTransaksiUseCase: RejectTransaksiUseCase,
    private val approvePerpanjanganUseCase: ApprovePerpanjanganUseCase,
    private val rejectPerpanjanganUseCase: RejectPerpanjanganUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AntreanApprovalUiState())
    val uiState: StateFlow<AntreanApprovalUiState> = _uiState.asStateFlow()
    private var userId = 0

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            sessionManager.session.firstOrNull()?.let { userId = it.userId }
        }
        viewModelScope.launch {
            combine(
                transaksiRepo.getByStatus(TransaksiStatus.MENUNGGU_PERSETUJUAN),
                perpanjanganRepo.getPendingAll()
            ) { pending, perps ->
                _uiState.update {
                    it.copy(
                        pendingTransaksi    = pending,
                        pendingPerpanjangan = perps,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun selectTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }

    fun approveTransaksi(transaksiId: Int, catatan: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = approveTransaksiUseCase(transaksiId, userId, catatan)) {
                is ResultState.Success -> _uiState.update { it.copy(isLoading = false, successMessage = "Disetujui! QR Code digenerate.") }
                is ResultState.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }

    fun rejectTransaksi(transaksiId: Int, alasan: String) {
        viewModelScope.launch {
            when (val r = rejectTransaksiUseCase(transaksiId, userId, alasan)) {
                is ResultState.Success -> _uiState.update { it.copy(successMessage = "Pengajuan ditolak") }
                is ResultState.Error   -> _uiState.update { it.copy(error = r.message) }
                else -> {}
            }
        }
    }

    fun approvePerpanjangan(perpanjanganId: Int, transaksiId: Int) {
        viewModelScope.launch {
            when (val r = approvePerpanjanganUseCase(perpanjanganId, userId, transaksiId)) {
                is ResultState.Success -> _uiState.update { it.copy(successMessage = "Perpanjangan disetujui!") }
                is ResultState.Error   -> _uiState.update { it.copy(error = r.message) }
                else -> {}
            }
        }
    }

    fun rejectPerpanjangan(perpanjanganId: Int, transaksiId: Int, alasan: String) {
        viewModelScope.launch {
            when (val r = rejectPerpanjanganUseCase(perpanjanganId, userId, transaksiId, alasan)) {
                is ResultState.Success -> _uiState.update { it.copy(successMessage = "Perpanjangan ditolak") }
                is ResultState.Error   -> _uiState.update { it.copy(error = r.message) }
                else -> {}
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}