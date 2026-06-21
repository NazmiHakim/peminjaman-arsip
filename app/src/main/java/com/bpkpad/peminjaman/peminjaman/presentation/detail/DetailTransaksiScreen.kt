package com.bpkpad.peminjaman.peminjaman.presentation.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage // <-- INI LIBRARY UNTUK BACA GAMBAR
import com.bpkpad.peminjaman.R
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.core.common.InputRules
import com.bpkpad.peminjaman.core.common.toDisplayString
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.core.ui.*
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.*
import com.bpkpad.peminjaman.qr.QrShareHelper
import java.time.temporal.ChronoUnit

/**
 * [LOCAL] DetailTransaksiScreen
 * Ownership: Peminjaman feature
 * RBAC: Both roles (different action buttons)
 * v2.2 - Implemented Coil AsyncImage for real photo rendering
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
    val bypassProofPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(viewModel::onBypassProofSelected)
    }
    val extensionLetterPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(viewModel::onExtensionLetterSelected)
    }

    LaunchedEffect(uiState.successMessage) { uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Error: $it"); viewModel.clearMessages() } }

    DetailTransaksiContent(
        uiState = uiState, snackbarHostState = snackbarHostState, onBack = onBack,
        onConfirmHandover = viewModel::confirmHandover,
        onReturn = viewModel::returnTransaksi,
        onBypassNoteChange = viewModel::onBypassNoteChange,
        onPickBypassProof = { bypassProofPicker.launch("image/*") },
        onSubmitBypass = viewModel::bypassPendingTransaksi,
        onExtensionDateChange = viewModel::onExtensionDateChange,
        onExtensionReasonChange = viewModel::onExtensionReasonChange,
        onPickExtensionLetter = { extensionLetterPicker.launch("image/*") },
        onSubmitExtension = viewModel::createExtension,
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
    onBypassNoteChange: (String) -> Unit,
    onPickBypassProof: () -> Unit,
    onSubmitBypass: () -> Unit,
    onExtensionDateChange: (String) -> Unit,
    onExtensionReasonChange: (String) -> Unit,
    onPickExtensionLetter: () -> Unit,
    onSubmitExtension: () -> Unit,
    onAcknowledgeBypass: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val qrShareFailedMessage = stringResource(id = R.string.qr_share_failed)
    var showReturnDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showBypassDialog by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
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
                    item {
                        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Detail Pengajuan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                            }
                        }
                    }

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
                            }
                        }
                    }

                    // ── Data Peminjam ──
                    item {
                        FigmaNewSectionCard(title = "Data Peminjam") {
                            FigmaNewKeyValueRow("Instansi", t.namaInstansi)
                            FigmaNewKeyValueRow("PIC", t.picNama)
                            FigmaNewKeyValueRow("NO. HP", t.picNoHp)
                            FigmaNewKeyValueRow("No. Surat Pengantar", t.nomorSuratPengantar)
                        }
                    }

                    // ── Dokumen yang Dipinjam ──
                    if (t.details.isNotEmpty()) {
                        item {
                            FigmaNewSectionCard(title = "Dokumen yang Dipinjam") {
                                t.details.forEachIndexed { index, detail ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("${index + 1}", fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.width(24.dp))
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(detail.nomorDokumen, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                                            if (!detail.perihalDokumen.isNullOrBlank()) {
                                                Text(detail.perihalDokumen ?: "", fontSize = 12.sp, color = Color(0xFF6B7280))
                                            }
                                        }
                                        val docType = if (detail.nomorDokumen.contains("SP2D", ignoreCase = true)) "SP2D" else "Lampiran"
                                        Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE5E7EB)), color = Color.Transparent) {
                                            Text(docType, fontSize = 11.sp, color = Color(0xFF374151), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        }
                                    }
                                    if (index < t.details.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp), color = Color(0xFFF3F4F6))
                                    }
                                }
                            }
                        }
                    }

                    // ── Jadwal Peminjaman ──
                    item {
                        FigmaNewSectionCard(title = "Jadwal Peminjaman") {
                            FigmaNewKeyValueRow("Tanggal Pinjam", t.tanggalPinjam.toDisplayString())
                            val durasi = ChronoUnit.DAYS.between(t.tanggalPinjam, t.tanggalKembaliRencana)
                            FigmaNewKeyValueRow("Tanggal Kembali", "${t.tanggalKembaliRencana.toDisplayString()} ($durasi Hari)")
                            t.tanggalKembaliAktual?.let {
                                Spacer(Modifier.height(4.dp))
                                FigmaNewKeyValueRow("Dikembalikan", it.toDisplayString())
                            }
                        }
                    }

                    // ── Foto Surat Pengantar ──
                    item {
                        FigmaNewSectionCard(title = "Foto Surat Pengantar") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { showImageDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                // --- MODIFIKASI: Menampilkan gambar sungguhan dengan Coil ---
                                val imagePath = t.fotoSuratPengantarPath?.replace("local://", "file://")

                                if (!imagePath.isNullOrBlank()) {
                                    AsyncImage(
                                        model = imagePath,
                                        contentDescription = "Foto Surat Pengantar",
                                        contentScale = ContentScale.Crop, // Crop agar memenuhi kotak 180dp
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Jika benar-benar kosong, baru tampilkan ikon abu-abu
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Image, contentDescription = "Foto", modifier = Modifier.size(48.dp), tint = Color(0xFF9CA3AF))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Ketuk untuk melihat foto", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                                    }
                                }
                                // -----------------------------------------------------------

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (t.isOverdue) {
                        item {
                            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFF8D7DA), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = Color(0xFFC62828))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Terlambat ${t.daysOverdue} hari", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    t.alasanPenolakan?.let { alasan ->
                        item {
                            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = BpkpadRed.copy(0.08f), shape = RoundedCornerShape(10.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("Alasan Penolakan:", style = MaterialTheme.typography.labelSmall, color = BpkpadRed, fontWeight = FontWeight.SemiBold)
                                    Text(alasan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    t.catatanBypass?.let { catatan ->
                        item {
                            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = BpkpadOrange.copy(0.08f), shape = RoundedCornerShape(10.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("Catatan Bypass:", style = MaterialTheme.typography.labelSmall, color = BpkpadOrange, fontWeight = FontWeight.SemiBold)
                                    Text(catatan, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    t.qrCodeToken?.let { token ->
                        item {
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.fillMaxWidth().background(Color(0xFFDFF5E1).copy(alpha = 0.5f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF207125))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(id = R.string.qr_card_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF207125))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    QrCodeDisplay(token, 200.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                                        Text(token, style = MaterialTheme.typography.labelMedium, color = Color(0xFF207125), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                    val canSendActiveQr = t.status != TransaksiStatus.DIKEMBALIKAN && t.status != TransaksiStatus.DIBATALKAN && t.status != TransaksiStatus.DITOLAK
                                    if (role == UserRole.ARSIPARIS && t.picNoHp.isNotBlank() && canSendActiveQr) {
                                        Spacer(Modifier.height(12.dp))
                                        BpkpadSecondaryButton(
                                            text = stringResource(id = R.string.btn_send_qr_whatsapp),
                                            onClick = {
                                                val sent = QrShareHelper.sendQrToWhatsApp(context, t.picNoHp, token, buildQrWhatsAppMessage(t, token), "qr_transaksi_${t.id}")
                                                if (!sent) Toast.makeText(context, qrShareFailedMessage, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.perpanjanganList.isNotEmpty()) {
                        item {
                            FigmaNewSectionCard(title = "Riwayat Perpanjangan") {
                                uiState.perpanjanganList.forEachIndexed { index, extension ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Text(
                                            "${extension.tanggalKembaliLama.toDisplayString()} → ${extension.tanggalKembaliBaru.toDisplayString()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Status: ${extension.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(extension.alasan, style = MaterialTheme.typography.bodySmall)
                                        extension.alasanPenolakan?.let {
                                            Text(
                                                "Alasan penolakan: $it",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BpkpadRed
                                            )
                                        }
                                    }
                                    if (index < uiState.perpanjanganList.lastIndex) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    // ── Action buttons (RBAC) ──
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (role == UserRole.ARSIPARIS) {
                                if (t.canBeBypassed) {
                                    Button(
                                        onClick = { showBypassDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BpkpadBlue)
                                    ) {
                                        Text(
                                            stringResource(R.string.btn_bypass),
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (t.status == TransaksiStatus.DISETUJUI) {
                                    Button(onClick = onConfirmHandover, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))) { Text("Konfirmasi Serah Dokumen", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                }
                                if (t.status == TransaksiStatus.DIPINJAM) {
                                    val hasPendingExtension = uiState.perpanjanganList.any {
                                        it.status == PerpanjanganStatus.PENDING
                                    }
                                    OutlinedButton(
                                        onClick = { showExtensionDialog = true },
                                        enabled = !hasPendingExtension,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.EventRepeat, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (hasPendingExtension) {
                                                "Perpanjangan Menunggu Persetujuan"
                                            } else {
                                                "Ajukan Perpanjangan"
                                            }
                                        )
                                    }
                                    Button(onClick = { showReturnDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))) { Text("Selesaikan Peminjaman", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                }
                                if (t.picNoHp.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val phone = t.picNoHp.filter { it.isDigit() }.let { if (it.startsWith("0")) "62${it.substring(1)}" else it }
                                            val msg = "Halo ${t.picNama}, reminder dokumen peminjaman ${t.nomorSuratPengantar} dari BPKPAD Balangan. Tenggat: ${t.tanggalKembaliRencana.toDisplayString()}. Terima kasih."
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Constants.WHATSAPP_API_URL}?phone=$phone&text=${Uri.encode(msg)}")))
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, null, tint = Color(0xFF207125))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Hubungi via WhatsApp", color = Color(0xFF207125), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (t.canBeCancelled) {
                                    TextButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Batalkan Transaksi", color = BpkpadRed) }
                                }
                            }
                            if (role == UserRole.KASUBAG) {
                                if (t.needsBypassAcknowledge) {
                                    Button(onClick = onAcknowledgeBypass, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))) { Text("Ketahui (Verifikasi Bypass)", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                }
                                if (t.canBeCancelled) {
                                    TextButton(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Batalkan Transaksi (Override)", color = BpkpadRed) }
                                }
                            }
                        }
                    }

                    if (uiState.auditLogs.isNotEmpty()) {
                        item {
                            Text("Audit Trail (${uiState.auditLogs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                        itemsIndexed(uiState.auditLogs) { idx, log ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                val fallbackReturnNotes = if (
                                    log.catatan.isNullOrBlank() &&
                                    log.aksi in setOf(
                                        AuditAction.DIKEMBALIKAN_RUSAK,
                                        AuditAction.DIKEMBALIKAN_HILANG
                                    )
                                ) {
                                    t.details.mapNotNull { detail ->
                                        detail.catatanKondisi
                                            ?.takeIf(String::isNotBlank)
                                            ?.let { note ->
                                                "${detail.nomorDokumen}: $note"
                                            }
                                    }.joinToString("\n").ifBlank { null }
                                } else null
                                AuditTimelineItem(
                                    log = log.copy(catatan = log.catatan ?: fallbackReturnNotes),
                                    isLast = idx == uiState.auditLogs.size - 1
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    // ── DIALOG GAMBAR FULLSCREEN ──
    if (showBypassDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) showBypassDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = BpkpadOrange
                )
            },
            title = { Text(stringResource(R.string.bypass_pending_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.bypass_pending_description))
                    BpkpadSecondaryButton(
                        text = if (uiState.bypassProofUri == null) {
                            stringResource(R.string.bypass_attach_proof)
                        } else {
                            stringResource(R.string.bypass_replace_proof)
                        },
                        onClick = onPickBypassProof
                    )
                    if (uiState.bypassProofUri != null) {
                        Text(
                            stringResource(R.string.bypass_proof_selected),
                            color = Color(0xFF207125),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    BpkpadTextField(
                        value = uiState.bypassNote,
                        onValueChange = onBypassNoteChange,
                        label = stringResource(R.string.bypass_note_label),
                        singleLine = false,
                        maxLines = 5,
                        error = InputRules.validateBypassNote(uiState.bypassNote)
                            .takeIf { uiState.bypassNote.isNotEmpty() }
                    )
                    Text(
                        "${uiState.bypassNote.length}/${InputRules.BYPASS_NOTE_MAX}",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isLoading &&
                        uiState.bypassProofUri != null &&
                        InputRules.validateBypassNote(uiState.bypassNote) == null,
                    onClick = {
                        onSubmitBypass()
                        showBypassDialog = false
                    }
                ) {
                    Text(stringResource(R.string.bypass_process))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = { showBypassDialog = false }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showExtensionDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) showExtensionDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.EventRepeat,
                    contentDescription = null,
                    tint = BpkpadBlue
                )
            },
            title = { Text("Ajukan Perpanjangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tenggat saat ini: ${uiState.transaksi?.tanggalKembaliRencana?.toDisplayString()}"
                    )
                    BpkpadDatePickerField(
                        value = uiState.extensionDate,
                        onDateSelected = onExtensionDateChange,
                        label = "Tanggal kembali baru *",
                        error = uiState.extensionDate
                            .takeIf { it.isNotBlank() }
                            ?.let { selected ->
                                val oldDate = uiState.transaksi?.tanggalKembaliRencana
                                val newDate = runCatching {
                                    java.time.LocalDate.parse(selected)
                                }.getOrNull()
                                if (newDate == null || oldDate == null || !newDate.isAfter(oldDate)) {
                                    "Tanggal harus setelah tenggat saat ini"
                                } else null
                            }
                    )
                    BpkpadSecondaryButton(
                        text = if (uiState.extensionLetterUri == null) {
                            "Lampirkan Surat Perpanjangan"
                        } else {
                            "Ganti Surat Perpanjangan"
                        },
                        onClick = onPickExtensionLetter
                    )
                    if (uiState.extensionLetterUri != null) {
                        Text(
                            "Surat perpanjangan sudah dipilih",
                            color = Color(0xFF207125),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    BpkpadTextField(
                        value = uiState.extensionReason,
                        onValueChange = onExtensionReasonChange,
                        label = "Alasan perpanjangan *",
                        singleLine = false,
                        maxLines = 5,
                        error = InputRules.validateExtensionReason(uiState.extensionReason)
                            .takeIf { uiState.extensionReason.isNotEmpty() }
                    )
                    Text(
                        "${uiState.extensionReason.length}/${InputRules.EXTENSION_REASON_MAX}",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                val oldDate = uiState.transaksi?.tanggalKembaliRencana
                val newDate = runCatching {
                    java.time.LocalDate.parse(uiState.extensionDate)
                }.getOrNull()
                TextButton(
                    enabled = !uiState.isLoading &&
                        uiState.extensionLetterUri != null &&
                        InputRules.validateExtensionReason(uiState.extensionReason) == null &&
                        oldDate != null &&
                        newDate?.isAfter(oldDate) == true,
                    onClick = {
                        onSubmitExtension()
                        showExtensionDialog = false
                    }
                ) {
                    Text("Ajukan")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = { showExtensionDialog = false }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showImageDialog) {
        Dialog(
            onDismissRequest = { showImageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showImageDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Mengambil 90% lebar layar
                        .fillMaxHeight(0.8f) // Mengambil 80% tinggi layar
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // --- MODIFIKASI: Menampilkan gambar full di dialog ---
                    val imagePath = uiState.transaksi?.fotoSuratPengantarPath?.replace("local://", "file://")

                    if (!imagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = imagePath,
                            contentDescription = "Foto Fullscreen",
                            contentScale = ContentScale.Fit, // Fit agar seluruh gambar terlihat tanpa terpotong
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = "Foto", modifier = Modifier.size(80.dp), tint = Color(0xFF9CA3AF))
                            Spacer(Modifier.height(16.dp))
                            Text("Tidak ada foto lampiran", color = Color(0xFF6B7280), fontSize = 14.sp)
                        }
                    }
                    // -----------------------------------------------------
                }
            }
        }
    }

    if (showReturnDialog && uiState.transaksi != null) {
        KondisiReturnDialog(
            details = uiState.transaksi.details,
            onConfirm = { map -> onReturn(map); showReturnDialog = false },
            onDismiss = { showReturnDialog = false }
        )
    }

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

// ── KOMPONEN DESAIN FIGMA BARU ──

@Composable
private fun FigmaNewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun FigmaNewKeyValueRow(key: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = key,
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value ?: "-",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun buildQrWhatsAppMessage(transaksi: Transaksi, qrToken: String): String {
    return buildString {
        append("Halo ${transaksi.picNama}, pengajuan peminjaman dokumen ")
        append(transaksi.nomorSuratPengantar ?: "-")
        append(" telah disetujui oleh BPKPAD Balangan.\n\n")
        append("Token QR pengembalian: $qrToken\n")
        append("Tenggat pengembalian: ${transaksi.tanggalKembaliRencana.toDisplayString()}\n\n")
        append("Mohon tunjukkan QR ini saat proses pengembalian dokumen. Terima kasih.")
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
            onBack = {},
            onConfirmHandover = {},
            onReturn = {},
            onBypassNoteChange = {},
            onPickBypassProof = {},
            onSubmitBypass = {},
            onExtensionDateChange = {},
            onExtensionReasonChange = {},
            onPickExtensionLetter = {},
            onSubmitExtension = {},
            onAcknowledgeBypass = {},
            onCancel = {}
        )
    }
}
