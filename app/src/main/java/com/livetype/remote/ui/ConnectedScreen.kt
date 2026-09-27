package com.livetype.remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectedScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    val view = LocalView.current

    // Keep the screen awake while streaming: dictation should not fight a sleeping phone.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCapsule(ui.conn, ui.streamingEnabled, ui.reconnectAttempt)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.setLanguage(if (ui.language == "ar") "en" else "ar") }) {
                Text(if (ui.language == "ar") "EN" else "ع")
            }
            TextButton(onClick = { vm.goDiagnostics() }) { Text(s("diagnostics")) }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = when (ui.conn) {
                ConnState.PAIRED -> if (ui.streamingEnabled) s("sub_connected") else s("sub_paused")
                ConnState.RECONNECTING -> s("sub_reconnecting")
                ConnState.CONNECTING -> s("sub_connecting")
                else -> s("status_disconnected")
            },
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))
        ErrorBanner(ui.lastError)

        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(s("laptop"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(ui.laptopAddress, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.text,
            onValueChange = { vm.onTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            placeholder = { Text(s("start_hint")) },
            shape = RoundedCornerShape(14.dp),
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.toggleStream() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    if (ui.streamingEnabled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    Modifier.height(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (ui.streamingEnabled) s("stop_stream") else s("resume_stream"))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.clearText() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Delete, contentDescription = null, Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s("clear_text"))
            }
            OutlinedButton(
                onClick = { vm.disconnect() },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Filled.LinkOff, contentDescription = null, Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s("disconnect"))
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                StatRow(s("latency"), ui.latencyMs?.let { "$it ms" } ?: s("no_measure"))
                StatRow(s("ops_sent"), ui.opsSent.toString())
                StatRow(s("revision"), ui.revision.toString())
                StatRow(
                    s("session_expires"),
                    if (ui.sessionExpiresAtMs > 0)
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ui.sessionExpiresAtMs))
                    else s("none"),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DiagnosticsScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        AppHeader(s("diagnostics"), onBack = { vm.backFromDiagnostics() })
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                StatRow("State", ui.conn.name)
                StatRow(s("laptop"), ui.laptopAddress.ifEmpty { s("none") })
                StatRow(s("latency"), ui.latencyMs?.let { "$it ms" } ?: s("no_measure"))
                StatRow(s("ops_sent"), ui.opsSent.toString())
                StatRow(s("revision"), ui.revision.toString())
                StatRow(s("last_error"), ui.lastError ?: s("none"))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(s("settings_title"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(s("language"), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row {
                    FilterChip(
                        selected = ui.language == "ar",
                        onClick = { vm.setLanguage("ar") },
                        label = { Text(s("arabic")) },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = ui.language == "en",
                        onClick = { vm.setLanguage("en") },
                        label = { Text(s("english")) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                SettingSwitch(s("vibrate_on_pair"), ui.vibrateOnPair) { vm.setVibrate(it) }
                SettingSwitch(s("remember_pairing"), ui.rememberPairingEnabled) { vm.setRememberPairing(it) }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { vm.forgetPairing() }, enabled = ui.savedPairingAvailable) {
                    Text(s("forget_pairing"))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        ErrorBanner(ui.lastError)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
