package com.bpkpad.peminjaman.peminjaman.presentation.form

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen

/**
 * [LOCAL] FormTransaksiScreen
 * Ownership: Form feature (Arsiparis only)
 * RBAC: ARSIPARIS
 * v1.0 2026-05-24
 */
@Composable
fun FormTransaksiScreen(
    onBack: () -> Unit,
    viewModel: FormTransaksiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) { snackbarHostState.showSnackbar("Transaksi berhasil dibuat!"); viewModel.clearSuccess(); onBack() }
    }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Error: $it"); viewModel.clearError() } }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onFotoSelected(it) }
    }

    FormTransaksiContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPicNamaChange = viewModel::onPicNamaChange,
        onPicHpChange = viewModel::onPicHpChange,
        onNomorSuratChange = viewModel::onNomorSuratChange,
        onTanggalKembaliChange = viewModel::onTanggalKembaliChange,
        onInstansiSelect = viewModel::onInstansiSelect,
        onInstansiSearch = viewModel::onInstansiSearch,
        onPickFoto = { imagePickerLauncher.launch("image/*") },
        onDokumenSearch = viewModel::searchDokumen,
        onDokumenAdd = viewModel::addDokumen,
        onDokumenRemove = viewModel::removeDokumen,
        onSubmit = viewModel::submit
    )
}

@Composable
fun FormTransaksiContent(
    uiState: FormTransaksiUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onPicNamaChange: (String) -> Unit,
    onPicHpChange: (String) -> Unit,
    onNomorSuratChange: (String) -> Unit,
    onTanggalKembaliChange: (String) -> Unit,
    onInstansiSelect: (Instansi) -> Unit,
    onInstansiSearch: (String) -> Unit,
    onPickFoto: () -> Unit,
    onDokumenSearch: (String) -> Unit,
    onDokumenAdd: (MasterDokumen) -> Unit,
    onDokumenRemove: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    var dokumenQuery by remember { mutableStateOf("") }
    var instansiQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BpkpadTopBar("Buat Peminjaman Baru", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Informasi Peminjam", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }

            // Instansi autocomplete
            item {
                BpkpadTextField(
                    value = instansiQuery,
                    onValueChange = { instansiQuery = it; onInstansiSearch(it) },
                    label = "Instansi Peminjam *",
                    error = if (uiState.submitted && uiState.instansiId == null) "Pilih instansi" else null,
                    trailingIcon = { Icon(Icons.Default.Business, null) }
                )
                if (uiState.instansiSuggestions.isNotEmpty() && instansiQuery.isNotBlank()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Column {
                            uiState.instansiSuggestions.take(5).forEach { inst ->
                                TextButton(
                                    onClick = { onInstansiSelect(inst); instansiQuery = inst.namaInstansi },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                        Column {
                                            Text(inst.namaInstansi, style = MaterialTheme.typography.bodyMedium)
                                            inst.kodeInstansi?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                uiState.selectedInstansi?.let { inst ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(inst.namaInstansi, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                BpkpadTextField(uiState.picNama, onPicNamaChange, "Nama PIC *",
                    error = if (uiState.submitted && uiState.picNama.isBlank()) "Wajib diisi" else null)
            }
            item {
                BpkpadTextField(uiState.picNoHp, onPicHpChange, "No. HP PIC *",
                    error = if (uiState.submitted && uiState.picNoHp.isBlank()) "Wajib diisi (min 10 digit)" else null)
            }
            item {
                BpkpadTextField(uiState.nomorSurat, onNomorSuratChange, "Nomor Surat Pengantar *",
                    error = if (uiState.submitted && uiState.nomorSurat.isBlank()) "Wajib diisi" else null)
            }
            item {
                BpkpadTextField(
                    value = uiState.tanggalKembali,
                    onValueChange = onTanggalKembaliChange,
                    label = "Tanggal Kembali Rencana (YYYY-MM-DD) *",
                    error = if (uiState.submitted && uiState.tanggalKembali.isBlank()) "Wajib diisi" else null,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                )
            }

            // Foto
            item {
                Text("Foto Surat Pengantar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedCard(onClick = onPickFoto, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (uiState.fotoUri != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Foto dipilih", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(8.dp))
                                Text("Ketuk untuk pilih foto *", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                if (uiState.submitted && uiState.fotoUri == null) {
                    Text("Foto surat wajib dilampirkan", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Dokumen
            item { Text("Dokumen yang Dipinjam", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            item {
                BpkpadTextField(
                    value = dokumenQuery,
                    onValueChange = { dokumenQuery = it; onDokumenSearch(it) },
                    label = "Cari nomor / perihal dokumen",
                    trailingIcon = { Icon(Icons.Default.Search, null) }
                )
                if (uiState.dokumenSearchResults.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Column {
                            uiState.dokumenSearchResults.take(5).forEach { dok ->
                                TextButton(onClick = { onDokumenAdd(dok); dokumenQuery = "" }, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                        Column {
                                            Text(dok.nomorDokumen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(dok.perihal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                if (uiState.submitted && uiState.selectedDokumen.isEmpty()) {
                    Text("Minimal 1 dokumen harus dipilih", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            items(uiState.selectedDokumen) { dok ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dok.nomorDokumen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(dok.perihal, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDokumenRemove(dok.id) }) {
                            Icon(Icons.Default.Close, "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                BpkpadPrimaryButton(
                    text = if (uiState.isLoading) "Menyimpan..." else "Buat Peminjaman",
                    enabled = !uiState.isLoading,
                    onClick = onSubmit
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormTransaksi_Preview() {
    BpkpadTheme {
        FormTransaksiContent(
            uiState = FormTransaksiUiState(), onBack = {},
            onPicNamaChange = {}, onPicHpChange = {}, onNomorSuratChange = {}, onTanggalKembaliChange = {},
            onInstansiSelect = {}, onInstansiSearch = {}, onPickFoto = {},
            onDokumenSearch = {}, onDokumenAdd = {}, onDokumenRemove = {}, onSubmit = {}
        )
    }
}
