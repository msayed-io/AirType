package com.livetype.remote.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.livetype.remote.core.network.ConnState

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

/** Status dot; pulses while [pulsing] (healthy connection). */
@Composable
fun PulsingDot(color: Color, pulsing: Boolean, size: Int = 10) {
    val alpha = if (pulsing) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Reverse),
            label = "alpha",
        )
        a
    } else 1f
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun statusColor(conn: ConnState, streaming: Boolean): Color = when (conn) {
    ConnState.PAIRED -> if (streaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    ConnState.CONNECTING, ConnState.RECONNECTING -> Color(0xFFF59E0B)
    ConnState.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun StatusCapsule(conn: ConnState, streamingEnabled: Boolean, reconnectAttempt: Int) {
    val label = when (conn) {
        ConnState.PAIRED -> if (streamingEnabled) s("status_connected") else s("status_paused")
        ConnState.CONNECTING -> s("status_connecting")
        ConnState.RECONNECTING -> s("status_reconnecting") + if (reconnectAttempt > 0) " ($reconnectAttempt)" else ""
        ConnState.FAILED -> s("status_error")
        else -> s("status_disconnected")
    }
    val pulsing = conn == ConnState.PAIRED && streamingEnabled
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = statusColor(conn, streamingEnabled).copy(alpha = 0.12f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            PulsingDot(statusColor(conn, streamingEnabled), pulsing)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = statusColor(conn, streamingEnabled))
        }
    }
}

@Composable
fun ErrorBanner(message: String?) {
    if (message.isNullOrBlank()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = errorText(message),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp),
        )
    }
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
    return if (key != null) s(key) else s("err_generic")
}

@Composable
fun AppRoot(vm: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    // Language + direction
    val rtl = ui.language != "en"
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStrings provides Strings.forLang(ui.language),
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (rtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (ui.screen) {
                Screen.SCANNER -> QrScannerScreen(vm)
                Screen.MANUAL_ENTRY -> ManualEntryScreen(vm)
                Screen.CONNECTED -> ConnectedScreen(vm)
                Screen.DIAGNOSTICS -> DiagnosticsScreen(vm)
                else -> WelcomeScreen(vm)
            }
        }
    }

    // One-shot vibration on successful pairing
    val justPaired by vm.pairSucceeded.collectAsState()
    androidx.compose.runtime.LaunchedEffect(justPaired) {
        if (justPaired) {
            vibrateClick(context)
            vm.consumePairSucceeded()
        }
    }
}

@Composable
fun AppHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(s("back")) }
            Spacer(Modifier.width(4.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(s("app_title"), fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(s("tagline"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(26.dp))

        Button(
            onClick = { vm.goScanner() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) { Text(s("pair_laptop"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

        if (ui.savedPairingAvailable) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.reconnectSaved() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text(s("try_saved")) }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.goManualEntry() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(s("manual_entry"))
        }
        TextButton(onClick = { vm.goDiagnostics() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(s("diagnostics"))
        }

        Spacer(Modifier.height(22.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(s("welcome_how"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(s("welcome_step1"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(s("welcome_step2"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(s("welcome_step3"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            s("privacy_note"),
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun ManualEntryScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    var host by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var port by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("53017") }
    var sid by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var pin by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        AppHeader(s("manual_entry_title"), onBack = { vm.goHome() })
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(s("host")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
            label = { Text(s("port")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = sid,
            onValueChange = { sid = it.trim() },
            label = { Text(s("session_id")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text(s("pin")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        ErrorBanner(ui.lastError)

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.connectManual(host, port.toIntOrNull() ?: 0, sid, pin) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) { Text(s("connect")) }
    }
}
