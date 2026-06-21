package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.auth.domain.usecase.LogoutUseCase
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardArsiparisUiState(
    val session: SessionObject? = null,
    val overdueList: List<Transaksi> = emptyList(),
    val recentList: List<Transaksi> = emptyList(),
    val totalSemua: Int = 0,
    val totalDisetujui: Int = 0,
    val totalDipinjam: Int = 0,
    val totalMenunggu: Int = 0,
    val totalOverdue: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardArsiparisViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardArsiparisUiState())
    val uiState: StateFlow<DashboardArsiparisUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch { transaksiRepo.syncPending() }
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            combine(
                transaksiRepo.getOverdue(),
                transaksiRepo.getAll()
            ) { overdue, all ->
                _uiState.update {
                    it.copy(
                        overdueList = overdue,
                        recentList  = all.take(10),
                        totalSemua = all.size,
                        totalDisetujui = all.count { t -> t.status == TransaksiStatus.DISETUJUI },
                        totalDipinjam = all.count { t -> t.status == TransaksiStatus.DIPINJAM },
                        totalMenunggu = all.count { t -> t.status == TransaksiStatus.MENUNGGU_PERSETUJUAN },
                        totalOverdue  = overdue.size,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun logout() { viewModelScope.launch { logoutUseCase() } }
}
