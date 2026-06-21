package com.bpkpad.peminjaman.peminjaman.presentation.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.core.storage.FileRepository
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.PerpanjanganStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.*
import com.bpkpad.peminjaman.peminjaman.domain.usecase.perpanjangan.CreatePerpanjanganUseCase
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DetailTransaksiUiState(
    val transaksi: Transaksi? = null,
    val auditLogs: List<AuditLog> = emptyList(),
    val session: SessionObject? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val bypassProofUri: Uri? = null,
    val bypassNote: String = "",
    val perpanjanganList: List<Perpanjangan> = emptyList(),
    val extensionDate: String = "",
    val extensionReason: String = "",
    val extensionLetterUri: Uri? = null
)

@HiltViewModel
class DetailTransaksiViewModel @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val instansiRepo: InstansiRepository,
    private val auditLogRepo: AuditLogRepository,
    private val perpanjanganRepo: PerpanjanganRepository,
    private val sessionManager: SessionManager,
    private val confirmHandoverUseCase: ConfirmHandoverUseCase,
    private val returnTransaksiUseCase: ReturnTransaksiUseCase,
    private val bypassTransaksiUseCase: BypassTransaksiUseCase,
    private val createPerpanjanganUseCase: CreatePerpanjanganUseCase,
    private val acknowledgeBypassUseCase: AcknowledgeBypassUseCase,
    private val cancelTransaksiUseCase: CancelTransaksiUseCase,
    private val fileRepository: FileRepository
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
                _uiState.update { it.copy(transaksi = t, isLoading = false) }
            } catch (e: Exception) { _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
        viewModelScope.launch { auditLogRepo.getByTransaksiId(id).collect { logs -> _uiState.update { it.copy(auditLogs = logs) } } }
        viewModelScope.launch {
            perpanjanganRepo.getByTransaksiId(id).collect { extensions ->
                _uiState.update { it.copy(perpanjanganList = extensions) }
            }
        }
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

    fun onBypassProofSelected(uri: Uri) {
        _uiState.update { it.copy(bypassProofUri = uri, error = null) }
    }

    fun onBypassNoteChange(value: String) {
        _uiState.update {
            it.copy(
                bypassNote = InputRules.filterBypassNote(value),
                error = null
            )
        }
    }

    fun bypassPendingTransaksi() {
        viewModelScope.launch {
            val state = _uiState.value
            val actorId = state.session?.userId ?: run {
                _uiState.update { it.copy(error = "Sesi tidak valid") }
                return@launch
            }
            val proofUri = state.bypassProofUri ?: run {
                _uiState.update { it.copy(error = "Foto bukti bypass wajib dilampirkan") }
                return@launch
            }
            InputRules.validateBypassNote(state.bypassNote)?.let { validationError ->
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val uploadResult = fileRepository.uploadImage(
                proofUri,
                "bypass/${UUID.randomUUID()}.jpg"
            )
            if (uploadResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, error = uploadResult.message) }
                return@launch
            }

            val proofPath = (uploadResult as ResultState.Success).data
            when (
                val result = bypassTransaksiUseCase(
                    transaksiId = transaksiId,
                    arsiparisId = actorId,
                    buktiPath = proofPath,
                    catatan = state.bypassNote
                )
            ) {
                is ResultState.Success -> {
                    reload()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Transaksi berhasil di-bypass dan menunggu verifikasi Kasubag",
                            bypassProofUri = null,
                            bypassNote = ""
                        )
                    }
                }
                is ResultState.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun onExtensionDateChange(value: String) {
        _uiState.update { it.copy(extensionDate = value, error = null) }
    }

    fun onExtensionReasonChange(value: String) {
        _uiState.update {
            it.copy(
                extensionReason = InputRules.filterExtensionReason(value),
                error = null
            )
        }
    }

    fun onExtensionLetterSelected(uri: Uri) {
        _uiState.update { it.copy(extensionLetterUri = uri, error = null) }
    }

    fun createExtension() {
        viewModelScope.launch {
            val state = _uiState.value
            val transaction = state.transaksi ?: return@launch
            val actorId = state.session?.userId ?: run {
                _uiState.update { it.copy(error = "Sesi tidak valid") }
                return@launch
            }
            if (state.perpanjanganList.any { it.status == PerpanjanganStatus.PENDING }) {
                _uiState.update {
                    it.copy(error = "Masih ada pengajuan perpanjangan yang menunggu persetujuan")
                }
                return@launch
            }
            val newDate = runCatching { java.time.LocalDate.parse(state.extensionDate) }
                .getOrElse {
                    _uiState.update { it.copy(error = "Pilih tanggal kembali baru") }
                    return@launch
                }
            InputRules.validateExtensionReason(state.extensionReason)?.let { validationError ->
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }
            val letterUri = state.extensionLetterUri ?: run {
                _uiState.update { it.copy(error = "Surat perpanjangan wajib dilampirkan") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val uploadResult = fileRepository.uploadImage(
                letterUri,
                "perpanjangan/${UUID.randomUUID()}.jpg"
            )
            if (uploadResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, error = uploadResult.message) }
                return@launch
            }

            val extension = Perpanjangan(
                id = 0,
                transaksiId = transaction.id,
                tanggalKembaliLama = transaction.tanggalKembaliRencana,
                tanggalKembaliBaru = newDate,
                fotoSuratPerpanjanganPath = (uploadResult as ResultState.Success).data,
                alasan = state.extensionReason.trim(),
                status = PerpanjanganStatus.PENDING,
                alasanPenolakan = null,
                createdBy = actorId,
                approvedBy = null,
                createdAt = System.currentTimeMillis()
            )
            when (val result = createPerpanjanganUseCase(extension)) {
                is ResultState.Success ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Perpanjangan berhasil diajukan",
                            extensionDate = "",
                            extensionReason = "",
                            extensionLetterUri = null
                        )
                    }
                is ResultState.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
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
        _uiState.update { it.copy(transaksi = t, isLoading = false) }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}
