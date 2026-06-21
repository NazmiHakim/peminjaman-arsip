package com.bpkpad.peminjaman.peminjaman.presentation.form

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.storage.FileRepository
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.MetodePersetujuan
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi.CreateTransaksiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class FormTransaksiUiState(
    val instansiName: String = "",
    val instansiSuggestions: List<Instansi> = emptyList(),
    val picNama: String = "",
    val picNoHp: String = "",
    val nomorSurat: String = "",
    val tanggalKembali: String = "",
    val fotoUri: Uri? = null,
    val dokumenSearchResults: List<MasterDokumen> = emptyList(),
    val selectedDokumen: List<MasterDokumen> = emptyList(),
    val isLoading: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false,
    val isBypass: Boolean = false,
    val fotoBypassUri: Uri? = null,
    val catatanBypass: String = ""
)

@OptIn(FlowPreview::class)
@HiltViewModel
class FormTransaksiViewModel @Inject constructor(
    private val instansiRepo: InstansiRepository,
    private val masterDokumenRepo: MasterDokumenRepository,
    private val fileRepo: FileRepository,
    private val sessionManager: SessionManager,
    private val createTransaksiUseCase: CreateTransaksiUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormTransaksiUiState())
    val uiState: StateFlow<FormTransaksiUiState> = _uiState.asStateFlow()

    private val _instansiQuery = MutableStateFlow("")
    private val _dokumenQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _instansiQuery.debounce(300).collect { q ->
                if (q.isBlank()) {
                    _uiState.update { it.copy(instansiSuggestions = emptyList()) }
                } else {
                    instansiRepo.search(q).firstOrNull()?.let { list ->
                        _uiState.update { it.copy(instansiSuggestions = list) }
                    }
                }
            }
        }
        viewModelScope.launch {
            _dokumenQuery.debounce(300).collect { q ->
                if (q.isBlank()) {
                    _uiState.update { it.copy(dokumenSearchResults = emptyList()) }
                } else {
                    masterDokumenRepo.search(q).firstOrNull()?.let { list ->
                        val selected = _uiState.value.selectedDokumen.map { it.id }.toSet()
                        _uiState.update { it.copy(dokumenSearchResults = list.filter { d -> d.id !in selected && d.status.name == "TERSEDIA" }) }
                    }
                }
            }
        }
    }

    fun onPicNamaChange(v: String) =
        _uiState.update { it.copy(picNama = InputRules.filterApplicantName(v)) }

    fun onPicHpChange(v: String) =
        _uiState.update { it.copy(picNoHp = InputRules.filterPhone(v)) }

    fun onNomorSuratChange(v: String) =
        _uiState.update { it.copy(nomorSurat = InputRules.filterLetterNumber(v)) }
    fun onTanggalKembaliChange(v: String) = _uiState.update { it.copy(tanggalKembali = v) }
    fun onFotoSelected(uri: Uri) = _uiState.update { it.copy(fotoUri = uri) }
    fun onBypassToggle(v: Boolean) = _uiState.update { it.copy(isBypass = v) }
    fun onFotoBypassSelected(uri: Uri) = _uiState.update { it.copy(fotoBypassUri = uri) }
    fun onCatatanBypassChange(v: String) =
        _uiState.update { it.copy(catatanBypass = InputRules.filterBypassNote(v)) }

    fun onInstansiSearch(q: String) {
        val filtered = InputRules.filterWorkUnit(q)
        _uiState.update { it.copy(instansiName = filtered) }
        _instansiQuery.value = filtered
    }
    fun onInstansiSelect(inst: Instansi) {
        // Saat diklik dari autocomplete, masukkan namanya ke kolom teks
        _uiState.update { it.copy(instansiName = inst.namaInstansi, instansiSuggestions = emptyList()) }
    }
    fun searchDokumen(q: String) {
        _dokumenQuery.value = InputRules.filterDocumentSearch(q)
    }
    fun addDokumen(dok: MasterDokumen) = _uiState.update { it.copy(selectedDokumen = it.selectedDokumen + dok, dokumenSearchResults = emptyList()) }
    fun removeDokumen(id: Int) = _uiState.update { it.copy(selectedDokumen = it.selectedDokumen.filter { d -> d.id != id }) }
    fun clearSuccess() = _uiState.update { it.copy(submitSuccess = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun submit() {
        val s = _uiState.value
        _uiState.update { it.copy(submitted = true) }
        if (
            InputRules.validateWorkUnit(s.instansiName) != null ||
            InputRules.validateApplicantName(s.picNama) != null ||
            InputRules.validatePhone(s.picNoHp) != null ||
            InputRules.validateLetterNumber(s.nomorSurat) != null ||
            s.tanggalKembali.isBlank() ||
            s.fotoUri == null ||
            s.selectedDokumen.isEmpty()
        ) return

        if (
            s.isBypass &&
            (s.fotoBypassUri == null || InputRules.validateBypassNote(s.catatanBypass) != null)
        ) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = sessionManager.session.firstOrNull()?.userId ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Sesi tidak valid") }
                return@launch
            }

            // Upload foto surat pengantar
            val fotoResult = fileRepo.uploadImage(s.fotoUri, "surat/${UUID.randomUUID()}.jpg")
            if (fotoResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false, error = fotoResult.message) }
                return@launch
            }
            val fotoPath = (fotoResult as ResultState.Success).data

            // Upload foto bukti bypass jika bypass aktif
            var buktiBypassPath: String? = null
            if (s.isBypass && s.fotoBypassUri != null) {
                val buktiResult = fileRepo.uploadImage(s.fotoBypassUri, "bypass/${UUID.randomUUID()}.jpg")
                if (buktiResult is ResultState.Error) {
                    _uiState.update { it.copy(isLoading = false, error = buktiResult.message) }
                    return@launch
                }
                buktiBypassPath = (buktiResult as ResultState.Success).data
            }

            val tanggalKembali = try { LocalDate.parse(s.tanggalKembali) } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Format tanggal tidak valid (YYYY-MM-DD)") }
                return@launch
            }

            val status = if (s.isBypass) TransaksiStatus.DISETUJUI else TransaksiStatus.MENUNGGU_PERSETUJUAN
            val metodePersetujuan = if (s.isBypass) MetodePersetujuan.BYPASS else null
            val catatanBypassStr = if (s.isBypass) s.catatanBypass else null

            val transaksi = Transaksi(
                id = 0, namaInstansi = s.instansiName,
                picNama = s.picNama, picNoHp = s.picNoHp, nomorSuratPengantar = s.nomorSurat,
                fotoSuratPengantarPath = fotoPath, qrCodeToken = null,
                tanggalPinjam = LocalDate.now(), tanggalKembaliRencana = tanggalKembali,
                tanggalKembaliAktual = null, status = status,
                metodePersetujuan = metodePersetujuan, buktiBypassPath = buktiBypassPath, catatanBypass = catatanBypassStr,
                isBypassAcknowledged = false, alasanPenolakan = null,
                createdBy = userId, namaCreatedBy = "", approvedBy = if (s.isBypass) userId else null, namaApprovedBy = null,
                createdAt = System.currentTimeMillis(), details = emptyList()
            )

            when (val r = createTransaksiUseCase(transaksi, s.selectedDokumen.map { it.id })) {
                is ResultState.Success -> _uiState.update { it.copy(isLoading = false, submitSuccess = true) }
                is ResultState.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }
}
