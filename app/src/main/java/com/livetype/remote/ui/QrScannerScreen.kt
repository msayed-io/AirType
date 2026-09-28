package com.livetype.remote.ui

import android.Manifest
import android.content.Context
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.livetype.remote.ui.components.FloatingGlassTopBar
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiveTypePrimaryButton
import com.livetype.remote.ui.components.LiveTypeSecondaryButton
import com.livetype.remote.ui.components.TactileCard
import com.livetype.remote.ui.theme.LiveTypeTokens
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FloatingGlassTopBar(
            title = s("scanner_title"),
            onBack = { vm.goHome() },
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            LiquidErrorBanner(ui.lastError)
            Spacer(Modifier.height(10.dp))

            if (hasPermission) {
                // Futuristic Camera Viewport Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(LiveTypeTokens.CardShapeLarge)
                        .border(
                            width = 1.5.dp,
                            color = LiveTypeTokens.BorderGlow,
                            shape = LiveTypeTokens.CardShapeLarge,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraQrView(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        hasPermission = hasPermission,
                        onQr = { vm.onQrScanned(it) },
                    )

                    // Laser sweep animation line
                    val transition = rememberInfiniteTransition(label = "laser")
                    val laserOffset by transition.animateFloat(
                        initialValue = -140f,
                        targetValue = 140f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "laser_y",
                    )

                    // Target scanning square overlay
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(width = 2.dp, color = Color.White.copy(alpha = 0.3f), shape = LiveTypeTokens.CardShapeMedium),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Glowing laser beam
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = laserOffset.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            LiveTypeTokens.CyanPulse,
                                            LiveTypeTokens.VioletNeon,
                                            Color.Transparent,
                                        )
                                    )
                                )
                                .shadow(8.dp, spotColor = LiveTypeTokens.CyanPulse),
                        )
                    }
                }
            } else {
                // Permission Request Card
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgCard,
                    borderColor = LiveTypeTokens.BorderStrong,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LiveTypeTokens.BgCardElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = LiveTypeTokens.IndigoElectric,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = s("perm_camera_title"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = LiveTypeTokens.TextPrimary,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = s("perm_camera_text"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            ),
                        )
                        Spacer(Modifier.height(18.dp))
                        LiveTypePrimaryButton(
                            text = s("grant"),
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Instructions Hint Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeMedium,
                backgroundColor = LiveTypeTokens.BgSurface,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = LiveTypeTokens.IndigoElectric,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = s("scanner_hint"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LiveTypeTokens.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Fallback: Manual Entry Button
            LiveTypeSecondaryButton(
                text = s("manual_entry"),
                icon = Icons.Default.Dialpad,
                onClick = { vm.goManualEntry() },
            )

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CameraQrView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    hasPermission: Boolean,
    onQr: (String) -> Unit,
) {
    val previewView = remember { PreviewView(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
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
                // Camera unavailable on emulator or busy: user can pair via manual entry
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
        modifier = Modifier.fillMaxSize(),
    )
}
