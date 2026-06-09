package com.bpkpad.peminjaman.peminjaman.presentation.approval

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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color(0xFFF7F8FA),
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    Text("Antrean Persetujuan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }

            // ── Tab Row ──
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF207125),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = Color(0xFF207125)
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    text = { Text("Peminjaman (${uiState.pendingTransaksi.size})", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFF207125),
                    unselectedContentColor = Color(0xFF6B7280)
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    text = { Text("Perpanjangan (${uiState.pendingPerpanjangan.size})", fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Color(0xFF207125),
                    unselectedContentColor = Color(0xFF6B7280)
                )
            }

            // ── Content ──
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
                    OutlinedTextField(catatan, { catatan = it }, label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { onApproveTransaksi(id, catatan.ifBlank { null }); approveDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
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
                    OutlinedTextField(rejectAlasan, { rejectAlasan = it }, label = { Text("Alasan *") }, isError = rejectAlasan.isBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
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
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${transaksi.id} ${transaksi.namaInstansi}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusBadge(transaksi.status)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = Color(0xFF9CA3AF))
                Spacer(Modifier.width(4.dp))
                Text("${transaksi.picNama} · ${transaksi.picNoHp}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, Modifier.size(14.dp), tint = Color(0xFF9CA3AF))
                Spacer(Modifier.width(4.dp))
                Text("Surat: ${transaksi.nomorSuratPengantar}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, Modifier.size(14.dp), tint = Color(0xFF9CA3AF))
                Spacer(Modifier.width(4.dp))
                Text("Kembali rencana: ${transaksi.tanggalKembaliRencana}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onViewDetail, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Detail", color = Color(0xFF374151)) }
                Button(onApprove, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(Color(0xFF207125))) { Text("Setujui", color = Color.White) }
                Button(onReject, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(BpkpadRed)) { Text("Tolak", color = Color.White) }
            }
        }
    }
}

@Composable
private fun PerpanjanganCard(perpanjangan: Perpanjangan, onApprove: () -> Unit, onReject: (String) -> Unit) {
    var showReject by remember { mutableStateOf(false) }
    var alasan by remember { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Perpanjangan · Transaksi #${perpanjangan.transaksiId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, Modifier.size(14.dp), tint = Color(0xFF9CA3AF))
                Spacer(Modifier.width(4.dp))
                Text("${perpanjangan.tanggalKembaliLama} → ${perpanjangan.tanggalKembaliBaru}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = Color(0xFF9CA3AF))
                Spacer(Modifier.width(4.dp))
                Text("Alasan: ${perpanjangan.alasan}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onApprove, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(Color(0xFF207125))) { Text("Setujui", color = Color.White) }
                Button({ showReject = true }, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(BpkpadRed)) { Text("Tolak", color = Color.White) }
            }
        }
    }
    if (showReject) {
        AlertDialog(
            onDismissRequest = { showReject = false },
            title = { Text("Tolak Perpanjangan") },
            text = { OutlinedTextField(alasan, { alasan = it }, label = { Text("Alasan *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) },
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
