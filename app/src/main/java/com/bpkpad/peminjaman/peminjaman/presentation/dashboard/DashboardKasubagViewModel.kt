package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.auth.domain.usecase.LogoutUseCase
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
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
    val totalMenunggu: Int = 0,
    val totalDipinjam: Int = 0,
    val totalDikembalikan: Int = 0,
    val totalOverdue: Int = 0,
    val perpanjanganPending: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardKasubagViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val instansiRepo: InstansiRepository,
    private val perpanjanganRepo: PerpanjanganRepository,
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardKasubagUiState())
    val uiState: StateFlow<DashboardKasubagUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            sessionManager.session.collect { s -> _uiState.update { it.copy(session = s) } }
        }
        viewModelScope.launch {
            val instansiMap = mutableMapOf<Int, String>()
            instansiRepo.getAll().firstOrNull()?.forEach { i -> instansiMap[i.id] = i.namaInstansi }

            combine(
                transaksiRepo.getAll(),
                transaksiRepo.getOverdue(),
                perpanjanganRepo.getPendingAll()
            ) { all, overdue, perps ->
                val withName: (Transaksi) -> Transaksi = { t ->
                    t.copy(namaInstansi = instansiMap[t.instansiId] ?: "Instansi #${t.instansiId}")
                }
                val pending = all.filter { it.status == TransaksiStatus.MENUNGGU_PERSETUJUAN }.map(withName)
                val bypass  = all.filter { it.needsBypassAcknowledge }.map(withName)
                _uiState.update {
                    it.copy(
                        pendingTransaksi  = pending,
                        bypassPendingList = bypass,
                        totalMenunggu     = pending.size,
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
