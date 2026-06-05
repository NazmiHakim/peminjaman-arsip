package com.bpkpad.peminjaman.peminjaman.presentation.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color(0xFFF7F8FA),
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
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // ── Figma Header: Back arrow + "Detail Pengajuan" ──
                    item {
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
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Kembali",
                                        tint = Color(0xFF374151),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Detail Pengajuan",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                        }
                    }

                    // ── Header card: Instansi + Status ──
                    item {
                        Card(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text(t.namaInstansi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusBadge(t.status)
                                }
                                if (t.needsBypassAcknowledge) { Spacer(Modifier.height(4.dp)); BypassIndicator(false) }
                                else if (t.metodePersetujuan == MetodePersetujuan.BYPASS) { Spacer(Modifier.height(4.dp)); BypassIndicator(true) }

                                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE5E7EB))
                            }
                        }
                    }

                    // ── Section: Informasi Pengajuan ──
                    item {
                        DetailSectionCard(title = "Informasi Pengajuan") {
                            FigmaDetailRow(Icons.Default.Person, "Nama Pemohon", "${t.picNama} (${t.picNoHp})")
                            FigmaDetailRow(Icons.Default.Business, "Unit Kerja", t.namaInstansi)
                            FigmaDetailRow(Icons.Default.Phone, "No. Telepon", t.picNoHp)
                        }
                    }

                    // ── Section: Data Arsip ──
                    item {
                        DetailSectionCard(title = "Data Arsip") {
                            FigmaDetailRow(Icons.Default.Description, "Nomor Surat", t.nomorSuratPengantar)
                            FigmaDetailRow(Icons.Default.CalendarMonth, "Tgl Pinjam", t.tanggalPinjam.toDisplayString())
                            FigmaDetailRow(Icons.Default.Event, "Tenggat", t.tanggalKembaliRencana.toDisplayString(), isHighlight = t.isOverdue)
                            t.tanggalKembaliAktual?.let {
                                FigmaDetailRow(Icons.Default.EventAvailable, "Kembali Aktual", it.toDisplayString())
                            }
                        }
                    }

                    // ── Overdue Warning ──
                    if (t.isOverdue) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                color = Color(0xFFF8D7DA),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = Color(0xFFC62828))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Terlambat ${t.daysOverdue} hari", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // ── Alasan Penolakan ──
                    t.alasanPenolakan?.let { alasan ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                color = BpkpadRed.copy(0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("Alasan Penolakan:", style = MaterialTheme.typography.labelSmall, color = BpkpadRed, fontWeight = FontWeight.SemiBold)
                                    Text(alasan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // ── Catatan Bypass ──
                    t.catatanBypass?.let { catatan ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                color = BpkpadOrange.copy(0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("Catatan Bypass:", style = MaterialTheme.typography.labelSmall, color = BpkpadOrange, fontWeight = FontWeight.SemiBold)
                                    Text(catatan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // ── QR Code ──
                    t.qrCodeToken?.let { token ->
                        item {
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDFF5E1).copy(alpha = 0.6f))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF207125))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(id = R.string.qr_card_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF207125)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.qr_card_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF374151)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    QrCodeDisplay(token, 200.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            token,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF207125),
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

                    // ── Documents ──
                    if (t.details.isNotEmpty()) {
                        item {
                            Text(
                                "Dokumen Dipinjam (${t.details.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(t.details) { detail -> DokumenListItem(detail) }
                    }

                    // ── Action buttons (RBAC) ──
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Arsiparis actions
                            if (role == UserRole.ARSIPARIS) {
                                if (t.status == TransaksiStatus.DISETUJUI) {
                                    Button(
                                        onClick = onConfirmHandover,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
                                    ) {
                                        Text("Konfirmasi Serah Dokumen", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (t.status == TransaksiStatus.DIPINJAM) {
                                    Button(
                                        onClick = { showReturnDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
                                    ) {
                                        Text("Selesaikan Peminjaman", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (t.picNoHp.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val phone = t.picNoHp.filter { it.isDigit() }.let { if (it.startsWith("0")) "62${it.substring(1)}" else it }
                                            val msg = "Halo ${t.picNama}, reminder dokumen peminjaman ${t.nomorSuratPengantar} dari BPKPAD Balangan. Tenggat: ${t.tanggalKembaliRencana.toDisplayString()}. Terima kasih."
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Constants.WHATSAPP_API_URL}?phone=$phone&text=${Uri.encode(msg)}")))
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, null, tint = Color(0xFF207125))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Hubungi via WhatsApp", color = Color(0xFF207125), fontWeight = FontWeight.SemiBold)
                                    }
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
                                    Button(
                                        onClick = onAcknowledgeBypass,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
                                    ) {
                                        Text("Ketahui (Verifikasi Bypass)", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (t.canBeCancelled) {
                                    TextButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Batalkan Transaksi (Override)", color = BpkpadRed)
                                    }
                                }
                            }
                        }
                    }

                    // ── Audit Trail ──
                    if (uiState.auditLogs.isNotEmpty()) {
                        item {
                            Text(
                                "Audit Trail (${uiState.auditLogs.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
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

// ── Figma-style Section Card ──
@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

// ── Figma-style Detail Row with icon ──
@Composable
private fun FigmaDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color(0xFF9CA3AF))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) Color(0xFFC62828) else Color(0xFF1A1A1A)
            )
        }
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
