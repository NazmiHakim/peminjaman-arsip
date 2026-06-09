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
    var showLogoutDialog by remember { mutableStateOf(false) }
    // State untuk mengontrol mekar/tutupnya FAB
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
                        MiniFabItem("Scan QR", Icons.Default.QrCodeScanner, Color(0xFF1976D2)) { isFabExpanded = false; onNavigate("scan_qr") }
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
            FigmaBottomNavBar(
                onDashboard = {},
                onSearch = { onNavigate(Screen.ListRiwayat.route) },
                onAdd = { onNavigate(Screen.FormTransaksi.route) },
                onInstansi = { onNavigate(Screen.ListInstansi.route) },
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { FigmaHeader(userName = uiState.session?.namaLengkap ?: "Admin", onLogoutClick = { showLogoutDialog = true }) }

                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("Peminjaman Arsip", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                        Spacer(Modifier.height(2.dp))
                        Text("Kelola pengajuan dan riwayat peminjaman arsip", fontSize = 13.sp, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item {
                    Button(
                        onClick = { onNavigate(Screen.FormTransaksi.route) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125), contentColor = Color.White)
                    ) { Text("+ Ajukan Peminjaman", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    FigmaStatusCardsRow(
                        onNavigate = onNavigate,
                        totalAll = uiState.totalDipinjam + uiState.totalMenunggu + uiState.totalOverdue,
                        totalMenunggu = uiState.totalMenunggu,
                        totalDipinjam = uiState.totalDipinjam,
                        totalOverdue = uiState.totalOverdue
                    )
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pengajuan Terbaru", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                        Text("Lihat Semua", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF207125), modifier = Modifier.clickable { onNavigate(Screen.ListRiwayat.route) })
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (uiState.overdueList.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = BpkpadRed)
                            Spacer(Modifier.width(6.dp))
                            Text("Terlambat (${uiState.overdueList.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BpkpadRed)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.overdueList) { t ->
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            TransaksiCard(t, showOverdueBadge = true, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) })
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                items(uiState.recentList.take(5)) { t ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        TransaksiCard(t, showOverdueBadge = t.isOverdue, onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) } // Ruang kosong tambahan di bawah agar tidak tertutup FAB
            }

            // --- Jika FAB mekar, kita bisa tambahkan scrim/layar redup pembantu ---
            if (isFabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { isFabExpanded = false } // Klik dimana saja untuk menutup
                )
            }

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

// ── Komponen Mini FAB ──
@Composable
private fun MiniFabItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun FigmaHeader(userName: String, onLogoutClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.lambang_balangan), // Panggil PNG Anda
                    contentDescription = "Logo Kabupaten Balangan",
                    modifier = Modifier
                        .size(40.dp) // Sesuaikan ukurannya agar pas, misal 40dp
                        .padding(end = 4.dp) // Beri sedikit jarak ke tulisan
                )
                Spacer(Modifier.width(10.dp))
                Text("BPKPAD Balangan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = onLogoutClick), contentAlignment = Alignment.Center) { Icon(Icons.Default.ExitToApp, "Keluar", tint = Color(0xFFE53935), modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun FigmaStatusCardsRow(onNavigate: (String) -> Unit, totalAll: Int, totalMenunggu: Int, totalDipinjam: Int, totalOverdue: Int) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FigmaStatCard("Total Pengajuan", "Semua Waktu", totalAll, Icons.Default.Description, Color(0xFFDFF5E1), Color(0xFF207125)) { onNavigate("list_riwayat?status=ALL") }
        FigmaStatCard("Menunggu", "Persetujuan", totalMenunggu, Icons.Default.HourglassEmpty, Color(0xFFFFF3CD), Color(0xFFD4A017)) { onNavigate("list_riwayat?status=MENUNGGU_PERSETUJUAN") }
        FigmaStatCard("Sedang dipinjam", "Aktif", totalDipinjam, Icons.Default.CheckCircleOutline, Color(0xFFD1ECF1), Color(0xFF0C5460)) { onNavigate("list_riwayat?status=DIPINJAM") }
        FigmaStatCard("Terlambat", "Overdue", totalOverdue, Icons.Default.Warning, Color(0xFFF8D7DA), Color(0xFFC62828)) { onNavigate("list_riwayat?status=TERLAMBAT") }
    }
}

@Composable
private fun FigmaStatCard(label: String, subLabel: String, value: Int, icon: ImageVector, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
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
private fun FigmaBottomNavBar(onDashboard: () -> Unit, onSearch: () -> Unit, onAdd: () -> Unit, onInstansi: () -> Unit, selectedIndex: Int = 0) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            FigmaNavItem(Icons.Default.Home, "Dashboard", selectedIndex == 0, onDashboard)
            FigmaNavItem(Icons.Default.Search, "Search", selectedIndex == 1, onSearch)
            FigmaNavItem(Icons.Default.AddCircleOutline, "Add", selectedIndex == 2, onAdd)
            FigmaNavItem(Icons.Default.Business, "Instansi", selectedIndex == 3, onInstansi)
        }
    }
}

@Composable
private fun FigmaNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val contentColor = if (isSelected) Color(0xFF207125) else Color(0xFF9CA3AF)
    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).then(if (isSelected) Modifier.background(Color(0xFFDFF5E1), RoundedCornerShape(12.dp)) else Modifier).padding(horizontal = 16.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = contentColor)
    }
}

@Preview(showBackground = true, name = "Dashboard Arsiparis")
@Composable
private fun DashboardArsiparis_Preview() {
    BpkpadTheme { DashboardArsiparisContent(uiState = DashboardArsiparisUiState(totalDipinjam = 5, totalMenunggu = 2, totalOverdue = 1, isLoading = false), onNavigate = {}, onLogout = {}) }
}