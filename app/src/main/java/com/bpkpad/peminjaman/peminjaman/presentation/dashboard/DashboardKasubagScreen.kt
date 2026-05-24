package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.navigation.Screen

/**
 * [LOCAL] DashboardKasubagScreen
 * Ownership: Dashboard feature (Kasubag view)
 * RBAC: KASUBAG only
 * v1.0 2026-05-24
 */
@Composable
fun DashboardKasubagScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardKasubagViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardKasubagContent(uiState = uiState, onNavigate = onNavigate, onLogout = { viewModel.logout(); onLogout() })
}

@Composable
fun DashboardKasubagContent(
    uiState: DashboardKasubagUiState,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            BpkpadTopBar(title = "Dashboard Kasubag", actions = {
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
            })
        }
    ) { padding ->
        if (uiState.isLoading) { BpkpadLoadingIndicator(Modifier.padding(padding)); return@Scaffold }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                uiState.session?.let { s ->
                    Text("Selamat datang, ${s.namaLengkap}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Kasubag · BPKPAD Balangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Stats grid 2×2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KStatCard("Menunggu", uiState.totalMenunggu, Icons.Default.PendingActions, BpkpadGold, Modifier.weight(1f))
                        KStatCard("Dipinjam", uiState.totalDipinjam, Icons.Default.BookmarkAdded, BpkpadGreen, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KStatCard("Terlambat", uiState.totalOverdue, Icons.Default.Warning, BpkpadRed, Modifier.weight(1f))
                        KStatCard("Dikembalikan", uiState.totalDikembalikan, Icons.Default.AssignmentReturn, Color(0xFF546E7A), Modifier.weight(1f))
                    }
                }
            }

            // Pending approval alert
            if (uiState.totalMenunggu > 0) {
                item {
                    Card(
                        onClick = { onNavigate(Screen.AntreanApproval.route) },
                        colors = CardDefaults.cardColors(containerColor = BpkpadGold.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PendingActions, null, Modifier.size(32.dp), tint = BpkpadGold)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${uiState.totalMenunggu} Pengajuan Menunggu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Ketuk untuk melihat antrean", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = BpkpadGold)
                        }
                    }
                }
            }

            // Bypass pending
            if (uiState.bypassPendingList.isNotEmpty()) {
                item {
                    Text("Bypass · Perlu Verifikasi (${uiState.bypassPendingList.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BpkpadOrange)
                }
                items(uiState.bypassPendingList) { t ->
                    TransaksiCard(t, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) })
                }
            }

            // Perpanjangan pending alert
            if (uiState.perpanjanganPending > 0) {
                item {
                    Card(
                        onClick = { onNavigate(Screen.AntreanApproval.route) },
                        colors = CardDefaults.cardColors(containerColor = BpkpadBlue.copy(0.07f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventRepeat, null, Modifier.size(28.dp), tint = BpkpadBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${uiState.perpanjanganPending} Perpanjangan Pending", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Ketuk untuk review perpanjangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = BpkpadBlue)
                        }
                    }
                }
            }

            // Quick menu
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuCard("Riwayat", Icons.Default.History, Color(0xFF546E7A), { onNavigate(Screen.ListRiwayat.route) }, Modifier.weight(1f))
                    MenuCard("Laporan", Icons.Default.Assessment, BpkpadBlue, { onNavigate(Screen.Laporan.route) }, Modifier.weight(1f))
                    MenuCard("Dokumen", Icons.Default.LibraryBooks, Color(0xFF7B1FA2), { onNavigate(Screen.ListDokumen.route) }, Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KStatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = color.copy(0.08f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), color = color.copy(0.2f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MenuCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(36.dp), color = color.copy(0.12f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(20.dp), tint = color) }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Kasubag")
@Composable
private fun DashboardKasubag_Preview() {
    BpkpadTheme {
        DashboardKasubagContent(
            uiState = DashboardKasubagUiState(
                totalMenunggu = 3, totalDipinjam = 8, totalOverdue = 1,
                totalDikembalikan = 12, perpanjanganPending = 2, isLoading = false
            ),
            onNavigate = {}, onLogout = {}
        )
    }
}
