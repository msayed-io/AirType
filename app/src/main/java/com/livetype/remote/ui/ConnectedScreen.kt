package com.livetype.remote.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.session.AirTypeSession
import com.livetype.remote.service.AirTypeSessionService
import com.livetype.remote.ui.overlay.OverlayPermission
import com.livetype.remote.ui.components.DarkGraphiteIconButton
import com.livetype.remote.ui.components.LiquidErrorBanner
import com.livetype.remote.ui.components.LiquidGlassIconButton
import com.livetype.remote.ui.components.LiquidGlassTitlePill
import com.livetype.remote.ui.components.LiquidStatusDot
import com.livetype.remote.ui.components.LiquidStatusPill
import com.livetype.remote.ui.components.LiveTypeDangerButton
import com.livetype.remote.ui.components.LiveTypePrimaryButton
import com.livetype.remote.ui.components.LiveTypeSecondaryButton
import com.livetype.remote.ui.components.MetricStatTile
import com.livetype.remote.ui.components.TactileCard
import com.livetype.remote.ui.components.BubbleSizeSlider
import com.livetype.remote.ui.theme.LiveTypeTokens

/** Professional bubble colours drawn from the existing AirType palette. */
private val BUBBLE_COLORS = listOf(
    0xFF5F7ABF.toInt(), // periwinkle deep (default)
    0xFF1C1E22.toInt(), // graphite
    0xFF7C93D6.toInt(), // periwinkle light
    0xFF10B981.toInt(), // success green
    0xFFF59E0B.toInt(), // amber
)

/** A session the user started and did not cut yet (it may be reconnecting). */
private val ConnState.isLive: Boolean
    get() = this == ConnState.PAIRED || this == ConnState.CONNECTING || this == ConnState.RECONNECTING

/** Unwraps the Compose context down to the hosting Activity (for moveTaskToBack). */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun ConnectedScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    val view = LocalView.current
    var isLaptopCardVisible by remember { mutableStateOf(false) }

    // ---- floating mode (bubble + capsule) -----------------------------------
    val context = LocalContext.current
    val activity = context.findActivity()
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var awaitingOverlayPermission by remember { mutableStateOf(false) }
    var floatingMessage by remember { mutableStateOf<String?>(null) }
    // Read the localization map here (composable context) so the helpers below stay plain
    // functions that the lifecycle observer can also call.
    val strings = LocalStrings.current

    fun enterFloatingMode() {
        // Read the process-wide snapshot, not a captured composition value, so the
        // on-resume re-check after the system permission screen sees live state.
        if (!AirTypeSession.snapshot.value.hasLiveSession) {
            floatingMessage = strings["floating_needs_session"]
            return
        }
        if (!OverlayPermission.canDraw(context)) {
            showOverlayPermissionDialog = true
            return
        }
        AirTypeSessionService.showOverlay(context)
        floatingMessage = null
        // Put AirType itself in the background, exactly like pressing Home. The session
        // lives in the foreground service, so this never touches the connection.
        activity?.moveTaskToBack(true)
    }

    // Android 13+ needs a runtime permission before the session notice can be shown.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Re-check the overlay permission when the user comes back from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingOverlayPermission) {
                awaitingOverlayPermission = false
                if (OverlayPermission.canDraw(context)) {
                    enterFloatingMode()
                } else {
                    floatingMessage = strings["floating_permission_needed"]
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

                // Animated Collapsible Laptop Connection Card (Rich Periwinkle Gradient matching Home Identity)
                AnimatedVisibility(
                    visible = isLaptopCardVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
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
                }

                LiquidErrorBanner(ui.lastError)
                if (!ui.lastError.isNullOrBlank()) Spacer(Modifier.height(10.dp))
                LiquidErrorBanner(floatingMessage)
                if (!floatingMessage.isNullOrBlank()) Spacer(Modifier.height(10.dp))

                // Main Dictation & Live Typing Pad Card
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LiveTypeTokens.CardShapeLarge,
                    backgroundColor = LiveTypeTokens.BgSurfaceWhite,
                    borderColor = if (ui.streamingEnabled) LiveTypeTokens.AccentPeriwinkle else LiveTypeTokens.BorderSubtle,
                    elevation = if (ui.streamingEnabled) 6.dp else 3.dp,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
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

                            // Small, elegant clear text icon button strictly inside the input box in the top-left corner
                            if (ui.text.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(AbsoluteAlignment.TopLeft)
                                        .padding(10.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .border(1.dp, LiveTypeTokens.BorderSubtle, CircleShape)
                                        .clickable { vm.clearText() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = s("clear_text"),
                                        tint = LiveTypeTokens.StateError,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

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

                // Single, elegant full-width Disconnect button
                LiveTypeDangerButton(
                    text = s("disconnect"),
                    icon = Icons.Filled.LinkOff,
                    onClick = { vm.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                )

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
                            LiveTypeTokens.BgContainer.copy(alpha = 0.98f),
                            LiveTypeTokens.BgContainer.copy(alpha = 0.85f),
                            Color.Transparent,
                        )
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
                // Interactive Live Status Capsule (Click to toggle Laptop info dropdown)
                LiquidStatusPill(
                    conn = ui.conn,
                    streamingEnabled = ui.streamingEnabled,
                    reconnectAttempt = ui.reconnectAttempt,
                    modifier = Modifier.clickable { isLaptopCardVisible = !isLaptopCardVisible }
                )

                // Floating mode + Settings capsules on the opposite edge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DarkGraphiteIconButton(
                        icon = Icons.Default.PictureInPictureAlt,
                        contentDescription = s("floating_mode"),
                        onClick = { enterFloatingMode() },
                        size = 46.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    DarkGraphiteIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = s("settings_title"),
                        onClick = { vm.goSettings() },
                        size = 46.dp,
                    )
                }
            }
        }
    }
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text(s("floating_permission_title")) },
            text = { Text(s("floating_permission_text")) },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog = false
                    awaitingOverlayPermission = true
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { context.startActivity(intent) }
                }) { Text(s("open_settings")) }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) { Text(s("cancel")) }
            },
        )
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()

    BackHandler { vm.backFromSettings() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 80.dp), // Clears the floating top navigation capsules
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
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

                        Spacer(Modifier.height(18.dp))

                        // ---- floating bubble appearance -----------------------
                        Text(
                            text = s("floating_bubble_size"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BubbleSizeSlider(
                                value = ui.bubbleSizeDp,
                                valueRange = 44..66,
                                onValueChange = { vm.setBubbleSize(it) },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            // Live preview of the bubble at the chosen size and colour.
                            Box(
                                modifier = Modifier
                                    .size(ui.bubbleSizeDp.dp)
                                    .clip(CircleShape)
                                    .background(Color(ui.bubbleColor)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size((ui.bubbleSizeDp * 0.42f).dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = s("floating_bubble_color"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LiveTypeTokens.TextSecondary,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            BUBBLE_COLORS.forEach { color ->
                                val selected = ui.bubbleColor == color
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) LiveTypeTokens.TextPrimary else LiveTypeTokens.BorderSubtle,
                                            shape = CircleShape,
                                        )
                                        .clickable { vm.setBubbleColor(color) },
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        LiveTypeSecondaryButton(
                            text = s("floating_stop"),
                            onClick = { vm.stopFloatingMode() },
                            height = 44.dp,
                        )

                        Spacer(Modifier.height(10.dp))

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
                            LiveTypeTokens.BgContainer.copy(alpha = 0.98f),
                            LiveTypeTokens.BgContainer.copy(alpha = 0.85f),
                            Color.Transparent,
                        )
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
                    onClick = { vm.backFromSettings() },
                )

                LiquidGlassTitlePill(title = s("settings_title"))
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
