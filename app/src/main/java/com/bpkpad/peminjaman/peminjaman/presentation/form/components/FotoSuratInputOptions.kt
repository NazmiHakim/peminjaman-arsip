package com.bpkpad.peminjaman.peminjaman.presentation.form.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bpkpad.peminjaman.core.theme.BpkpadTheme

/**
 * [LOCAL] FotoSuratInputOptions
 *
 * Ownership: Form transaksi feature
 * Scope: FormTransaksiScreen photo attachment
 * Theme: BpkpadTheme compliant
 * RBAC: Neutral
 *
 * Changelog:
 * - v1.0 (2026-05-28): Added scan/camera and gallery choices.
 *
 * Dependencies:
 * - androidx.compose.material3.OutlinedButton
 */
@Composable
fun FotoSuratInputOptions(
    hasFoto: Boolean,
    isError: Boolean,
    onScanFoto: () -> Unit,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasFoto) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasFoto) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = if (hasFoto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (hasFoto) "Foto surat sudah dilampirkan" else "Lampirkan foto surat pengantar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pakai scanner dokumen untuk crop/filter otomatis, atau upload file dari galeri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onScanFoto,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Dokumen")
                }
                OutlinedButton(
                    onClick = onPickGallery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
            }
            if (isError) {
                Text(
                    text = "Foto surat wajib dilampirkan",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Foto Surat Empty")
@Composable
private fun FotoSuratInputOptions_EmptyPreview() {
    BpkpadTheme {
        FotoSuratInputOptions(
            hasFoto = false,
            isError = false,
            onScanFoto = {},
            onPickGallery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Foto Surat Selected")
@Composable
private fun FotoSuratInputOptions_SelectedPreview() {
    BpkpadTheme {
        FotoSuratInputOptions(
            hasFoto = true,
            isError = false,
            onScanFoto = {},
            onPickGallery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
