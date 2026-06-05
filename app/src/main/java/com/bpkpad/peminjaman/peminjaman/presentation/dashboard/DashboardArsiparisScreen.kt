package com.bpkpad.peminjaman.peminjaman.presentation.dashboard

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

/**
 * [LOCAL] DashboardArsiparisScreen
 * Ownership: Dashboard feature (Arsiparis view)
 * RBAC: ARSIPARIS only
 * v2.0 - Figma-aligned UI with original functionality preserved
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
        containerColor = Color.White,
        bottomBar = {
            FigmaBottomNavBar(
                onDashboard = {},
                onSearch = { onNavigate(Screen.ListRiwayat.route) },
                onAdd = { onNavigate(Screen.FormTransaksi.route) },
                onProfile = onLogout,
                selectedIndex = 0
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            BpkpadLoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header ──
            item {
                FigmaHeader(
                    userName = uiState.session?.namaLengkap ?: "Admin",
                    onLogout = onLogout
                )
            }

            // ── Title & Subtitle ──
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Peminjaman Arsip",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Kelola pengajuan dan riwayat peminjaman arsip",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── "+ Ajukan Peminjaman" Button ──
            item {
                Button(
                    onClick = { onNavigate(Screen.FormTransaksi.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF207125),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "+ Ajukan Peminjaman",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Status Cards (Horizontal Scroll) ──
            item {
                FigmaStatusCardsRow(
                    totalAll = uiState.totalDipinjam + uiState.totalMenunggu + uiState.totalOverdue,
                    totalMenunggu = uiState.totalMenunggu,
                    totalDipinjam = uiState.totalDipinjam,
                    totalOverdue = uiState.totalOverdue
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── "Pengajuan Terbaru" Section ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengajuan Terbaru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Lihat Semua",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF207125),
                        modifier = Modifier.clickable { onNavigate(Screen.ListRiwayat.route) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Overdue List ──
            if (uiState.overdueList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = BpkpadRed)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Terlambat (${uiState.overdueList.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BpkpadRed
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(uiState.overdueList) { t ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        TransaksiCard(
                            t,
                            showOverdueBadge = true,
                            onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Recent Transactions ──
            items(uiState.recentList.take(5)) { t ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TransaksiCard(
                        t,
                        showOverdueBadge = t.isOverdue,
                        onCardClick = { onNavigate(Screen.DetailTransaksi.createRoute(t.id)) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Figma: Header (Logo + BPKPAD Balangan + Bell icon) ──
@Composable
private fun FigmaHeader(userName: String, onLogout: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo placeholder (green circle with icon)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF207125)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "BPKPAD Balangan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }

            // Bell / notification icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Logout",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Figma: Status Cards Row (Horizontal scrollable) ──
@Composable
private fun FigmaStatusCardsRow(
    totalAll: Int,
    totalMenunggu: Int,
    totalDipinjam: Int,
    totalOverdue: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FigmaStatCard(
            label = "Total Pengajuan",
            subLabel = "Semua Waktu",
            value = totalAll,
            icon = Icons.Default.Description,
            bgColor = Color(0xFFDFF5E1),
            iconColor = Color(0xFF207125)
        )
        FigmaStatCard(
            label = "Menunggu",
            subLabel = "Persetujuan",
            value = totalMenunggu,
            icon = Icons.Default.HourglassEmpty,
            bgColor = Color(0xFFFFF3CD),
            iconColor = Color(0xFFD4A017)
        )
        FigmaStatCard(
            label = "Sedang dipinjam",
            subLabel = "Aktif",
            value = totalDipinjam,
            icon = Icons.Default.CheckCircleOutline,
            bgColor = Color(0xFFD1ECF1),
            iconColor = Color(0xFF0C5460)
        )
        FigmaStatCard(
            label = "Terlambat",
            subLabel = "Overdue",
            value = totalOverdue,
            icon = Icons.Default.Warning,
            bgColor = Color(0xFFF8D7DA),
            iconColor = Color(0xFFC62828)
        )
    }
}

@Composable
private fun FigmaStatCard(
    label: String,
    subLabel: String,
    value: Int,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color
) {
    Surface(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280)
                )
            }
            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

// ── Figma: Bottom Navigation Bar ──
@Composable
private fun FigmaBottomNavBar(
    onDashboard: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onProfile: () -> Unit,
    selectedIndex: Int = 0
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FigmaNavItem(
                icon = Icons.Default.Home,
                label = "Dashboard",
                isSelected = selectedIndex == 0,
                onClick = onDashboard
            )
            FigmaNavItem(
                icon = Icons.Default.Search,
                label = "Search",
                isSelected = selectedIndex == 1,
                onClick = onSearch
            )
            FigmaNavItem(
                icon = Icons.Default.AddCircleOutline,
                label = "Add",
                isSelected = selectedIndex == 2,
                onClick = onAdd
            )
            FigmaNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedIndex == 3,
                onClick = onProfile
            )
        }
    }
}

@Composable
private fun FigmaNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) Color(0xFF207125) else Color(0xFF9CA3AF)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected)
                    Modifier.background(Color(0xFFDFF5E1), RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Preview(showBackground = true, name = "Dashboard Arsiparis")
@Composable
private fun DashboardArsiparis_Preview() {
    BpkpadTheme {
        DashboardArsiparisContent(
            uiState = DashboardArsiparisUiState(
                totalDipinjam = 5,
                totalMenunggu = 2,
                totalOverdue = 1,
                isLoading = false
            ),
            onNavigate = {},
            onLogout = {}
        )
    }
}
