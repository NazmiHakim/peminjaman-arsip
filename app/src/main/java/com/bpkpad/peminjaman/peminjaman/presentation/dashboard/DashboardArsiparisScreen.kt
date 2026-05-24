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
 * [LOCAL] DashboardArsiparisScreen
 * Ownership: Dashboard feature (Arsiparis view)
 * RBAC: ARSIPARIS only
 * v1.0 2026-05-24
 */
@Composable
fun DashboardArsiparisScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardArsiparisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardArsiparisContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onLogout = { viewModel.logout(); onLogout() }
    )
}

@Composable
fun DashboardArsiparisContent(
    uiState: DashboardArsiparisUiState,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            BpkpadTopBar(title = "Dashboard Arsiparis", actions = {
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Buat Peminjaman") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { onNavigate(Screen.FormTransaksi.route) },
                containerColor = BpkpadBlue,
                contentColor = Color.White
            )
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
                    Text("Arsiparis · BPKPAD Balangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Stat cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Dipinjam", uiState.totalDipinjam, Icons.Default.BookmarkAdded, BpkpadGreen, Modifier.weight(1f))
                    StatCard("Menunggu", uiState.totalMenunggu, Icons.Default.HourglassEmpty, BpkpadGold, Modifier.weight(1f))
                    StatCard("Terlambat", uiState.totalOverdue, Icons.Default.Warning, BpkpadRed, Modifier.weight(1f))
                }
            }

            // Quick actions
            item { Text("Aksi Cepat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard("Scan QR", Icons.Default.QrCodeScanner, BpkpadBlue, { onNavigate(Screen.ScanQr.route) }, Modifier.weight(1f))
                    ActionCard("Riwayat", Icons.Default.History, Color(0xFF546E7A), { onNavigate(Screen.ListRiwayat.route) }, Modifier.weight(1f))
                    ActionCard("Laporan", Icons.Default.Assessment, BpkpadGold, { onNavigate(Screen.Laporan.route) }, Modifier.weight(1f))
                    ActionCard("Master", Icons.Default.LibraryBooks, Color(0xFF7B1FA2), { onNavigate(Screen.ListDokumen.route) }, Modifier.weight(1f))
                }
            }

            // Overdue list
            if (uiState.overdueList.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = BpkpadRed)
                        Spacer(Modifier.width(6.dp))
                        Text("Terlambat (${uiState.overdueList.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BpkpadRed)
                    }
                }
                items(uiState.overdueList) { t ->
                    TransaksiCard(t, showOverdueBadge = true, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) })
                }
            }

            // Recent
            if (uiState.recentList.isNotEmpty()) {
                item { Text("Aktivitas Terbaru", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                items(uiState.recentList.take(5)) { t ->
                    TransaksiCard(t, showOverdueBadge = t.isOverdue, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Spacer(Modifier.height(4.dp))
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ActionCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(38.dp), color = color.copy(0.15f), shape = RoundedCornerShape(10.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(20.dp), tint = color) }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Arsiparis")
@Composable
private fun DashboardArsiparis_Preview() {
    BpkpadTheme {
        DashboardArsiparisContent(
            uiState = DashboardArsiparisUiState(totalDipinjam = 5, totalMenunggu = 2, totalOverdue = 1, isLoading = false),
            onNavigate = {}, onLogout = {}
        )
    }
}
