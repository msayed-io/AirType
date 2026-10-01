package com.livetype.remote.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.components.DarkGraphiteIconButton
import com.livetype.remote.ui.components.FloatingGlassTopBar
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiquidGlassCapsule
import com.livetype.remote.ui.components.LiquidGlassIconButton
import com.livetype.remote.ui.components.LiquidGlassTitlePill
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

    // Keep screen awake during live typing
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler { vm.disconnect() }

    Box(Modifier.fillMaxSize()) {
        // Scrollable Content Area
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 80.dp) // Generous clearance for the pinned top glass capsules
                .imePadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {

                // Hero Laptop Connection Card (Rich Periwinkle Gradient matching Home Identity)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = LiveTypeTokens.CardShapeLarge,
                            spotColor = LiveTypeTokens.ShadowHero,
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                        )
                        .clip(LiveTypeTokens.CardShapeLarge)
                        .background(LiveTypeTokens.HeroPeriwinkleGradient)
                        .border(width = 1.dp, color = LiveTypeTokens.BorderWhiteGlass, shape = LiveTypeTokens.CardShapeLarge)
                        .padding(18.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Target Device Info
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(4.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Laptop,
                                        contentDescription = null,
                                        tint = LiveTypeTokens.SurfaceDark,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = s("laptop"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    )
                                    Text(
                                        text = ui.laptopAddress.ifEmpty { "192.168.1.X" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                        ),
                                    )
                                }
                            }

                            // Live Active Badge with Green Dot
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.35f), shape = CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                LiquidStatusDot(
                                    color = if (ui.conn == ConnState.PAIRED) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                    pulsing = ui.conn == ConnState.PAIRED && ui.streamingEnabled,
                                    size = 7.dp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (ui.streamingEnabled) s("active_now") else s("status_paused"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    ),
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Stream State Description Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(LiveTypeTokens.CardShapeSmall)
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when (ui.conn) {
                                    ConnState.PAIRED -> if (ui.streamingEnabled) s("sub_connected") else s("sub_paused")
                                    ConnState.RECONNECTING -> s("sub_reconnecting")
                                    ConnState.CONNECTING -> s("sub_connecting")
                                    else -> s("status_disconnected")
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                LiquidErrorBanner(ui.lastError)
                if (!ui.lastError.isNullOrBlank()) Spacer(Modifier.height(10.dp))

                // Main Dictation & Live Typing Pad Card
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = if (ui.streamingEnabled) LiveTypeTokens.AccentPeriwinkle else LiveTypeTokens.BorderSubtle,
                    elevation = if (ui.streamingEnabled) 6.dp else 3.dp,
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
                                focusedContainerColor = LiveTypeTokens.BgInput,
                                unfocusedContainerColor = LiveTypeTokens.BgInput,
                                focusedBorderColor = LiveTypeTokens.AccentPeriwinkle,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = LiveTypeTokens.AccentPeriwinkleDeep,
                            ),
                        )

                        Spacer(Modifier.height(12.dp))

                        // Text Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${ui.text.length} حرف · ${ui.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size} كلمة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LiveTypeTokens.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )

                            // Periwinkle Revision Badge
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LiveTypeTokens.AccentPeriwinkleBg)
                                    .border(width = 0.8.dp, color = LiveTypeTokens.AccentPeriwinkle.copy(alpha = 0.4f), shape = CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "Rev: ${ui.revision}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Primary Stream Pause / Resume Toggle (Periwinkle Glow & Solid Graphite)
                LiveTypePrimaryButton(
                    text = if (ui.streamingEnabled) s("stop_stream") else s("resume_stream"),
                    icon = if (ui.streamingEnabled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    onClick = { vm.toggleStream() },
                    height = 56.dp,
                )

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
                        height = 50.dp,
                    )

                    LiveTypeDangerButton(
                        text = s("disconnect"),
                        icon = Icons.Filled.LinkOff,
                        onClick = { vm.disconnect() },
                        modifier = Modifier.weight(1f),
                        height = 50.dp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Telemetry & Metrics Dashboard Card (with Periwinkle Accent Headers)
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = LiveTypeTokens.BorderSubtle,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "إحصائيات الجلسة",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = LiveTypeTokens.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                ),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LiveTypeTokens.AccentPeriwinkleBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        val latencyColor = when {
                            ui.latencyMs == null -> LiveTypeTokens.TextSecondary
                            ui.latencyMs!! < 30 -> LiveTypeTokens.StateSuccess
                            ui.latencyMs!! < 100 -> LiveTypeTokens.StateWarning
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

        // Pinned Top Glass Navigation Bar: Symmetrical Two-Sided Layout
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
                // Live Status Capsule
                LiquidStatusPill(
                    conn = ui.conn,
                    streamingEnabled = ui.streamingEnabled,
                    reconnectAttempt = ui.reconnectAttempt,
                )

                // Settings Capsule on the opposite edge
                DarkGraphiteIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = s("diagnostics"),
                    onClick = { vm.goDiagnostics() },
                    size = 46.dp,
                )
            }
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

    BackHandler { vm.backFromDiagnostics() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 80.dp), // Clears the floating top navigation capsules
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                // Live Session Details Card
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = LiveTypeTokens.BorderSubtle,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "تفاصيل الجلسة والشبكة",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = LiveTypeTokens.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp,
                                ),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LiveTypeTokens.AccentPeriwinkleBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "DIAG",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
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
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = LiveTypeTokens.BorderSubtle,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = s("settings_title"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = LiveTypeTokens.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp,
                            ),
                        )
                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = s("language"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))

                        // Language Selector Tabs with Periwinkle Active Tab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(LiveTypeTokens.BgCardHighlight)
                                .border(width = 1.dp, color = LiveTypeTokens.BorderSubtle, shape = CircleShape)
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

                        Spacer(Modifier.height(8.dp))

                        SettingSwitchRow(
                            label = s("auto_connect_title"),
                            checked = ui.autoConnectEnabled,
                            icon = Icons.Default.Sensors,
                            onChange = { vm.setAutoConnect(it) },
                        )

                        Spacer(Modifier.height(16.dp))

                        LiveTypeSecondaryButton(
                            text = s("forget_pairing"),
                            enabled = ui.savedPairingAvailable,
                            onClick = { vm.forgetPairing() },
                            height = 48.dp,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                LiquidErrorBanner(ui.lastError)
                Spacer(Modifier.height(30.dp))
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
                    onClick = { vm.backFromDiagnostics() },
                )

                LiquidGlassTitlePill(title = s("diagnostics"))
            }
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
            .height(38.dp)
            .clip(CircleShape)
            .background(
                if (selected) LiveTypeTokens.HeroPeriwinkleGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else LiveTypeTokens.TextSecondary,
                fontSize = 13.sp,
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LiveTypeTokens.AccentPeriwinkleBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LiveTypeTokens.AccentPeriwinkleDeep,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LiveTypeTokens.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LiveTypeTokens.AccentPeriwinkleDeep,
                uncheckedThumbColor = LiveTypeTokens.TextTertiary,
                uncheckedTrackColor = LiveTypeTokens.BgCardHighlight,
                uncheckedBorderColor = LiveTypeTokens.BorderSubtle,
            ),
        )
    }
}
