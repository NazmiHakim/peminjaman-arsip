package com.bpkpad.peminjaman.master.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.common.InputRules
import androidx.room.Delete
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus

/**
 * [LOCAL] ListDokumenScreen
 * Ownership: Master feature
 * RBAC: Both roles (view only for Kasubag)
 * v1.0 2026-05-24
 */
@Composable
fun ListDokumenScreen(
    onBack: () -> Unit,
    viewModel: ListDokumenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.filtered

    Scaffold(
        containerColor = Color(0xFFF7F8FA)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Figma-style Header ──
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
                        Icon(Icons.Default.ArrowBack, "Kembali", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Master Dokumen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }

            OutlinedTextField(
                value = uiState.searchQuery, onValueChange = viewModel::onSearch,
                placeholder = { Text("Cari nomor / perihal...", color = Color(0xFF9CA3AF)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = Color(0xFF207125)
                )
            )
            Text("${filtered.size} dokumen", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280), modifier = Modifier.padding(horizontal = 16.dp))
            if (uiState.isLoading) BpkpadLoadingIndicator()
            else if (filtered.isEmpty()) BpkpadEmptyState("Tidak ada dokumen ditemukan", Icons.Default.FolderOff)
            else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { dok -> DokumenMasterCard(dok) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DokumenMasterCard(dok: MasterDokumen) {
    val statusColor = when (dok.status) {
        DokumenStatus.TERSEDIA -> BpkpadGreen
        DokumenStatus.DIPINJAM -> BpkpadGold
        DokumenStatus.RUSAK    -> BpkpadOrange
        DokumenStatus.HILANG   -> BpkpadRed
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(Modifier.size(40.dp), color = statusColor.copy(0.15f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, null, Modifier.size(22.dp), tint = statusColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dok.nomorDokumen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(dok.perihal, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                dok.lokasiRak?.let { Text("Rak: $it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF)) }
            }
            Surface(color = statusColor.copy(0.12f), shape = RoundedCornerShape(6.dp)) {
                Text(dok.status.name, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListDokumen_Preview() { BpkpadTheme { ListDokumenScreen(onBack = {}) } }

// ================================================================

/**
 * [LOCAL] ListInstansiScreen
 * Ownership: Master feature (Arsiparis editable)
 * v1.0 2026-05-24
 */
@Composable
fun ListInstansiScreen(
    onBack: () -> Unit,
    viewModel: ListInstansiViewModel = hiltViewModel()
) {
    // UI sekarang HANYA membaca dari 1 sumber ini
    val uiState by viewModel.uiState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var inputNama by remember { mutableStateOf("") }
    var inputKode by remember { mutableStateOf("") }
    var inputAlamat by remember { mutableStateOf("") }
    var formSubmitted by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF207125),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Instansi")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Header ──
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, "Kembali", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Master Instansi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }

            // ── Kolom Pencarian ──
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearch,
                placeholder = { Text("Cari nama / kode instansi...", color = Color(0xFF9CA3AF)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = Color(0xFF207125)
                )
            )
            Text("${uiState.instansiList.size} instansi", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280), modifier = Modifier.padding(horizontal = 16.dp))

            // ── Tampilkan Data ──
            if (uiState.isLoading) {
                BpkpadLoadingIndicator()
            } else if (uiState.instansiList.isEmpty()) {
                BpkpadEmptyState("Data tidak ditemukan", Icons.Default.Business)
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Masukkan instansiList langsung ke dalam items
                    items(uiState.instansiList) { inst ->
                        InstansiCard(
                            inst = inst,
                            onDelete = { viewModel.deleteInstansi(inst.id) } // Jalankan perintah hapus
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // ── Dialog Tambah Data ──
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Tambah Instansi Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputNama,
                            onValueChange = { inputNama = InputRules.filterAgencyName(it) },
                            label = { Text("Nama Instansi *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = formSubmitted && InputRules.validateAgencyName(inputNama) != null,
                            supportingText = {
                                val error = InputRules.validateAgencyName(inputNama)
                                    .takeIf { formSubmitted }
                                Text(error ?: "${inputNama.length}/${InputRules.AGENCY_NAME_MAX}")
                            }
                        )
                        OutlinedTextField(
                            value = inputKode,
                            onValueChange = { inputKode = InputRules.filterAgencyCode(it) },
                            label = { Text("Kode (Opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = formSubmitted && InputRules.validateAgencyCode(inputKode) != null,
                            supportingText = {
                                Text(
                                    InputRules.validateAgencyCode(inputKode)
                                        .takeIf { formSubmitted }
                                        ?: "${inputKode.length}/${InputRules.AGENCY_CODE_MAX}"
                                )
                            }
                        )
                        OutlinedTextField(
                            value = inputAlamat,
                            onValueChange = { inputAlamat = InputRules.filterAgencyAddress(it) },
                            label = { Text("Alamat (Opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            isError = formSubmitted && InputRules.validateAgencyAddress(inputAlamat) != null,
                            supportingText = {
                                Text(
                                    InputRules.validateAgencyAddress(inputAlamat)
                                        .takeIf { formSubmitted }
                                        ?: "${inputAlamat.length}/${InputRules.AGENCY_ADDRESS_MAX}"
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            formSubmitted = true
                            if (
                                InputRules.validateAgencyName(inputNama) == null &&
                                InputRules.validateAgencyCode(inputKode) == null &&
                                InputRules.validateAgencyAddress(inputAlamat) == null
                            ) {
                                viewModel.addInstansi(inputNama, inputKode, inputAlamat)
                                showDialog = false
                                inputNama = ""; inputKode = ""; inputAlamat = ""
                                formSubmitted = false
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Batal", color = Color(0xFF6B7280)) }
                }
            )
        }
    }
}

@Composable
private fun InstansiCard(inst: Instansi, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), color = Color(0xFF207125).copy(0.12f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, null, Modifier.size(22.dp), tint = Color(0xFF207125))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(inst.namaInstansi, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                inst.kodeInstansi?.let { Text("Kode: $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280)) }
                inst.alamat?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280)) }
            }
            // Tombol Hapus / Delete
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ListInstansi_Preview() { BpkpadTheme { ListInstansiScreen(onBack = {}) } }
