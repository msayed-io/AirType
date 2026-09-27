package com.livetype.remote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** ML Kit QR analyzer (bundled model — nothing is downloaded at scan time). */
private class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                codes.firstOrNull()?.rawValue?.let(onResult)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

@Composable
fun QrScannerScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        AppHeader(s("scanner_title"), onBack = { vm.goHome() })
        Spacer(Modifier.height(6.dp))
        Text(
            s("scanner_hint"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        ErrorBanner(ui.lastError)
        Spacer(Modifier.height(10.dp))

        if (hasPermission) {
            CameraQrView(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onQr = { vm.onQrScanned(it) },
            )
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(s("perm_camera_title"), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(s("perm_camera_text"), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(s("grant"))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        s("err_camera_denied"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { vm.goManualEntry() }) { Text(s("manual_entry")) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CameraQrView(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQr: (String) -> Unit,
) {
    val previewView = remember { PreviewView(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    // Debounce: one action per scanned value.
    val lastScan = remember { AtomicLong(0) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(hasPermission) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analyzerExecutor, QrAnalyzer { raw ->
                    val now = System.currentTimeMillis()
                    val last = lastScan.get()
                    if (now - last > 1500 && lastScan.compareAndSet(last, now)) {
                        mainHandler.post { onQr(raw) }
                    }
                })
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (_: Exception) {
                // Camera unavailable (emulator without camera, busy camera): user can
                // still pair through manual entry.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try { provider?.unbindAll() } catch (_: Exception) { }
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        factory = {
            previewView.apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
    )
}
