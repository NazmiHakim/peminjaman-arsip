package com.bpkpad.peminjaman.laporan.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadBlue
import com.bpkpad.peminjaman.core.theme.BpkpadGreen
import com.bpkpad.peminjaman.core.theme.BpkpadGold
import com.bpkpad.peminjaman.core.theme.BpkpadRed
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.BpkpadDatePickerField
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import java.io.File

/**
 * [LOCAL] LaporanScreen
 * Ownership: Laporan feature
 * RBAC: Both roles can export
 * v1.1 - Fixed Shadow Bleeding on Cards (Clean Pastel UI)
 */
@Composable
fun LaporanScreen(
    onBack: () -> Unit,
    viewModel: LaporanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.exportMessage, uiState.exportedReport) {
        uiState.exportMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (uiState.exportedReport != null) "Bagikan" else null
            )
            if (result == SnackbarResult.ActionPerformed) {
                uiState.exportedReport?.let { shareExportedReport(context, it) }
            }
            viewModel.clearMessage()
        }
    }

    LaporanContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExportPdf = viewModel::exportPdf,
        onExportExcel = viewModel::exportExcel,
        onFilterChange = viewModel::onFilterChange,
        onShareReport = { shareExportedReport(context, it) }
    )
}

@Composable
fun LaporanContent(
    uiState: LaporanUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onFilterChange: (String, String) -> Unit,
    onShareReport: (ExportedReport) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Figma-style Header ──
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
                            Icon(Icons.Default.ArrowBack, "Kembali", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Laporan & Ekspor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    }
                }
            }

            // ── Info Laporan Peminjaman ──
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White) // Warna mutlak untuk blokir shadow
                ) {
                    // Isi card diberi background hijau tipis
                    Column(Modifier.fillMaxWidth().background(Color(0xFFDFF5E1).copy(alpha = 0.5f)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, null, tint = Color(0xFF207125))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Laporan Peminjaman", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${uiState.totalFiltered} dari ${uiState.totalAll} transaksi masuk filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Gunakan filter periode sebelum ekspor PDF atau Excel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Filter Periode ──
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Filter Periode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BpkpadDatePickerField(
                                    value = uiState.dateFrom,
                                    onDateSelected = { onFilterChange(it, uiState.dateTo) },
                                    label = "Dari",
                                    placeholder = "Semua tanggal",
                                    modifier = Modifier.weight(1f),
                                    isError = uiState.filterError != null
                                )
                                BpkpadDatePickerField(
                                    value = uiState.dateTo,
                                    onDateSelected = { onFilterChange(uiState.dateFrom, it) },
                                    label = "Sampai",
                                    placeholder = "Semua tanggal",
                                    modifier = Modifier.weight(1f),
                                    isError = uiState.filterError != null
                                )
                            }
                            uiState.filterError?.let { error ->
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (uiState.dateFrom.isNotBlank() || uiState.dateTo.isNotBlank()) {
                                TextButton(onClick = { onFilterChange("", "") }) {
                                    Text("Reset periode")
                                }
                            }
                        }
                    }
                }
            }

            // ── Ringkasan ──
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Ringkasan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("Total Filter", uiState.totalFiltered, Icons.Default.Assignment, BpkpadBlue, Modifier.weight(1f))
                            StatCard("Menunggu", uiState.totalMenunggu, Icons.Default.HourglassEmpty, BpkpadGold, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("Dipinjam", uiState.totalDipinjam, Icons.Default.BookmarkAdded, BpkpadGreen, Modifier.weight(1f))
                            StatCard("Overdue", uiState.totalOverdue, Icons.Default.Warning, BpkpadRed, Modifier.weight(1f))
                        }
                    }
                }
            }

            uiState.exportedReport?.let { report ->
                item {
                    ExportedReportCard(report = report, onShare = { onShareReport(report) })
                }
            }

            // ── Ekspor Data ──
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Ekspor Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportCard(
                            title = "Ekspor PDF",
                            description = "Laporan peminjaman dalam format PDF siap cetak",
                            icon = Icons.Default.PictureAsPdf,
                            color = BpkpadRed,
                            isLoading = uiState.isExportingPdf,
                            enabled = uiState.filterError == null && uiState.totalFiltered > 0 && !uiState.isExportingExcel,
                            onClick = onExportPdf
                        )
                        ExportCard(
                            title = "Ekspor Excel",
                            description = "Data transaksi dalam format spreadsheet (.xlsx)",
                            icon = Icons.Default.TableChart,
                            color = BpkpadGreen,
                            isLoading = uiState.isExportingExcel,
                            enabled = uiState.filterError == null && uiState.totalFiltered > 0 && !uiState.isExportingPdf,
                            onClick = onExportExcel
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── KOMPONEN STAT CARD YANG SUDAH DIPERBAIKI ──
@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // Blokir shadow bocor
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f)) // Warna transparan diaplikasikan di isi konten
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

// ── KOMPONEN EXPORT CARD YANG SUDAH DIPERBAIKI ──
@Composable
private fun ExportCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), // Blokir shadow bocor
        enabled = enabled && !isLoading
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (enabled) color.copy(alpha = 0.05f) else Color(0xFFF3F4F6)) // Warna diterapkan di dalam
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(48.dp), color = color.copy(0.15f), shape = RoundedCornerShape(12.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color, strokeWidth = 2.dp)
                    else Icon(icon, null, Modifier.size(26.dp), tint = color)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    if (enabled) description else "Lengkapi filter valid dan pastikan ada data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Default.Download, null, tint = color)
        }
    }
}

// ── KOMPONEN HASIL EXPORT ──
@Composable
private fun ExportedReportCard(report: ExportedReport, onShare: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // Blokir shadow bocor
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(BpkpadGreen.copy(alpha = 0.05f)) // Warna hijau transparan
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(32.dp), tint = BpkpadGreen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("File siap dibagikan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${report.fileName} (${report.totalRows} transaksi)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            OutlinedButton(onClick = onShare, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bagikan")
            }
        }
    }
}

private fun shareExportedReport(context: Context, report: ExportedReport) {
    val file = File(report.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = report.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Laporan Peminjaman Dokumen")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan laporan"))
}

@Preview(showBackground = true)
@Composable
private fun Laporan_Preview() {
    BpkpadTheme {
        LaporanContent(
            uiState = LaporanUiState(
                totalAll = 25,
                totalFiltered = 20,
                totalMenunggu = 4,
                totalDipinjam = 8,
                totalOverdue = 2
            ),
            onBack = {},
            onExportPdf = {},
            onExportExcel = {},
            onFilterChange = { _, _ -> },
            onShareReport = {}
        )
    }
}