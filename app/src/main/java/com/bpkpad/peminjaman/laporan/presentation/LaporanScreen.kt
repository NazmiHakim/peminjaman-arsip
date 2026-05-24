package com.bpkpad.peminjaman.laporan.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*

/**
 * [LOCAL] LaporanScreen
 * Ownership: Laporan feature
 * RBAC: Both roles can export
 * v1.0 2026-05-24
 */
@Composable
fun LaporanScreen(
    onBack: () -> Unit,
    viewModel: LaporanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    LaporanContent(
        uiState = uiState, snackbarHostState = snackbarHostState, onBack = onBack,
        onExportPdf = viewModel::exportPdf,
        onExportExcel = viewModel::exportExcel,
        onFilterChange = viewModel::onFilterChange
    )
}

@Composable
fun LaporanContent(
    uiState: LaporanUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onFilterChange: (String, String) -> Unit
) {
    Scaffold(
        topBar = { BpkpadTopBar("Laporan & Ekspor", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Filter Periode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.dateFrom, onValueChange = { onFilterChange(it, uiState.dateTo) },
                        label = { Text("Dari (YYYY-MM-DD)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.dateTo, onValueChange = { onFilterChange(uiState.dateFrom, it) },
                        label = { Text("Sampai") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                }
            }

            // Stats summary
            item {
                Text("Ringkasan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Total", uiState.totalAll, Icons.Default.Assignment, BpkpadBlue, Modifier.weight(1f))
                    StatCard("Dipinjam", uiState.totalDipinjam, Icons.Default.BookmarkAdded, BpkpadGreen, Modifier.weight(1f))
                    StatCard("Overdue", uiState.totalOverdue, Icons.Default.Warning, BpkpadRed, Modifier.weight(1f))
                }
            }

            // Export buttons
            item {
                Text("Ekspor Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportCard(
                        title = "Ekspor PDF",
                        description = "Laporan peminjaman dalam format PDF siap cetak",
                        icon = Icons.Default.PictureAsPdf,
                        color = BpkpadRed,
                        isLoading = uiState.isExportingPdf,
                        onClick = onExportPdf
                    )
                    ExportCard(
                        title = "Ekspor Excel",
                        description = "Data transaksi dalam format spreadsheet (.xlsx)",
                        icon = Icons.Default.TableChart,
                        color = BpkpadGreen,
                        isLoading = uiState.isExportingExcel,
                        onClick = onExportExcel
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(color.copy(0.08f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ExportCard(title: String, description: String, icon: ImageVector, color: Color, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp),
        enabled = !isLoading
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), color = color.copy(0.15f), shape = RoundedCornerShape(12.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color, strokeWidth = 2.dp)
                    else Icon(icon, null, Modifier.size(26.dp), tint = color)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Icon(Icons.Default.Download, null, tint = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Laporan_Preview() {
    BpkpadTheme {
        LaporanContent(
            uiState = LaporanUiState(totalAll = 25, totalDipinjam = 8, totalOverdue = 2),
            onBack = {}, onExportPdf = {}, onExportExcel = {}, onFilterChange = { _, _ -> }
        )
    }
}
