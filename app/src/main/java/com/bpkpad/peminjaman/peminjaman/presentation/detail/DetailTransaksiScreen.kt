package com.bpkpad.peminjaman.peminjaman.presentation.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.R
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.core.common.toDisplayString
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.*
import com.bpkpad.peminjaman.qr.QrShareHelper

/**
 * [LOCAL] DetailTransaksiScreen
 * Ownership: Peminjaman feature
 * RBAC: Both roles (different action buttons)
 * v1.0 2026-05-24
 */
@Composable
fun DetailTransaksiScreen(
    transaksiId: Int,
    onBack: () -> Unit,
    viewModel: DetailTransaksiViewModel = hiltViewModel()
) {
    LaunchedEffect(transaksiId) { viewModel.load(transaksiId) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) { uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Error: $it"); viewModel.clearMessages() } }

    DetailTransaksiContent(
        uiState = uiState, snackbarHostState = snackbarHostState, onBack = onBack,
        onConfirmHandover = viewModel::confirmHandover,
        onReturn = viewModel::returnTransaksi,
        onAcknowledgeBypass = viewModel::acknowledgeBypass,
        onCancel = viewModel::cancelTransaksi
    )
}

@Composable
fun DetailTransaksiContent(
    uiState: DetailTransaksiUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onConfirmHandover: () -> Unit,
    onReturn: (Map<Int, Pair<String, String?>>) -> Unit,
    onAcknowledgeBypass: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val qrShareFailedMessage = stringResource(id = R.string.qr_share_failed)
    var showReturnDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    val role = uiState.session?.role

    Scaffold(
        topBar = { BpkpadTopBar("Detail Transaksi #${uiState.transaksi?.id ?: ""}", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading && uiState.transaksi == null ->
                BpkpadLoadingIndicator(Modifier.padding(padding))
            uiState.error != null && uiState.transaksi == null ->
                BpkpadErrorView(uiState.error, {}, Modifier.padding(padding))
            uiState.transaksi != null -> {
                val t = uiState.transaksi
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card
                    item {
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text(t.namaInstansi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusBadge(t.status)
                                }
                                if (t.needsBypassAcknowledge) { Spacer(Modifier.height(4.dp)); BypassIndicator(false) }
                                else if (t.metodePersetujuan == MetodePersetujuan.BYPASS) { Spacer(Modifier.height(4.dp)); BypassIndicator(true) }

                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                DetailRow("PIC", "${t.picNama} (${t.picNoHp})")
                                DetailRow("Surat", t.nomorSuratPengantar)
                                DetailRow("Tgl Pinjam", t.tanggalPinjam.toDisplayString())
                                DetailRow("Tenggat", t.tanggalKembaliRencana.toDisplayString(), isHighlight = t.isOverdue)
                                t.tanggalKembaliAktual?.let { DetailRow("Kembali Aktual", it.toDisplayString()) }

                                if (t.isOverdue) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = BpkpadRed.copy(0.1f), shape = RoundedCornerShape(6.dp)) {
                                        Text("Terlambat ${t.daysOverdue} hari", color = BpkpadRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                t.alasanPenolakan?.let { alasan ->
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = BpkpadRed.copy(0.08f), shape = RoundedCornerShape(8.dp)) {
                                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                            Text("Alasan Penolakan:", style = MaterialTheme.typography.labelSmall, color = BpkpadRed, fontWeight = FontWeight.SemiBold)
                                            Text(alasan, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                t.catatanBypass?.let { catatan ->
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = BpkpadOrange.copy(0.08f), shape = RoundedCornerShape(8.dp)) {
                                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                            Text("Catatan Bypass:", style = MaterialTheme.typography.labelSmall, color = BpkpadOrange, fontWeight = FontWeight.SemiBold)
                                            Text(catatan, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // QR Code
                    t.qrCodeToken?.let { token ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(3.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(id = R.string.qr_card_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.qr_card_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    QrCodeDisplay(token, 200.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            token,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    val canSendActiveQr = t.status != TransaksiStatus.DIKEMBALIKAN &&
                                        t.status != TransaksiStatus.DIBATALKAN &&
                                        t.status != TransaksiStatus.DITOLAK
                                    if (role == UserRole.ARSIPARIS && t.picNoHp.isNotBlank() && canSendActiveQr) {
                                        Spacer(Modifier.height(12.dp))
                                        BpkpadSecondaryButton(
                                            text = stringResource(id = R.string.btn_send_qr_whatsapp),
                                            onClick = {
                                                val sent = QrShareHelper.sendQrToWhatsApp(
                                                    context = context,
                                                    rawPhoneNumber = t.picNoHp,
                                                    qrToken = token,
                                                    message = buildQrWhatsAppMessage(t, token),
                                                    fileName = "qr_transaksi_${t.id}"
                                                )
                                                if (!sent) {
                                                    Toast.makeText(context, qrShareFailedMessage, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Documents
                    if (t.details.isNotEmpty()) {
                        item { Text("Dokumen Dipinjam (${t.details.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                        items(t.details) { detail -> DokumenListItem(detail) }
                    }

                    // Action buttons (RBAC)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Arsiparis actions
                            if (role == UserRole.ARSIPARIS) {
                                if (t.status == TransaksiStatus.DISETUJUI) {
                                    BpkpadPrimaryButton("Konfirmasi Serah Dokumen", onClick = onConfirmHandover)
                                }
                                if (t.status == TransaksiStatus.DIPINJAM) {
                                    BpkpadPrimaryButton("Selesaikan Peminjaman (Pengembalian)", onClick = { showReturnDialog = true })
                                }
                                if (t.picNoHp.isNotBlank()) {
                                    BpkpadSecondaryButton(
                                        text = "Hubungi via WhatsApp",
                                        onClick = {
                                            val phone = t.picNoHp.filter { it.isDigit() }.let { if (it.startsWith("0")) "62${it.substring(1)}" else it }
                                            val msg = "Halo ${t.picNama}, reminder dokumen peminjaman ${t.nomorSuratPengantar} dari BPKPAD Balangan. Tenggat: ${t.tanggalKembaliRencana.toDisplayString()}. Terima kasih."
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Constants.WHATSAPP_API_URL}?phone=$phone&text=${Uri.encode(msg)}")))
                                        }
                                    )
                                }
                                if (t.canBeCancelled) {
                                    TextButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Batalkan Transaksi", color = BpkpadRed)
                                    }
                                }
                            }
                            // Kasubag actions
                            if (role == UserRole.KASUBAG) {
                                if (t.needsBypassAcknowledge) {
                                    BpkpadPrimaryButton("Ketahui (Verifikasi Bypass)", onClick = onAcknowledgeBypass)
                                }
                                if (t.canBeCancelled) {
                                    TextButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Batalkan Transaksi (Override)", color = BpkpadRed)
                                    }
                                }
                            }
                        }
                    }

                    // Audit Trail
                    if (uiState.auditLogs.isNotEmpty()) {
                        item { Text("Audit Trail (${uiState.auditLogs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                        itemsIndexed(uiState.auditLogs) { idx, log ->
                            AuditTimelineItem(log, isLast = idx == uiState.auditLogs.size - 1)
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Return dialog
    if (showReturnDialog && uiState.transaksi != null) {
        KondisiReturnDialog(
            details = uiState.transaksi.details,
            onConfirm = { map -> onReturn(map); showReturnDialog = false },
            onDismiss = { showReturnDialog = false }
        )
    }

    // Cancel dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Batalkan Transaksi") },
            text = { Text("Apakah Anda yakin? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = { Button({ onCancel(); showCancelDialog = false }, colors = ButtonDefaults.buttonColors(BpkpadRed)) { Text("Batalkan", color = Color.White) } },
            dismissButton = { TextButton({ showCancelDialog = false }) { Text("Kembali") } }
        )
    }
}

private fun buildQrWhatsAppMessage(transaksi: Transaksi, qrToken: String): String {
    return buildString {
        append("Halo ${transaksi.picNama}, pengajuan peminjaman dokumen ")
        append(transaksi.nomorSuratPengantar)
        append(" telah disetujui oleh BPKPAD Balangan.\n\n")
        append("Token QR pengembalian: $qrToken\n")
        append("Tenggat pengembalian: ${transaksi.tanggalKembaliRencana.toDisplayString()}\n\n")
        append("Mohon tunjukkan QR ini saat proses pengembalian dokumen. Terima kasih.")
    }
}

@Composable
private fun DetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (isHighlight) BpkpadRed else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KondisiReturnDialog(details: List<DetailPeminjaman>, onConfirm: (Map<Int, Pair<String, String?>>) -> Unit, onDismiss: () -> Unit) {
    val kondisiMap = remember { mutableStateMapOf<Int, String>().apply { details.forEach { put(it.id, "baik") } } }
    val catatanMap = remember { mutableStateMapOf<Int, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kondisi Pengembalian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.forEach { detail ->
                    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(detail.nomorDokumen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("baik", "rusak", "hilang").forEach { k ->
                                    FilterChip(
                                        selected = kondisiMap[detail.id] == k,
                                        onClick = { kondisiMap[detail.id] = k },
                                        label = { Text(k.replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (k) {
                                                "rusak"  -> BpkpadOrange.copy(0.3f)
                                                "hilang" -> BpkpadRed.copy(0.3f)
                                                else     -> BpkpadGreen.copy(0.3f)
                                            }
                                        )
                                    )
                                }
                            }
                            if (kondisiMap[detail.id] in listOf("rusak", "hilang")) {
                                OutlinedTextField(
                                    value = catatanMap[detail.id] ?: "",
                                    onValueChange = { catatanMap[detail.id] = it },
                                    label = { Text("Catatan kondisi *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = (catatanMap[detail.id] ?: "").isBlank()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val valid = details.all { d -> kondisiMap[d.id].let { k -> if (k in listOf("rusak","hilang")) !(catatanMap[d.id]?:"").isBlank() else true } }
            Button(onClick = {
                onConfirm(details.associate { d -> d.id to Pair(kondisiMap[d.id] ?: "baik", catatanMap[d.id]) })
            }, enabled = valid) { Text("Selesaikan") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Batal") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailTransaksi_Preview() {
    BpkpadTheme {
        DetailTransaksiContent(
            uiState = DetailTransaksiUiState(isLoading = false, error = "Transaksi tidak ditemukan"),
            onBack = {}, onConfirmHandover = {}, onReturn = {}, onAcknowledgeBypass = {}, onCancel = {}
        )
    }
}
