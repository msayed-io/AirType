package com.livetype.remote.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.components.FloatingGlassTopBar
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiquidGlassCapsule
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
fun PulsingDot(color: Color, pulsing: Boolean, size: Int = 10) {
    LiquidStatusDot(color = color, pulsing = pulsing, size = size.dp)
}

@Composable
fun statusColor(conn: ConnState, streaming: Boolean): Color = when (conn) {
    ConnState.PAIRED -> if (streaming) LiveTypeTokens.StatePaired else LiveTypeTokens.StatePaused
    ConnState.CONNECTING, ConnState.RECONNECTING -> LiveTypeTokens.StateConnecting
    ConnState.FAILED -> LiveTypeTokens.StateError
    else -> LiveTypeTokens.TextTertiary
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
            color = LiveTypeTokens.BgDeep,
        ) {
            Box(Modifier.fillMaxSize()) {
                // Ambient Aurora Glow at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(LiveTypeTokens.TopAuroraGradient),
                )

                when (ui.screen) {
                    Screen.SCANNER -> QrScannerScreen(vm)
                    Screen.MANUAL_ENTRY -> ManualEntryScreen(vm)
                    Screen.CONNECTED -> ConnectedScreen(vm)
                    Screen.DIAGNOSTICS -> DiagnosticsScreen(vm)
                    else -> WelcomeScreen(vm)
                }
            }
        }
    }

    // One-shot vibration on successful pairing
    val justPaired by vm.pairSucceeded.collectAsState()
    LaunchedEffect(justPaired) {
        if (justPaired) {
            vibrateClick(context)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Floating Top Capsule Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // App badge pill
            LiquidGlassCapsule(modifier = Modifier.height(40.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = LiveTypeTokens.IndigoElectric,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "LAN Direct",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            // Quick actions capsules: Language & Diagnostics
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassCapsule(
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { vm.setLanguage(if (ui.language == "ar") "en" else "ar") },
                ) {
                    Text(
                        text = if (ui.language == "ar") "English" else "العربية",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = LiveTypeTokens.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }

                LiquidGlassCapsule(
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { vm.goDiagnostics() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = s("diagnostics"),
                        tint = LiveTypeTokens.TextSecondary,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Hero Beacon & Brand Card
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            shape = LiveTypeTokens.CardShapeLarge,
            backgroundColor = LiveTypeTokens.BgCard,
            borderColor = LiveTypeTokens.BorderStrong,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Luminous Keyboard Icon Container
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(LiveTypeTokens.VioletNeon, LiveTypeTokens.IndigoElectric)
                            )
                        )
                        .border(width = 2.dp, color = Color.White.copy(alpha = 0.35f), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = s("app_title"),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = s("tagline"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LiveTypeTokens.TextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Primary Action: Pair Laptop via QR
        LiveTypePrimaryButton(
            text = s("pair_laptop"),
            icon = Icons.Default.QrCodeScanner,
            onClick = { vm.goScanner() },
        )

        // Saved Pairing Reconnection Option
        if (ui.savedPairingAvailable) {
            Spacer(Modifier.height(12.dp))
            LiveTypeSecondaryButton(
                text = s("try_saved"),
                icon = Icons.Default.History,
                onClick = { vm.reconnectSaved() },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Manual Entry Pill Option
        LiveTypeSecondaryButton(
            text = s("manual_entry"),
            icon = Icons.Default.Dialpad,
            onClick = { vm.goManualEntry() },
        )

        Spacer(Modifier.height(24.dp))

        // How-it-Works Card with Step Pills
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            shape = LiveTypeTokens.CardShapeLarge,
            backgroundColor = LiveTypeTokens.BgCard,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cable,
                        contentDescription = null,
                        tint = LiveTypeTokens.IndigoElectric,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = s("welcome_how"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(Modifier.height(16.dp))

                WelcomeStepItem(number = "1", text = s("welcome_step1"))
                Spacer(Modifier.height(12.dp))
                WelcomeStepItem(number = "2", text = s("welcome_step2"))
                Spacer(Modifier.height(12.dp))
                WelcomeStepItem(number = "3", text = s("welcome_step3"))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Privacy Reassurance Card
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(LiveTypeTokens.StatePairedContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = LiveTypeTokens.StatePaired,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = s("privacy_note"),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LiveTypeTokens.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun WelcomeStepItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LiveTypeTokens.BgCardHighlight)
                .border(width = 1.dp, color = LiveTypeTokens.BorderStrong, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = LiveTypeTokens.IndigoElectric,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LiveTypeTokens.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ManualEntryScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("53017") }
    var sid by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FloatingGlassTopBar(
            title = s("manual_entry_title"),
            onBack = { vm.goHome() },
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeLarge,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = s("manual_entry_title"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
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
            Spacer(Modifier.height(16.dp))

            LiveTypePrimaryButton(
                text = s("connect"),
                icon = Icons.Default.Wifi,
                enabled = host.isNotBlank() && port.isNotBlank() && sid.isNotBlank() && pin.isNotBlank(),
                onClick = { vm.connectManual(host, port.toIntOrNull() ?: 0, sid, pin) },
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}
