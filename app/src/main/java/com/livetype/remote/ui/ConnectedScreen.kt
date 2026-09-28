package com.livetype.remote.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.components.FloatingGlassTopBar
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiquidGlassCapsule
import com.livetype.remote.ui.components.LiquidGlassIconButton
import com.livetype.remote.ui.components.LiquidStatusDot
import com.livetype.remote.ui.components.LiquidStatusPill
import com.livetype.remote.ui.components.LiveTypeDangerButton
import com.livetype.remote.ui.components.LiveTypePrimaryButton
import com.livetype.remote.ui.components.LiveTypeSecondaryButton
import com.livetype.remote.ui.components.MetricStatTile
import com.livetype.remote.ui.components.TactileCard
import com.livetype.remote.ui.theme.LiveTypeTokens
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
            .imePadding(),
    ) {
        // Floating Top Glass Navigation Capsule Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Live Status Capsule
            LiquidStatusPill(
                conn = ui.conn,
                streamingEnabled = ui.streamingEnabled,
                reconnectAttempt = ui.reconnectAttempt,
            )

            // Trailing Quick Capsules: Language Toggle & Diagnostics
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassCapsule(
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { vm.setLanguage(if (ui.language == "ar") "en" else "ar") },
                ) {
                    Text(
                        text = if (ui.language == "ar") "EN" else "عربي",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LiveTypeTokens.TextPrimary,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                LiquidGlassCapsule(
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { vm.goDiagnostics() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = s("diagnostics"),
                        tint = LiveTypeTokens.TextSecondary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(16.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Subtitle state explanation
            Text(
                text = when (ui.conn) {
                    ConnState.PAIRED -> if (ui.streamingEnabled) s("sub_connected") else s("sub_paused")
                    ConnState.RECONNECTING -> s("sub_reconnecting")
                    ConnState.CONNECTING -> s("sub_connecting")
                    else -> s("status_disconnected")
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LiveTypeTokens.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(Modifier.height(10.dp))
            LiquidErrorBanner(ui.lastError)
            Spacer(Modifier.height(10.dp))

            // Laptop Target Info Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeMedium,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LiveTypeTokens.BgCardHighlight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = null,
                            tint = LiveTypeTokens.IndigoElectric,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s("laptop"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextTertiary,
                                fontSize = 11.5.sp,
                            ),
                        )
                        Text(
                            text = ui.laptopAddress.ifEmpty { "127.0.0.1" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = LiveTypeTokens.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                            ),
                        )
                    }

                    // Live pulse indicator
                    LiquidStatusDot(
                        color = if (ui.conn == ConnState.PAIRED) LiveTypeTokens.StatePaired else LiveTypeTokens.StateConnecting,
                        pulsing = ui.conn == ConnState.PAIRED && ui.streamingEnabled,
                        size = 10.dp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Dictation & Live Typing Pad Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeLarge,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = if (ui.streamingEnabled) LiveTypeTokens.BorderGlow else LiveTypeTokens.BorderSubtle,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = ui.text,
                        onValueChange = { vm.onTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = {
                            Text(
                                s("start_hint"),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = LiveTypeTokens.TextTertiary,
                                    fontSize = 15.sp,
                                ),
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        shape = LiveTypeTokens.CardShapeMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LiveTypeTokens.BgDeep,
                            unfocusedContainerColor = LiveTypeTokens.BgDeep,
                            focusedBorderColor = LiveTypeTokens.IndigoElectric,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = LiveTypeTokens.VioletNeon,
                        ),
                    )

                    Spacer(Modifier.height(10.dp))

                    // Text stats pills: character count, word count, revision
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${ui.text.length} حرف · ${ui.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size} كلمة",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextTertiary,
                                fontSize = 11.5.sp,
                            ),
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(LiveTypeTokens.BgCardHighlight)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Rev: ${ui.revision}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LiveTypeTokens.IndigoElectric,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action Buttons: Stream Pause/Resume Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LiveTypePrimaryButton(
                    text = if (ui.streamingEnabled) s("stop_stream") else s("resume_stream"),
                    icon = if (ui.streamingEnabled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    onClick = { vm.toggleStream() },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Secondary Controls: Clear Text & Disconnect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LiveTypeSecondaryButton(
                    text = s("clear_text"),
                    icon = Icons.Filled.DeleteSweep,
                    onClick = { vm.clearText() },
                    modifier = Modifier.weight(1f),
                )

                LiveTypeDangerButton(
                    text = s("disconnect"),
                    icon = Icons.Filled.LinkOff,
                    onClick = { vm.disconnect() },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Telemetry & Metrics Dashboard Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeLarge,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "مؤشرات الاتصال الحية",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))

                    val latencyColor = when {
                        ui.latencyMs == null -> LiveTypeTokens.TextSecondary
                        ui.latencyMs!! < 30 -> LiveTypeTokens.StatePaired
                        ui.latencyMs!! < 100 -> LiveTypeTokens.StateConnecting
                        else -> LiveTypeTokens.StateError
                    }

                    MetricStatTile(
                        label = s("latency"),
                        value = ui.latencyMs?.let { "$it ms" } ?: s("no_measure"),
                        accentColor = latencyColor,
                        icon = Icons.Default.Speed,
                    )
                    MetricStatTile(
                        label = s("ops_sent"),
                        value = ui.opsSent.toString(),
                        icon = Icons.Default.Sensors,
                    )
                    MetricStatTile(
                        label = s("revision"),
                        value = ui.revision.toString(),
                    )
                    MetricStatTile(
                        label = s("session_expires"),
                        value = if (ui.sessionExpiresAtMs > 0) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ui.sessionExpiresAtMs))
                        } else {
                            s("none")
                        },
                        icon = Icons.Default.Timer,
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    MetricStatTile(label = label, value = value)
}

@Composable
fun DiagnosticsScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FloatingGlassTopBar(
            title = s("diagnostics"),
            onBack = { vm.backFromDiagnostics() },
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            // Live Status Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeLarge,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "تفاصيل الجلسة والشبكة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))

                    MetricStatTile(label = "الحالة", value = ui.conn.name)
                    MetricStatTile(label = s("laptop"), value = ui.laptopAddress.ifEmpty { s("none") })
                    MetricStatTile(label = s("latency"), value = ui.latencyMs?.let { "$it ms" } ?: s("no_measure"))
                    MetricStatTile(label = s("ops_sent"), value = ui.opsSent.toString())
                    MetricStatTile(label = s("revision"), value = ui.revision.toString())
                    MetricStatTile(label = s("last_error"), value = ui.lastError ?: s("none"))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Settings Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shape = LiveTypeTokens.CardShapeLarge,
                backgroundColor = LiveTypeTokens.BgCard,
                borderColor = LiveTypeTokens.BorderSubtle,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = s("settings_title"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LiveTypeTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = s("language"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = LiveTypeTokens.TextSecondary,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))

                    // Language Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(LiveTypeTokens.BgCardHighlight)
                            .padding(4.dp),
                    ) {
                        LanguageTabPill(
                            title = s("arabic"),
                            selected = ui.language == "ar",
                            onClick = { vm.setLanguage("ar") },
                            modifier = Modifier.weight(1f),
                        )
                        LanguageTabPill(
                            title = s("english"),
                            selected = ui.language == "en",
                            onClick = { vm.setLanguage("en") },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    SettingSwitchRow(
                        label = s("vibrate_on_pair"),
                        checked = ui.vibrateOnPair,
                        icon = Icons.Default.Vibration,
                        onChange = { vm.setVibrate(it) },
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingSwitchRow(
                        label = s("remember_pairing"),
                        checked = ui.rememberPairingEnabled,
                        onChange = { vm.setRememberPairing(it) },
                    )

                    Spacer(Modifier.height(14.dp))

                    LiveTypeSecondaryButton(
                        text = s("forget_pairing"),
                        enabled = ui.savedPairingAvailable,
                        onClick = { vm.forgetPairing() },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            LiquidErrorBanner(ui.lastError)
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun LanguageTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) LiveTypeTokens.IndigoElectric else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else LiveTypeTokens.TextSecondary,
            ),
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LiveTypeTokens.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LiveTypeTokens.TextPrimary,
                fontSize = 13.5.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LiveTypeTokens.IndigoElectric,
                uncheckedThumbColor = LiveTypeTokens.TextTertiary,
                uncheckedTrackColor = LiveTypeTokens.BgCardHighlight,
                uncheckedBorderColor = LiveTypeTokens.BorderSubtle,
            ),
        )
    }
}
