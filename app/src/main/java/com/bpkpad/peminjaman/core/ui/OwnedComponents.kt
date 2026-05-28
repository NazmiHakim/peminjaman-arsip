package com.bpkpad.peminjaman.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bpkpad.peminjaman.core.common.toDisplayString
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.peminjaman.domain.model.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * [OWNED] StatusBadge
 * Ownership: Modul Peminjaman
 * Scope: Global (Dashboard, Riwayat, Detail)
 * RBAC: Neutral
 * v1.0 2026-05-24
 */
@Composable
fun StatusBadge(status: TransaksiStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        TransaksiStatus.MENUNGGU_PERSETUJUAN -> Pair(BpkpadGold, "Menunggu")
        TransaksiStatus.DISETUJUI            -> Pair(BpkpadBlueLight, "Disetujui")
        TransaksiStatus.DITOLAK              -> Pair(BpkpadRed, "Ditolak")
        TransaksiStatus.DIPINJAM             -> Pair(BpkpadGreen, "Dipinjam")
        TransaksiStatus.DIKEMBALIKAN         -> Pair(Color(0xFF546E7A), "Dikembalikan")
        TransaksiStatus.DIBATALKAN           -> Pair(Color(0xFF78909C), "Dibatalkan")
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * [OWNED] TransaksiCard
 * Ownership: Modul Peminjaman
 * Scope: Global (Dashboard, Riwayat, Detail)
 * RBAC: Neutral
 * v1.0 2026-05-24
 */
@Composable
fun TransaksiCard(
    transaksi: Transaksi,
    showOverdueBadge: Boolean = false,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = transaksi.isOverdue
    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isOverdue) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue && showOverdueBadge)
                BpkpadRed.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isOverdue && showOverdueBadge)
            androidx.compose.foundation.BorderStroke(1.dp, BpkpadRed.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${transaksi.id} · ${transaksi.namaInstansi.ifBlank { "Instansi #${transaksi.instansiId}" }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                StatusBadge(transaksi.status)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                Text(transaksi.picNama, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Kembali: ${transaksi.tanggalKembaliRencana.toDisplayString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue && showOverdueBadge) BpkpadRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isOverdue && showOverdueBadge) {
                Spacer(Modifier.height(6.dp))
                Surface(color = BpkpadRed.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Modifier.size(13.dp), tint = BpkpadRed)
                        Spacer(Modifier.width(4.dp))
                        Text("Terlambat ${transaksi.daysOverdue} hari", style = MaterialTheme.typography.labelSmall, color = BpkpadRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (transaksi.needsBypassAcknowledge) {
                Spacer(Modifier.height(4.dp))
                BypassIndicator(isAcknowledged = false)
            }
        }
    }
}

/**
 * [OWNED] AuditTimelineItem
 * Ownership: Modul Peminjaman
 * Scope: DetailTransaksi audit trail
 * v1.0 2026-05-24
 */
@Composable
fun AuditTimelineItem(log: AuditLog, isLast: Boolean = false, modifier: Modifier = Modifier) {
    val sdf = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID")) }
    val actionColor = when (log.aksi) {
        AuditAction.DISETUJUI_ONLINE,
        AuditAction.PERPANJANGAN_DISETUJUI,
        AuditAction.DOKUMEN_DISERAHKAN,
        AuditAction.DIKEMBALIKAN_BAIK     -> BpkpadGreen
        AuditAction.DITOLAK,
        AuditAction.PERPANJANGAN_DITOLAK,
        AuditAction.DIKEMBALIKAN_HILANG   -> BpkpadRed
        AuditAction.DIBATALKAN            -> Color(0xFF78909C)
        AuditAction.DIKEMBALIKAN_RUSAK    -> BpkpadOrange
        AuditAction.DISETUJUI_BYPASS,
        AuditAction.BYPASS_DIAKUI_KASUBAG -> BpkpadOrange
        else                              -> BpkpadBlue
    }
    Row(modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(12.dp).background(actionColor, CircleShape))
            if (!isLast) {
                Box(Modifier.width(2.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 4.dp)) {
            Text(
                log.aksi.code.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = actionColor
            )
            Text(log.namaUser, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            log.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            Text(sdf.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * [OWNED] BypassIndicator
 * Ownership: Modul Peminjaman
 * Scope: TransaksiCard, DetailTransaksi
 * v1.0 2026-05-24
 */
@Composable
fun BypassIndicator(isAcknowledged: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (isAcknowledged) BpkpadGreen.copy(alpha = 0.1f) else BpkpadOrange.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isAcknowledged) Icons.Default.CheckCircle else Icons.Default.Warning,
                null, Modifier.size(13.dp),
                tint = if (isAcknowledged) BpkpadGreen else BpkpadOrange
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (isAcknowledged) "Bypass diakui Kasubag" else "Bypass · Perlu verifikasi Kasubag",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAcknowledged) BpkpadGreen else BpkpadOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * [OWNED] QrCodeDisplay
 * Generates QR code bitmap from token using ZXing.
 * v1.0 2026-05-24
 */
@Composable
fun QrCodeDisplay(token: String, size: Dp = 200.dp, modifier: Modifier = Modifier) {
    val bitmap = remember(token) {
        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 512, 512, hints)
            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) for (y in 0 until 512) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
            bmp
        } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code: $token",
            modifier = modifier.size(size).clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(token, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
    }
}

/**
 * [OWNED] DokumenListItem
 * v1.0 2026-05-24
 */
@Composable
fun DokumenListItem(
    detail: DetailPeminjaman,
    onConditionSelect: ((KondisiPengembalian) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(detail.nomorDokumen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            detail.perihalDokumen?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            detail.lokasiRak?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(3.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            detail.kondisiPengembalian?.let { kondisi ->
                Spacer(Modifier.height(4.dp))
                val (color, label) = when (kondisi) {
                    KondisiPengembalian.BAIK   -> Pair(BpkpadGreen, "Baik")
                    KondisiPengembalian.RUSAK  -> Pair(BpkpadOrange, "Rusak")
                    KondisiPengembalian.HILANG -> Pair(BpkpadRed, "Hilang")
                }
                Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                    Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                detail.catatanKondisi?.let { catatan ->
                    Spacer(Modifier.height(2.dp))
                    Text("Catatan: $catatan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/**
 * [OWNED] BpkpadDatePickerField
 *
 * Ownership: Modul Peminjaman
 * Scope: Global (Form transaksi, Laporan)
 * Theme: BpkpadTheme compliant
 * RBAC: Neutral
 *
 * Changelog:
 * - v1.0 (2026-05-28): Initial read-only date field with Material date picker.
 *
 * Dependencies:
 * - androidx.compose.material3.DatePickerDialog
 * - androidx.compose.material3.OutlinedTextField
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BpkpadDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    isError: Boolean = error != null,
    placeholder: String = "Pilih tanggal"
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedDate = remember(value) { value.toLocalDateOrNull() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = selectedDate?.toDisplayString() ?: value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { showPicker = true }
            ),
        readOnly = true,
        singleLine = true,
        isError = isError,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pilih tanggal")
            }
        },
        shape = RoundedCornerShape(12.dp)
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toLocalDate()
                            ?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            ?.let(onDateSelected)
                        showPicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    if (isBlank()) return null
    return try {
        LocalDate.parse(trim())
    } catch (e: Exception) {
        null
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

@Preview(showBackground = true, name = "StatusBadge All States")
@Composable
private fun StatusBadge_Preview() {
    BpkpadTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TransaksiStatus.entries.forEach { StatusBadge(it) }
        }
    }
}

@Preview(showBackground = true, name = "DatePickerField")
@Composable
private fun BpkpadDatePickerField_Preview() {
    BpkpadTheme {
        BpkpadDatePickerField(
            value = "2026-05-28",
            onDateSelected = {},
            label = "Tanggal Kembali Rencana *",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "BypassIndicator")
@Composable
private fun BypassIndicator_Preview() {
    BpkpadTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BypassIndicator(false)
            BypassIndicator(true)
        }
    }
}
