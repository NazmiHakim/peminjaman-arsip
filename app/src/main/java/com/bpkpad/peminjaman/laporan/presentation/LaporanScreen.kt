package com.bpkpad.peminjaman.laporan.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadBlue
import com.bpkpad.peminjaman.core.theme.BpkpadGreen
import com.bpkpad.peminjaman.core.theme.BpkpadGold
import com.bpkpad.peminjaman.core.theme.BpkpadRed
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.BpkpadDatePickerField
import com.bpkpad.peminjaman.core.ui.BpkpadTopBar
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import java.io.File

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
        topBar = { BpkpadTopBar("Laporan & Ekspor", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, null, tint = MaterialTheme.colorScheme.primary)
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

            item {
                Text("Filter Periode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
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

            item {
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

            uiState.exportedReport?.let { report ->
                item {
                    ExportedReportCard(report = report, onShare = { onShareReport(report) })
                }
            }

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

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(color.copy(0.08f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

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
        enabled = enabled && !isLoading
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

/**
 * [LOCAL] ExportedReportCard
 *
 * Ownership: Laporan feature
 * Scope: Laporan export result
 * Theme: BpkpadTheme compliant
 * RBAC: Neutral
 *
 * Changelog:
 * - v1.0 (2026-05-28): Initial report result card.
 *
 * Dependencies:
 * - androidx.compose.material3.OutlinedButton
 */
@Composable
private fun ExportedReportCard(report: ExportedReport, onShare: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
