package com.bpkpad.peminjaman.peminjaman.presentation.pengembalian

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.R
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.core.ui.BpkpadTopBar
import com.bpkpad.peminjaman.peminjaman.presentation.pengembalian.components.CameraPermissionPlaceholder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [LOCAL] ScanQrScreen
 * Ownership: Pengembalian feature (Arsiparis only)
 * RBAC: ARSIPARIS
 * v1.0 2026-05-24
 */
@Composable
fun ScanQrScreen(
    onBack: () -> Unit,
    onFound: (Int) -> Unit,
    viewModel: ScanQrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.foundTransaksiId) {
        uiState.foundTransaksiId?.let { id -> viewModel.clearFound(); onFound(id) }
    }
    ScanQrContent(
        uiState = uiState,
        onBack = onBack,
        onQrDetected = viewModel::onQrDetected,
        onManualSearch = viewModel::findByToken,
        onClearError = viewModel::clearError
    )
}

@Composable
fun ScanQrContent(
    uiState: ScanQrUiState,
    onBack: () -> Unit,
    onQrDetected: (String) -> Unit,
    onManualSearch: (String) -> Unit,
    onClearError: () -> Unit
) {
    var manualToken by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(
            isPreview || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showCameraPermissionDialog by remember { mutableStateOf(!hasCameraPermission && !isPreview) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        showCameraPermissionDialog = false
    }

    LaunchedEffect(hasCameraPermission, isPreview) {
        if (!hasCameraPermission && !isPreview) showCameraPermissionDialog = true
    }

    Scaffold(topBar = { BpkpadTopBar("Scan QR Code Pengembalian", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera preview box
            Card(
                Modifier.fillMaxWidth().aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box {
                    if (hasCameraPermission) {
                        CameraPreviewWithQrScan(onQrDetected, Modifier.fillMaxSize())
                        // Viewfinder overlay
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                Modifier.size(180.dp),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.7f))
                            ) {}
                        }
                    } else {
                        CameraPermissionPlaceholder(
                            onRequestPermission = { showCameraPermissionDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Arahkan kamera ke QR Code pada surat persetujuan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Atau masukkan token manual:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = manualToken, onValueChange = { manualToken = it },
                label = { Text("Token QR (contoh: QR-NORMAL-004)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (manualToken.isNotBlank()) onManualSearch(manualToken) },
                enabled = manualToken.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                else { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Cari Transaksi") }
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClearError, Modifier.size(20.dp)) { Icon(Icons.Default.Close, "Tutup", Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            title = { Text(stringResource(id = R.string.qr_permission_title)) },
            text = { Text(stringResource(id = R.string.qr_permission_message)) },
            confirmButton = {
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(id = R.string.qr_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text(stringResource(id = R.string.qr_permission_later))
                }
            }
        )
    }
}

@Composable
private fun CameraPreviewWithQrScan(onQrDetected: (String) -> Unit, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val detectedRef = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = androidx.camera.core.Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imgProxy ->
                    if (!detectedRef.get()) {
                        imgProxy.image?.let { img ->
                            val input = InputImage.fromMediaImage(img, imgProxy.imageInfo.rotationDegrees)
                            scanner.process(input)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                        ?.rawValue?.let { value ->
                                            if (detectedRef.compareAndSet(false, true)) {
                                                onQrDetected(value)
                                            }
                                        }
                                }
                                .addOnCompleteListener { imgProxy.close() }
                        } ?: imgProxy.close()
                    } else {
                        imgProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CAMERA", e.message ?: "Camera bind failed")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ScanQr_Preview() {
    BpkpadTheme {
        ScanQrContent(ScanQrUiState(), onBack = {}, onQrDetected = {}, onManualSearch = {}, onClearError = {})
    }
}
