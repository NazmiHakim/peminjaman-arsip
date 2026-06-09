package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.navigation.Screen
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.bpkpad.peminjaman.R

/**
 * [LOCAL] DashboardKasubagScreen
 * Ownership: Dashboard feature (Kasubag view)
 * RBAC: KASUBAG only
 * v2.3 - Full UI with Expandable FAB & Clickable Cards
 */
@Composable
fun DashboardKasubagScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardKasubagViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardKasubagContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onLogout = { viewModel.logout(); onLogout() }
    )
}

@Composable
fun DashboardKasubagContent(
    uiState: DashboardKasubagUiState,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        // --- EXPANDABLE FLOATING ACTION BUTTON ---
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        MiniFabItem("Laporan", Icons.Default.Assessment, Color(0xFFE65100)) { isFabExpanded = false; onNavigate(Screen.Laporan.route) }
                        MiniFabItem("Riwayat", Icons.Default.History, Color(0xFF546E7A)) { isFabExpanded = false; onNavigate(Screen.ListRiwayat.route) }
                    }
                }
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = Color(0xFF207125),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = "Menu Aksi"
                    )
                }
            }
        },
        // -----------------------------------------
        bottomBar = {
            KasubagBottomNavBar(
                onDashboard = {},
                onSearch = { onNavigate(Screen.ListRiwayat.route) },
                selectedIndex = 0
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            BpkpadLoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp) // Ruang bawah agar tidak tertutup FAB
            ) {
                // ── Header ──
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.lambang_balangan), // Panggil PNG Anda
                                    contentDescription = "Logo Kabupaten Balangan",
                                    modifier = Modifier
                                        .size(40.dp) // Ukuran 40dp agar terlihat gagah
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("BPKPAD Balangan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                            }
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = { showLogoutDialog = true }), contentAlignment = Alignment.Center) { Icon(Icons.Default.ExitToApp, "Keluar", tint = Color(0xFFE53935), modifier = Modifier.size(24.dp)) }
                        }
                    }
                }

                // ── Welcome & Title ──
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        uiState.session?.let { s -> Text("Selamat datang, ${s.namaLengkap}", fontSize = 14.sp, color = Color(0xFF6B7280)) }
                        Text("Dashboard Kasubag", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                        Spacer(Modifier.height(2.dp))
                        Text("Kelola persetujuan dan monitor peminjaman arsip", fontSize = 13.sp, color = Color(0xFF6B7280))
                    }
                }

                // ── Status Cards Row (Clickable) ──
                item {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val totalSemua = uiState.totalMenunggu + uiState.totalDipinjam + uiState.totalOverdue + uiState.totalDikembalikan
                        KFigmaStatCard("Total Pengajuan", "Semua Waktu", totalSemua, Icons.Default.Description, Color(0xFFDFF5E1), Color(0xFF207125)) { onNavigate("list_riwayat?status=ALL") }
                        KFigmaStatCard("Menunggu", "Semua Waktu", uiState.totalMenunggu, Icons.Default.HourglassEmpty, Color(0xFFFFF3CD), Color(0xFFD4A017)) { onNavigate("list_riwayat?status=MENUNGGU_PERSETUJUAN") }
                        KFigmaStatCard("Sedang dipinjam", "Semua Waktu", uiState.totalDipinjam, Icons.Default.CheckCircleOutline, Color(0xFFD1ECF1), Color(0xFF0C5460)) { onNavigate("list_riwayat?status=DIPINJAM") }
                        KFigmaStatCard("Selesai dipinjam", "Semua Waktu", uiState.totalDikembalikan, Icons.Default.CheckCircle, Color(0xFFD4EDDA), Color(0xFF155724)) { onNavigate("list_riwayat?status=DIKEMBALIKAN") }
                        KFigmaStatCard("Terlambat", "Semua Waktu", uiState.totalOverdue, Icons.Default.Warning, Color(0xFFF8D7DA), Color(0xFFC62828)) { onNavigate("list_riwayat?status=TERLAMBAT") }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Pending Approval Alert ──
                if (uiState.totalMenunggu > 0) {
                    item {
                        Card(onClick = { onNavigate(Screen.AntreanApproval.route) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PendingActions, null, Modifier.size(32.dp), tint = Color(0xFFD4A017))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${uiState.totalMenunggu} Pengajuan Menunggu", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                                    Text("Ketuk untuk melihat antrean", fontSize = 12.sp, color = Color(0xFF6B7280))
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFD4A017))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Bypass Pending ──
                if (uiState.bypassPendingList.isNotEmpty()) {
                    item {
                        Text("Bypass · Perlu Verifikasi (${uiState.bypassPendingList.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BpkpadOrange, modifier = Modifier.padding(horizontal = 20.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.bypassPendingList) { t ->
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) { TransaksiCard(t, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) }) }
                    }
                }

                // ── Perpanjangan Pending ──
                if (uiState.perpanjanganPending > 0) {
                    item {
                        Card(onClick = { onNavigate(Screen.AntreanApproval.route) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD1ECF1)), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EventRepeat, null, Modifier.size(28.dp), tint = Color(0xFF0C5460))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${uiState.perpanjanganPending} Perpanjangan Pending", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                                    Text("Ketuk untuk review perpanjangan", fontSize = 12.sp, color = Color(0xFF6B7280))
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF0C5460))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // --- Latar gelap pembantu saat FAB mekar ---
            if (isFabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { isFabExpanded = false }
                )
            }

            // --- Dialog Logout ---
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.Bold) },
                    text = { Text("Apakah Anda yakin ingin keluar dari aplikasi?") },
                    confirmButton = { Button(onClick = { showLogoutDialog = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Ya, Keluar") } },
                    dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal", color = Color(0xFF6B7280)) } }
                )
            }
        }
    }
}

// ── KOMPONEN PENDUKUNG (HANYA DITULIS 1 KALI) ──

@Composable
private fun MiniFabItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.padding(end = 12.dp)) {
            Text(label, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF374151), fontWeight = FontWeight.Medium)
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = color, contentColor = Color.White) { Icon(icon, null) }
    }
}

@Composable
private fun KFigmaStatCard(label: String, subLabel: String, value: Int, icon: ImageVector, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.width(140.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = bgColor, shadowElevation = 3.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Column { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151)); Text(subLabel, fontSize = 10.sp, color = Color(0xFF6B7280)) }
            Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            Text("Lihat Detail  >", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = iconColor)
        }
    }
}

@Composable
private fun KasubagBottomNavBar(onDashboard: () -> Unit, onSearch: () -> Unit, selectedIndex: Int = 0) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KNavItem(Icons.Default.Home, "Dashboard", selectedIndex == 0, onDashboard)
            Spacer(Modifier.width(48.dp))
            KNavItem(Icons.Default.Search, "Search", selectedIndex == 1, onSearch)
        }
    }
}

@Composable
private fun KNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val contentColor = if (isSelected) Color(0xFF207125) else Color(0xFF9CA3AF)
    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).then(if (isSelected) Modifier.background(Color(0xFFDFF5E1), RoundedCornerShape(12.dp)) else Modifier).padding(horizontal = 24.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = contentColor)
    }
}

@Preview(showBackground = true, name = "Dashboard Kasubag")
@Composable
private fun DashboardKasubag_Preview() {
    BpkpadTheme { DashboardKasubagContent(uiState = DashboardKasubagUiState(totalMenunggu = 3, totalDipinjam = 8, totalOverdue = 1, totalDikembalikan = 12, perpanjanganPending = 2, isLoading = false), onNavigate = {}, onLogout = {}) }
}