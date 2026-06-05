package com.bpkpad.peminjaman.peminjaman.presentation.form

import android.app.Activity
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.presentation.form.components.FotoSuratInputOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) { snackbarHostState.showSnackbar("Transaksi berhasil dibuat!"); viewModel.clearSuccess(); onBack() }
    }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Error: $it"); viewModel.clearError() } }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onFotoSelected(it) }
    }
    val documentScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = result.data?.let { GmsDocumentScanningResult.fromActivityResultIntent(it) }
            val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
            if (imageUri != null) {
                viewModel.onFotoSelected(imageUri)
            } else {
                scope.launch { snackbarHostState.showSnackbar("Scanner tidak mengembalikan gambar dokumen") }
            }
        }
    }
    val documentScanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
                .build()
        )
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
        onScanFoto = {
            val activity = context.findActivity()
            if (activity == null) {
                scope.launch { snackbarHostState.showSnackbar("Tidak dapat membuka scanner dokumen") }
            } else {
                documentScanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        documentScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar(error.message ?: "Gagal membuka scanner dokumen")
                        }
                    }
            }
        },
        onPickGallery = { imagePickerLauncher.launch("image/*") },
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
    onScanFoto: () -> Unit,
    onPickGallery: () -> Unit,
    onDokumenSearch: (String) -> Unit,
    onDokumenAdd: (MasterDokumen) -> Unit,
    onDokumenRemove: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    var dokumenQuery by remember { mutableStateOf("") }
    var instansiQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Figma Header: Back arrow + "Ajukan Permohonan" ──
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color(0xFF374151),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Ajukan Permohonan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
            }

            // ── Foto Surat (Green button at top per Figma) ──
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Foto Surat Pengantar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                    Spacer(Modifier.height(8.dp))
                    FotoSuratInputOptions(
                        hasFoto = uiState.fotoUri != null,
                        isError = uiState.submitted && uiState.fotoUri == null,
                        onScanFoto = onScanFoto,
                        onPickGallery = onPickGallery
                    )
                }
            }

            // ── Section: Data Pemohon ──
            item {
                FormSectionCard(title = "Data Pemohon") {
                    // Nama Pemohon
                    FigmaFormField(
                        icon = Icons.Default.Person,
                        label = "Nama Pemohon"
                    ) {
                        BpkpadTextField(uiState.picNama, onPicNamaChange, "Nama PIC *",
                            error = if (uiState.submitted && uiState.picNama.isBlank()) "Wajib diisi" else null)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Unit Kerja / Instansi
                    FigmaFormField(
                        icon = Icons.Default.Business,
                        label = "Unit Kerja"
                    ) {
                        BpkpadTextField(
                            value = instansiQuery,
                            onValueChange = { instansiQuery = it; onInstansiSearch(it) },
                            label = "Instansi Peminjam *",
                            error = if (uiState.submitted && uiState.instansiId == null) "Pilih instansi" else null
                        )
                        if (uiState.instansiSuggestions.isNotEmpty() && instansiQuery.isNotBlank()) {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
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
                            Spacer(Modifier.height(4.dp))
                            Surface(color = Color(0xFFDFF5E1), shape = RoundedCornerShape(8.dp)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF207125), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(inst.namaInstansi, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF207125))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // No. Telepon
                    FigmaFormField(
                        icon = Icons.Default.Phone,
                        label = "No. Telepon"
                    ) {
                        BpkpadTextField(uiState.picNoHp, onPicHpChange, "No. HP PIC *",
                            error = if (uiState.submitted && uiState.picNoHp.isBlank()) "Wajib diisi (min 10 digit)" else null)
                    }
                }
            }

            // ── Section: Data Arsip ──
            item {
                FormSectionCard(title = "Data Arsip") {
                    // Nomor Surat
                    FigmaFormField(
                        icon = Icons.Default.Description,
                        label = "Nomor Surat"
                    ) {
                        BpkpadTextField(uiState.nomorSurat, onNomorSuratChange, "Nomor Surat Pengantar *",
                            error = if (uiState.submitted && uiState.nomorSurat.isBlank()) "Wajib diisi" else null)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Cari Dokumen
                    FigmaFormField(
                        icon = Icons.Default.Search,
                        label = "Cari Dokumen"
                    ) {
                        BpkpadTextField(
                            value = dokumenQuery,
                            onValueChange = { dokumenQuery = it; onDokumenSearch(it) },
                            label = "Cari nomor / perihal dokumen"
                        )
                        if (uiState.dokumenSearchResults.isNotEmpty()) {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
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
                }
            }

            // ── Selected Dokumen List ──
            items(uiState.selectedDokumen) { dok ->
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(Color(0xFFDFF5E1))
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dok.nomorDokumen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF207125))
                            Text(dok.perihal, style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
                        }
                        IconButton(onClick = { onDokumenRemove(dok.id) }) {
                            Icon(Icons.Default.Close, "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── Section: Jadwal Peminjaman ──
            item {
                FormSectionCard(title = "Jadwal Peminjaman") {
                    FigmaFormField(
                        icon = Icons.Default.CalendarMonth,
                        label = "Tanggal Kembali"
                    ) {
                        BpkpadDatePickerField(
                            value = uiState.tanggalKembali,
                            onDateSelected = onTanggalKembaliChange,
                            label = "Tanggal Kembali Rencana *",
                            error = if (uiState.submitted && uiState.tanggalKembali.isBlank()) "Wajib diisi" else null
                        )
                    }
                }
            }

            // ── Submit Button ──
            item {
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF207125),
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isLoading) "Menyimpan..." else "Ajukan Peminjaman",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Figma Section Card ──
@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                content()
            }
        }
    }
}

// ── Figma Form Field with icon + label ──
@Composable
private fun FigmaFormField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier
                .padding(top = 14.dp)
                .size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
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
            onInstansiSelect = {}, onInstansiSearch = {}, onScanFoto = {}, onPickGallery = {},
            onDokumenSearch = {}, onDokumenAdd = {}, onDokumenRemove = {}, onSubmit = {}
        )
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
