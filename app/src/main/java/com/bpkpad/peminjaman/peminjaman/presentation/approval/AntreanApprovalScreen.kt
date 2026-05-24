package com.bpkpad.peminjaman.peminjaman.presentation.approval

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi

/**
 * [LOCAL] AntreanApprovalScreen
 * Ownership: Approval feature
 * RBAC: KASUBAG only
 * v1.0 2026-05-24
 */
@Composable
fun AntreanApprovalScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AntreanApprovalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    AntreanApprovalContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToDetail = onNavigateToDetail,
        onTabSelect = viewModel::selectTab,
        onApproveTransaksi = viewModel::approveTransaksi,
        onRejectTransaksi = viewModel::rejectTransaksi,
        onApprovePerpanjangan = viewModel::approvePerpanjangan,
        onRejectPerpanjangan = viewModel::rejectPerpanjangan,
        onClearMessages = viewModel::clearMessages
    )
}

@Composable
fun AntreanApprovalContent(
    uiState: AntreanApprovalUiState,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onTabSelect: (Int) -> Unit,
    onApproveTransaksi: (Int, String?) -> Unit,
    onRejectTransaksi: (Int, String) -> Unit,
    onApprovePerpanjangan: (Int, Int) -> Unit,
    onRejectPerpanjangan: (Int, Int, String) -> Unit,
    onClearMessages: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var approveDialogId by remember { mutableStateOf<Int?>(null) }
    var rejectDialogId by remember { mutableStateOf<Int?>(null) }
    var rejectAlasan by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) { uiState.successMessage?.let { snackbarHostState.showSnackbar(it); onClearMessages() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Error: $it"); onClearMessages() } }

    Scaffold(
        topBar = { BpkpadTopBar("Antrean Persetujuan", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(selected = uiState.selectedTab == 0, onClick = { onTabSelect(0) }, text = { Text("Peminjaman (${uiState.pendingTransaksi.size})") })
                Tab(selected = uiState.selectedTab == 1, onClick = { onTabSelect(1) }, text = { Text("Perpanjangan (${uiState.pendingPerpanjangan.size})") })
            }

            if (uiState.isLoading) { BpkpadLoadingIndicator() }
            else when (uiState.selectedTab) {
                0 -> if (uiState.pendingTransaksi.isEmpty()) {
                    BpkpadEmptyState("Tidak ada pengajuan yang menunggu persetujuan", Icons.Default.CheckCircle)
                } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.pendingTransaksi) { t ->
                        PendingApprovalCard(
                            transaksi = t,
                            onViewDetail = { onNavigateToDetail(t.id) },
                            onApprove = { approveDialogId = t.id },
                            onReject = { rejectDialogId = t.id }
                        )
                    }
                }
                1 -> if (uiState.pendingPerpanjangan.isEmpty()) {
                    BpkpadEmptyState("Tidak ada perpanjangan yang menunggu persetujuan", Icons.Default.EventAvailable)
                } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.pendingPerpanjangan) { p ->
                        PerpanjanganCard(p,
                            onApprove = { onApprovePerpanjangan(p.id, p.transaksiId) },
                            onReject = { alasan -> onRejectPerpanjangan(p.id, p.transaksiId, alasan) }
                        )
                    }
                }
            }
        }
    }

    // Approve Dialog
    approveDialogId?.let { id ->
        var catatan by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { approveDialogId = null },
            title = { Text("Setujui Pengajuan #$id") },
            text = {
                Column {
                    Text("Pengajuan akan disetujui dan QR Code digenerate.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(catatan, { catatan = it }, label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = { onApproveTransaksi(id, catatan.ifBlank { null }); approveDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BpkpadGreen)
                ) { Text("Setujui", color = Color.White) }
            },
            dismissButton = { TextButton({ approveDialogId = null }) { Text("Batal") } }
        )
    }

    // Reject Dialog
    rejectDialogId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectDialogId = null; rejectAlasan = "" },
            title = { Text("Tolak Pengajuan #$id") },
            text = {
                Column {
                    Text("Alasan penolakan wajib diisi:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(rejectAlasan, { rejectAlasan = it }, label = { Text("Alasan *") }, isError = rejectAlasan.isBlank(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (rejectAlasan.isNotBlank()) { onRejectTransaksi(id, rejectAlasan); rejectDialogId = null; rejectAlasan = "" } },
                    enabled = rejectAlasan.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BpkpadRed)
                ) { Text("Tolak", color = Color.White) }
            },
            dismissButton = { TextButton({ rejectDialogId = null; rejectAlasan = "" }) { Text("Batal") } }
        )
    }
}

@Composable
private fun PendingApprovalCard(transaksi: Transaksi, onViewDetail: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${transaksi.id} ${transaksi.namaInstansi.ifBlank { "Instansi #${transaksi.instansiId}" }}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusBadge(transaksi.status)
            }
            Spacer(Modifier.height(4.dp))
            Text("PIC: ${transaksi.picNama} · ${transaksi.picNoHp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text("Surat: ${transaksi.nomorSuratPengantar}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text("Kembali rencana: ${transaksi.tanggalKembaliRencana}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onViewDetail, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Detail") }
                Button(onApprove, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(BpkpadGreen)) { Text("Setujui", color = Color.White) }
                Button(onReject, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(BpkpadRed)) { Text("Tolak", color = Color.White) }
            }
        }
    }
}

@Composable
private fun PerpanjanganCard(perpanjangan: Perpanjangan, onApprove: () -> Unit, onReject: (String) -> Unit) {
    var showReject by remember { mutableStateOf(false) }
    var alasan by remember { mutableStateOf("") }
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Perpanjangan · Transaksi #${perpanjangan.transaksiId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("${perpanjangan.tanggalKembaliLama} → ${perpanjangan.tanggalKembaliBaru}", style = MaterialTheme.typography.bodySmall)
            Text("Alasan: ${perpanjangan.alasan}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onApprove, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(BpkpadGreen)) { Text("Setujui", color = Color.White) }
                Button({ showReject = true }, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(BpkpadRed)) { Text("Tolak", color = Color.White) }
            }
        }
    }
    if (showReject) {
        AlertDialog(
            onDismissRequest = { showReject = false },
            title = { Text("Tolak Perpanjangan") },
            text = { OutlinedTextField(alasan, { alasan = it }, label = { Text("Alasan *") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button({ onReject(alasan); showReject = false }, enabled = alasan.isNotBlank()) { Text("Tolak") } },
            dismissButton = { TextButton({ showReject = false }) { Text("Batal") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AntreanApproval_Preview() {
    BpkpadTheme {
        AntreanApprovalContent(
            uiState = AntreanApprovalUiState(isLoading = false),
            onBack = {}, onNavigateToDetail = {}, onTabSelect = {},
            onApproveTransaksi = { _, _ -> }, onRejectTransaksi = { _, _ -> },
            onApprovePerpanjangan = { _, _ -> }, onRejectPerpanjangan = { _, _, _ -> },
            onClearMessages = {}
        )
    }
}
