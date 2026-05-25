package com.bpkpad.peminjaman.peminjaman.presentation.pengembalian.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bpkpad.peminjaman.R
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.BpkpadPrimaryButton

/**
 * [LOCAL] CameraPermissionPlaceholder
 *
 * Ownership: Pengembalian feature
 * Scope: Scan QR permission state
 * Theme: BpkpadTheme compliant
 * RBAC: Neutral
 *
 * Changelog:
 * - v1.0 (2026-05-25): Initial permission placeholder.
 *
 * Dependencies:
 * - com.bpkpad.peminjaman.core.ui.BpkpadPrimaryButton (SHARED stub)
 */
@Composable
fun CameraPermissionPlaceholder(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.padding(16.dp).size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.qr_permission_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.qr_permission_unavailable_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        BpkpadPrimaryButton(
            text = stringResource(id = R.string.qr_permission_allow),
            onClick = onRequestPermission
        )
    }
}

@Preview(showBackground = true, name = "Camera Permission Placeholder")
@Composable
private fun CameraPermissionPlaceholder_Preview() {
    BpkpadTheme {
        CameraPermissionPlaceholder(onRequestPermission = {}, modifier = Modifier.size(320.dp))
    }
}
