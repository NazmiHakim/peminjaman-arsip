package com.bpkpad.peminjaman.peminjaman.presentation.riwayat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus

/**
 * [LOCAL] ListRiwayatScreen
 * Ownership: Riwayat feature
 * RBAC: Both roles
 * v1.0 2026-05-24
 */
@Composable
fun ListRiwayatScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ListRiwayatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ListRiwayatContent(uiState = uiState, onBack = onBack, onNavigateToDetail = onNavigateToDetail, onSearch = viewModel::onSearch, onStatusFilter = viewModel::onStatusFilter)
}

@Composable
fun ListRiwayatContent(
    uiState: ListRiwayatUiState,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onStatusFilter: (TransaksiStatus?) -> Unit
) {
    Scaffold(topBar = { BpkpadTopBar("Riwayat Peminjaman", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearch,
                label = { Text("Cari instansi, PIC, nomor surat...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                    { IconButton({ onSearch("") }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Status filter chips
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = uiState.selectedStatus == null, onClick = { onStatusFilter(null) }, label = { Text("Semua") })
                }
                items(TransaksiStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { onStatusFilter(if (uiState.selectedStatus == status) null else status) },
                        label = { Text(status.name.replace('_', ' ')) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (uiState.isLoading) {
                BpkpadLoadingIndicator()
            } else if (uiState.filteredList.isEmpty()) {
                BpkpadEmptyState("Tidak ada transaksi ditemukan", Icons.Default.SearchOff)
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            onBack = {}, onNavigateToDetail = {}, onSearch = {}, onStatusFilter = {}
        )
    }
}
