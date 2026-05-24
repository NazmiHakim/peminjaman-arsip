package com.bpkpad.peminjaman.peminjaman.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.repository.*
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailTransaksiUiState(
    val transaksi: Transaksi? = null,
    val auditLogs: List<AuditLog> = emptyList(),
    val session: SessionObject? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DetailTransaksiViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val instansiRepo: InstansiRepository,
    private val auditLogRepo: AuditLogRepository,
    private val sessionManager: SessionManager,
    private val confirmHandoverUseCase: ConfirmHandoverUseCase,
    private val returnTransaksiUseCase: ReturnTransaksiUseCase,
    private val acknowledgeBypassUseCase: AcknowledgeBypassUseCase,
    private val cancelTransaksiUseCase: CancelTransaksiUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailTransaksiUiState())
    val uiState: StateFlow<DetailTransaksiUiState> = _uiState.asStateFlow()
    private var transaksiId = 0

    fun load(id: Int) {
        transaksiId = id
        viewModelScope.launch { sessionManager.session.firstOrNull()?.let { s -> _uiState.update { it.copy(session = s) } } }
        viewModelScope.launch {
            try {
                val t = transaksiRepo.getById(id)
                if (t == null) { _uiState.update { it.copy(isLoading = false, error = "Transaksi tidak ditemukan") }; return@launch }
                val nama = instansiRepo.getById(t.instansiId)?.namaInstansi ?: "Instansi #${t.instansiId}"
                _uiState.update { it.copy(transaksi = t.copy(namaInstansi = nama), isLoading = false) }
            } catch (e: Exception) { _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
        viewModelScope.launch { auditLogRepo.getByTransaksiId(id).collect { logs -> _uiState.update { it.copy(auditLogs = logs) } } }
    }

    fun confirmHandover() = action {
        val r = confirmHandoverUseCase(transaksiId, it)
        if (r is ResultState.Success) { reload(); "Dokumen berhasil diserahkan!" } else (r as ResultState.Error).message
    }

    fun returnTransaksi(kondisiMap: Map<Int, Pair<String, String?>>) = action {
        val r = returnTransaksiUseCase(transaksiId, it, kondisiMap)
        if (r is ResultState.Success) { reload(); "Peminjaman berhasil diselesaikan!" } else (r as ResultState.Error).message
    }

    fun acknowledgeBypass() = action {
        val r = acknowledgeBypassUseCase(transaksiId, it)
        if (r is ResultState.Success) { reload(); "Bypass berhasil diverifikasi!" } else (r as ResultState.Error).message
    }

    fun cancelTransaksi() = action {
        val r = cancelTransaksiUseCase(transaksiId, it)
        if (r is ResultState.Success) { reload(); "Transaksi dibatalkan" } else (r as ResultState.Error).message
    }

    private fun action(block: suspend (userId: Int) -> String?) {
        viewModelScope.launch {
            val userId = _uiState.value.session?.userId ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            val msg = block(userId)
            if (msg != null && msg.startsWith("Gagal").not() && msg.startsWith("Error").not()) {
                _uiState.update { it.copy(isLoading = false, successMessage = msg) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    private suspend fun reload() {
        val t = transaksiRepo.getById(transaksiId) ?: return
        val nama = instansiRepo.getById(t.instansiId)?.namaInstansi ?: ""
        _uiState.update { it.copy(transaksi = t.copy(namaInstansi = nama), isLoading = false) }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}
