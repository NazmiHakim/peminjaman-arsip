package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.auth.domain.usecase.LogoutUseCase
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.PerpanjanganRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardKasubagUiState(
    val session: SessionObject? = null,
    val pendingTransaksi: List<Transaksi> = emptyList(),
    val bypassPendingList: List<Transaksi> = emptyList(),
    val totalSemua: Int = 0,
    val totalMenunggu: Int = 0,
    val totalDisetujui: Int = 0,
    val totalDipinjam: Int = 0,
    val totalDikembalikan: Int = 0,
    val totalOverdue: Int = 0,
    val perpanjanganPending: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardKasubagViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val perpanjanganRepo: PerpanjanganRepository,
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardKasubagUiState())
    val uiState: StateFlow<DashboardKasubagUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch { transaksiRepo.syncPending() }
        viewModelScope.launch {
            sessionManager.session.collect { s -> _uiState.update { it.copy(session = s) } }
        }
        viewModelScope.launch {
            combine(
                transaksiRepo.getAll(),
                transaksiRepo.getOverdue(),
                perpanjanganRepo.getPendingAll()
            ) { all, overdue, perps ->
                val pending = all.filter { it.status == TransaksiStatus.MENUNGGU_PERSETUJUAN }
                val bypass  = all.filter { it.needsBypassAcknowledge }

                _uiState.update {
                    it.copy(
                        pendingTransaksi  = pending,
                        bypassPendingList = bypass,
                        totalSemua         = all.size,
                        totalMenunggu     = pending.size,
                        totalDisetujui    = all.count { t -> t.status == TransaksiStatus.DISETUJUI },
                        totalDipinjam     = all.count { t -> t.status == TransaksiStatus.DIPINJAM },
                        totalDikembalikan = all.count { t -> t.status == TransaksiStatus.DIKEMBALIKAN },
                        totalOverdue      = overdue.size,
                        perpanjanganPending = perps.size,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun logout() { viewModelScope.launch { logoutUseCase() } }
}
