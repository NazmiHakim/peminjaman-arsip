package com.bpkpad.peminjaman.master.presentation

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
        topBar = { BpkpadTopBar("Master Dokumen", onBack = onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery, onValueChange = viewModel::onSearch,
                label = { Text("Cari nomor / perihal...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Text("${filtered.size} dokumen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
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
    Card(shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(Modifier.size(40.dp), color = statusColor.copy(0.15f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, null, Modifier.size(22.dp), tint = statusColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dok.nomorDokumen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(dok.perihal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                dok.lokasiRak?.let { Text("Rak: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
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
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.filtered

    Scaffold(topBar = { BpkpadTopBar("Master Instansi", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery, onValueChange = viewModel::onSearch,
                label = { Text("Cari nama / kode instansi...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Text("${filtered.size} instansi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
            if (uiState.isLoading) BpkpadLoadingIndicator()
            else if (filtered.isEmpty()) BpkpadEmptyState("Tidak ada instansi ditemukan", Icons.Default.Business)
            else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { inst -> InstansiCard(inst) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun InstansiCard(inst: Instansi) {
    Card(shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), color = BpkpadBlue.copy(0.12f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, null, Modifier.size(22.dp), tint = BpkpadBlue)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(inst.namaInstansi, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                inst.kodeInstansi?.let { Text("Kode: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                inst.alamat?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListInstansi_Preview() { BpkpadTheme { ListInstansiScreen(onBack = {}) } }
