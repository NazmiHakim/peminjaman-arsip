package com.bpkpad.peminjaman.peminjaman.presentation.riwayat

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus

/**
 * [LOCAL] ListRiwayatScreen
 * Ownership: Riwayat feature
 * RBAC: Both roles
 * v1.1 - Added status filter param support from Dashboard
 */
@Composable
fun ListRiwayatScreen(
    statusFilter: String? = null, // <-- Parameter dari NavGraph
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ListRiwayatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- TRIGGER FILTER AWAL DARI DASHBOARD ---
    LaunchedEffect(statusFilter) {
        viewModel.setInitialFilter(statusFilter)
    }

    ListRiwayatContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToDetail = onNavigateToDetail,
        onSearch = viewModel::onSearch,
        onStatusFilter = viewModel::onStatusFilter,
        onOverdueToggle = viewModel::onOverdueToggle
    )
}

@Composable
fun ListRiwayatContent(
    uiState: ListRiwayatUiState,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onStatusFilter: (TransaksiStatus?) -> Unit,
    onOverdueToggle: (Boolean) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF7F8FA)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Figma-style Header: "BPKPAD Balangan" ──
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
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))

                    // Green logo circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF207125)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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
            }

            // ── Search Bar ──
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("Cari Peminjaman", color = Color(0xFF9CA3AF)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                    { IconButton({ onSearch("") }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = Color(0xFF207125)
                )
            )

            // ── Status Filter Chips ──
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatus == null && !uiState.isOverdueOnly,
                        onClick = { onStatusFilter(null) },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF207125),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(TransaksiStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status && !uiState.isOverdueOnly,
                        onClick = { onStatusFilter(if (uiState.selectedStatus == status) null else status) },
                        label = { Text(status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }, // Dibuat lebih rapi format teksnya
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF207125),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ── Overdue Toggle Switch (Hanya muncul jika status "Dipinjam" terpilih) ──
            AnimatedVisibility(visible = uiState.selectedStatus == TransaksiStatus.DIPINJAM) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Tampilkan Hanya Terlambat (Overdue)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.isOverdueOnly) Color(0xFFC62828) else Color(0xFF374151)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = uiState.isOverdueOnly,
                        onCheckedChange = onOverdueToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFC62828),
                            uncheckedThumbColor = Color(0xFF9CA3AF),
                            uncheckedTrackColor = Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            // ── Transaction List ──
            if (uiState.isLoading) {
                BpkpadLoadingIndicator()
            } else if (uiState.filteredList.isEmpty()) {
                BpkpadEmptyState("Tidak ada transaksi ditemukan", Icons.Default.SearchOff)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredList) { t ->
                        TransaksiCard(t, showOverdueBadge = t.isOverdue, onCardClick = { onNavigateToDetail(t.id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListRiwayat_Preview() {
    BpkpadTheme {
        ListRiwayatContent(
            uiState = ListRiwayatUiState(isLoading = false),
            onBack = {}, onNavigateToDetail = {}, onSearch = {}, onStatusFilter = {}, onOverdueToggle = {}
        )
    }
}