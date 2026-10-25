package com.livetype.remote.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.discovery.DiscoveredDevice
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.components.DarkGraphiteIconButton
import com.livetype.remote.ui.components.FloatingGlassTopBar
import com.livetype.remote.ui.components.HeroConnectionCard
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiquidGlassCapsule
import com.livetype.remote.ui.components.LiquidGlassIconButton
import com.livetype.remote.ui.components.LiquidGlassTitlePill
import com.livetype.remote.ui.components.LiquidStatusDot
import com.livetype.remote.ui.components.LiquidStatusPill
import com.livetype.remote.ui.components.LiveTypeInput
import com.livetype.remote.ui.components.LiveTypePrimaryButton
import com.livetype.remote.ui.components.LiveTypeSecondaryButton
import com.livetype.remote.ui.components.TactileCard
import com.livetype.remote.ui.theme.LiveTypeTokens

fun vibrateClick(context: Context) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator?.hasVibrator() != true) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else {
        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Status dot delegation for compatibility */
@Composable
fun PulsingDot(color: Color, pulsing: Boolean, size: Int = 9) {
    LiquidStatusDot(color = color, pulsing = pulsing, size = size.dp)
}

@Composable
fun statusColor(conn: ConnState, streaming: Boolean): Color = when (conn) {
    ConnState.PAIRED -> if (streaming) LiveTypeTokens.StateSuccess else LiveTypeTokens.StatePaused
    ConnState.CONNECTING, ConnState.RECONNECTING -> LiveTypeTokens.StateWarning
    ConnState.FAILED -> LiveTypeTokens.StateError
    else -> LiveTypeTokens.TextSecondary
}

@Composable
fun StatusCapsule(conn: ConnState, streamingEnabled: Boolean, reconnectAttempt: Int) {
    LiquidStatusPill(conn = conn, streamingEnabled = streamingEnabled, reconnectAttempt = reconnectAttempt)
}

@Composable
fun ErrorBanner(message: String?) {
    LiquidErrorBanner(message = message)
}

/** Maps protocol/QR error codes to friendly localized messages. */
@Composable
fun errorText(codeOrMessage: String): String {
    val direct = mapOf(
        "session_expired" to "err_session_expired",
        "bad_pin" to "err_bad_pin",
        "unknown_session" to "err_unknown_session",
        "already_paired" to "err_already_paired",
        "bad_token" to "err_bad_token",
        "unexpected_message" to "err_unexpected_message",
        "malformed_message" to "err_malformed_message",
        "message_too_large" to "err_malformed_message",
        "server_closing" to "err_server_closing",
        "unsupported_version" to "err_unsupported_version",
        "UNSUPPORTED_VERSION" to "err_unsupported_version",
        "camera_denied" to "err_camera_denied",
        "NOT_JSON" to "err_NOT_JSON",
        "WRONG_APP" to "err_WRONG_APP",
        "EXPIRED" to "err_EXPIRED",
        "BAD_PIN" to "err_BAD_PIN",
        "BAD_ADDRESS" to "err_BAD_ADDRESS",
        "MISSING_FIELDS" to "err_MISSING_FIELDS",
    )
    val key = direct[codeOrMessage] ?: if (codeOrMessage.startsWith("err_")) codeOrMessage else null
    return if (key != null) s(key) else codeOrMessage
}

@Composable
fun AppRoot(vm: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    // Language + direction
    val rtl = ui.language != "en"
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStrings provides Strings.forLang(ui.language),
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LiveTypeTokens.BgContainer,
        ) {
            Box(Modifier.fillMaxSize()) {
                when (ui.screen) {
                    Screen.SCANNER -> QrScannerScreen(vm)
                    Screen.MANUAL_ENTRY -> ManualEntryScreen(vm)
                    Screen.CONNECTED -> ConnectedScreen(vm)
                    Screen.DIAGNOSTICS -> DiagnosticsScreen(vm)
                    else -> WelcomeScreen(vm)
                }

                ui.selectedDeviceForPin?.let { device ->
                    QuickPairPinDialog(
                        device = device,
                        onDismiss = { vm.dismissPinDialog() },
                        onConnect = { pin -> vm.connectDiscoveredWithPin(pin) }
                    )
                }
            }
        }
    }

    // One-shot vibration on successful pairing
    val justPaired by vm.pairSucceeded.collectAsState()
    LaunchedEffect(justPaired) {
        if (justPaired) {
            if (ui.vibrateOnPair) vibrateClick(context)
            vm.consumePairSucceeded()
        }
    }
}

/** Floating top bar capsule helper */
@Composable
fun AppHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    FloatingGlassTopBar(
        title = title,
        onBack = onBack,
        trailingActions = trailing,
    )
}

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val scrollState = rememberScrollState()

        // 1. Scrollable Content Area, Vertically Centered for Visual Harmony
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeight)
                    .padding(
                        top = 80.dp, // Clears the floating top navigation capsules
                        bottom = 24.dp,
                        start = 18.dp,
                        end = 18.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Emblem & Typography Brand Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    spotColor = LiveTypeTokens.ShadowCard,
                                    ambientColor = LiveTypeTokens.ShadowSubtle,
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(LiveTypeTokens.BgSurfaceWhite)
                                .border(
                                    width = 1.dp,
                                    color = LiveTypeTokens.BorderSubtle,
                                    shape = RoundedCornerShape(18.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = LiveTypeTokens.SurfaceDark,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = s("app_title"),
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = LiveTypeTokens.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                lineHeight = 36.sp,
                            ),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = s("tagline"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Signature Periwinkle Hero Card (Transplanted from Reference Bank Card)
                    HeroConnectionCard(
                        conn = ui.conn,
                        laptopAddress = ui.laptopAddress,
                    )

                    Spacer(Modifier.height(14.dp))

                    // LAN Auto-Discovery Section (Live ZeroConf / mDNS / UDP Beacons)
                    DiscoveredDevicesSection(
                        devices = ui.discoveredDevices,
                        isScanning = ui.isScanningDevices,
                        isAutoConnecting = ui.isAutoConnecting,
                        isDeviceTrusted = { device -> vm.isDeviceTrusted(device) },
                        onDeviceClick = { device -> vm.onSelectDiscoveredDevice(device) },
                        onRefresh = { vm.refreshDiscovery() }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Primary Action Button (Graphite Charcoal Capsule #1C1E22)
                    LiveTypePrimaryButton(
                        text = s("pair_laptop"),
                        icon = Icons.Default.QrCodeScanner,
                        onClick = { vm.goScanner() },
                        height = 56.dp,
                    )

                    // Saved Pairing Reconnect Button (if available)
                    if (ui.savedPairingAvailable) {
                        Spacer(Modifier.height(10.dp))
                        LiveTypeSecondaryButton(
                            text = s("try_saved"),
                            icon = Icons.Default.History,
                            onClick = { vm.reconnectSaved() },
                            height = 50.dp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Secondary 2-Column Action Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LiveTypeSecondaryButton(
                            text = s("manual_entry"),
                            icon = Icons.Default.Dialpad,
                            onClick = { vm.goManualEntry() },
                            modifier = Modifier.weight(1f),
                            height = 50.dp,
                        )

                        LiveTypeSecondaryButton(
                            text = s("diagnostics"),
                            icon = Icons.Default.Router,
                            onClick = { vm.goDiagnostics() },
                            modifier = Modifier.weight(1f),
                            height = 50.dp,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Security Footer Capsule
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LiveTypeTokens.BgSurfaceWhite)
                            .border(width = 1.dp, color = LiveTypeTokens.BorderSubtle, shape = CircleShape)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = LiveTypeTokens.StateSuccess,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = s("end_to_end_secure"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }

        // Fixed Sticky Top Navigation Bar: Symmetrical Two-Sided Layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LiveTypeTokens.BgContainer.copy(alpha = 0.96f),
                            LiveTypeTokens.BgContainer.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 180f,
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // App Title Glass Capsule (Pristine, clean text with no icon)
                LiquidGlassTitlePill(title = s("app_title"))

                // Settings Button Capsule (Graphite circle #1C1E22 on opposite edge)
                DarkGraphiteIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = s("settings_title"),
                    onClick = { vm.goDiagnostics() },
                    size = 46.dp,
                )
            }
        }
    }
}

@Composable
fun ManualEntryScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("53017") }
    var sid by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    BackHandler { vm.goHome() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 80.dp), // Clears the floating top navigation capsules
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = LiveTypeTokens.BorderSubtle,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = s("manual_entry_title"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = LiveTypeTokens.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                ),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LiveTypeTokens.AccentPeriwinkleBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "IP & PIN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        LiveTypeInput(
                            value = host,
                            onValueChange = { host = it.trim() },
                            label = s("host"),
                            placeholder = "192.168.1.100",
                            leadingIcon = Icons.Default.Router,
                        )
                        Spacer(Modifier.height(14.dp))

                        LiveTypeInput(
                            value = port,
                            onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                            label = s("port"),
                            placeholder = "53017",
                            leadingIcon = Icons.Default.Numbers,
                        )
                        Spacer(Modifier.height(14.dp))

                        LiveTypeInput(
                            value = sid,
                            onValueChange = { sid = it.trim() },
                            label = s("session_id"),
                            placeholder = "c34d6bb9...",
                            leadingIcon = Icons.Default.VpnKey,
                        )
                        Spacer(Modifier.height(14.dp))

                        LiveTypeInput(
                            value = pin,
                            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                            label = s("pin"),
                            placeholder = "123456",
                            leadingIcon = Icons.Default.Pin,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                LiquidErrorBanner(ui.lastError)
                if (!ui.lastError.isNullOrBlank()) Spacer(Modifier.height(10.dp))

                LiveTypePrimaryButton(
                    text = s("connect"),
                    icon = Icons.Default.Wifi,
                    enabled = host.isNotBlank() && port.isNotBlank() && sid.isNotBlank() && pin.isNotBlank(),
                    onClick = { vm.connectManual(host, port.toIntOrNull() ?: 0, sid, pin) },
                )

                Spacer(Modifier.height(28.dp))
            }
        }

        // Pinned Top Glass Bar (Two-sided: Back on Start edge, Title on End edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LiveTypeTokens.BgContainer.copy(alpha = 0.96f),
                            LiveTypeTokens.BgContainer.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 180f,
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s("back"),
                    onClick = { vm.goHome() },
                )

                LiquidGlassTitlePill(title = s("manual_entry_title"))
            }
        }
    }
}

@Composable
fun DiscoveredDevicesSection(
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    isAutoConnecting: Boolean = false,
    isDeviceTrusted: (DiscoveredDevice) -> Boolean = { false },
    onDeviceClick: (DiscoveredDevice) -> Unit,
    onRefresh: () -> Unit,
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        shape = LiveTypeTokens.CardShapeLarge,
        backgroundColor = LiveTypeTokens.BgSurfaceWhite,
        borderColor = if (devices.isNotEmpty()) LiveTypeTokens.AccentPeriwinkle else LiveTypeTokens.BorderSubtle,
        elevation = if (devices.isNotEmpty()) 5.dp else 2.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LiveTypeTokens.AccentPeriwinkleBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = LiveTypeTokens.AccentPeriwinkleDeep,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s("discovered_devices_title"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = LiveTypeTokens.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                            ),
                        )
                        Text(
                            text = s("discovered_devices_subtitle"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 11.5.sp,
                            ),
                        )
                    }
                }

                // Rescan Action Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(LiveTypeTokens.BgCardHighlight)
                        .border(1.dp, LiveTypeTokens.BorderSubtle, CircleShape)
                        .clickable(onClick = onRefresh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = s("refresh_scan"),
                        tint = LiveTypeTokens.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Auto-connecting pulsing notification banner
            if (isAutoConnecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LiveTypeTokens.CardShapeSmall)
                        .background(LiveTypeTokens.AccentPeriwinkleBg)
                        .border(1.dp, LiveTypeTokens.AccentPeriwinkle.copy(alpha = 0.5f), LiveTypeTokens.CardShapeSmall)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidStatusDot(color = Color(0xFF10B981), pulsing = true, size = 8.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = s("auto_connecting_status"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LiveTypeTokens.AccentPeriwinkleDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (devices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { device ->
                        val trusted = isDeviceTrusted(device)
                        DiscoveredDeviceItem(
                            device = device,
                            isTrusted = trusted,
                            onClick = { onDeviceClick(device) }
                        )
                    }
                }
            } else {
                // Scanning / Idle Indicator Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LiveTypeTokens.CardShapeSmall)
                        .background(LiveTypeTokens.BgCardHighlight)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidStatusDot(
                        color = if (isScanning) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                        pulsing = isScanning,
                        size = 7.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isScanning) s("scanning_network") else s("no_devices_found"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LiveTypeTokens.TextSecondary,
                            fontSize = 12.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceItem(
    device: DiscoveredDevice,
    isTrusted: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LiveTypeTokens.CardShapeMedium)
            .background(
                if (isTrusted) LiveTypeTokens.HeroPeriwinkleGradient
                else Brush.linearGradient(listOf(Color(0xFF2E333D), Color(0xFF1E2128)))
            )
            .border(
                width = if (isTrusted) 1.2.dp else 1.dp,
                color = if (isTrusted) LiveTypeTokens.BorderWhiteGlass else LiveTypeTokens.BorderSubtle,
                shape = LiveTypeTokens.CardShapeMedium
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = null,
                        tint = LiveTypeTokens.SurfaceDark,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                        if (isTrusted) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = s("trusted_badge"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                    Text(
                        text = device.displayAddress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Quick Connect Action Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = LiveTypeTokens.SurfaceDark,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = s("quick_connect_btn"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = LiveTypeTokens.SurfaceDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                    ),
                )
            }
        }
    }
}

@Composable
fun QuickPairPinDialog(
    device: DiscoveredDevice,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = LiveTypeTokens.CardShapeLarge,
        containerColor = LiveTypeTokens.BgSurfaceWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LiveTypeTokens.AccentPeriwinkleBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = null,
                        tint = LiveTypeTokens.AccentPeriwinkleDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = s("enter_pin_dialog_title"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                    Text(
                        text = "${device.name} (${device.displayAddress})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LiveTypeTokens.TextSecondary,
                            fontSize = 11.5.sp,
                        ),
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = s("enter_pin_for"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LiveTypeTokens.TextSecondary,
                        fontSize = 13.sp,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                LiveTypeInput(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                    label = s("pin"),
                    placeholder = "123456",
                    leadingIcon = Icons.Default.Pin,
                )
            }
        },
        confirmButton = {
            LiveTypePrimaryButton(
                text = s("connect"),
                icon = Icons.Default.Wifi,
                enabled = pin.length >= 4,
                onClick = { onConnect(pin) },
                height = 46.dp,
            )
        },
        dismissButton = {
            LiveTypeSecondaryButton(
                text = s("cancel"),
                onClick = onDismiss,
                height = 46.dp,
            )
        },
    )
}

