package com.livetype.remote.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.s
import com.livetype.remote.ui.theme.LiveTypeTokens

/**
 * Floating Liquid Glass Capsule container:
 * - Semi-translucent frosted glass backdrop
 * - Dual-layer specular border highlight (brighter top reflection, subtle bottom edge)
 * - Top-half specular gloss lens reflection
 * - Deep diffuse floating shadow
 */
@Composable
fun LiquidGlassCapsule(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LiveTypeTokens.GlassBackdrop,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                spotColor = LiveTypeTokens.GlassShadow,
                ambientColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .drawWithContent {
                drawContent()
                // Top half specular gloss lens reflection
                drawRect(
                    brush = LiveTypeTokens.GlassShimmerBrush,
                    size = Size(this.size.width, this.size.height * 0.55f),
                )
            }
            .border(
                width = 1.2.dp,
                brush = LiveTypeTokens.GlassSpecularBorderBrush,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Interactive Liquid Glass Icon Button with tactile spring feedback.
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LiveTypeTokens.TextPrimary,
    size: Dp = 46.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.92f else 1.0f

    LiquidGlassCapsule(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2, color = LiveTypeTokens.AccentPeriwinkle),
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Dark Graphite Circular Icon Button matching the reference top bar settings button.
 */
@Composable
fun DarkGraphiteIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.92f else 1.0f

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = LiveTypeTokens.ShadowDarkCta,
                ambientColor = Color.Black.copy(alpha = 0.2f),
            )
            .clip(CircleShape)
            .background(LiveTypeTokens.DarkCtaGradient)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent,
                        )
                    ),
                    size = Size(this.size.width, this.size.height * 0.5f),
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.3f),
                    )
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LiveTypeTokens.TextOnDark,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Clean Liquid Glass App Title Pill:
 * Simple, elegant glass capsule displaying the app name "AirType" with zero unnecessary clutter.
 */
@Composable
fun LiquidGlassTitlePill(
    title: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCapsule(
        modifier = modifier.height(44.dp),
        elevation = 6.dp,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = LiveTypeTokens.TextPrimary,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp,
            ),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

/**
 * Floating Liquid Glass Top Navigation Bar:
 * Strictly positions elements at opposite sides (Start edge and End edge) across all screens.
 */
@Composable
fun FloatingGlassTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingActions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null) {
            LiquidGlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = s("back"),
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.width(46.dp))
        }

        LiquidGlassTitlePill(title = title)

        if (trailingActions != null) {
            trailingActions()
        } else {
            Spacer(Modifier.width(46.dp))
        }
    }
}

/**
 * Breathing glowing status pulse dot.
 */
@Composable
fun LiquidStatusDot(
    color: Color,
    pulsing: Boolean,
    size: Dp = 9.dp,
) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        )
        a
    } else 1f

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
            .border(
                width = 1.2.dp,
                color = color.copy(alpha = if (pulsing) alpha * 0.9f else 0.6f),
                shape = CircleShape,
            ),
    )
}

/**
 * Top-tier floating status pill capsule for Connection State.
 */
@Composable
fun LiquidStatusPill(
    conn: ConnState,
    streamingEnabled: Boolean,
    reconnectAttempt: Int,
    modifier: Modifier = Modifier,
) {
    val (labelKey, stateColor, bgContainer) = when (conn) {
        ConnState.PAIRED -> if (streamingEnabled) {
            Triple("status_connected", LiveTypeTokens.StateSuccess, LiveTypeTokens.StateSuccessContainer)
        } else {
            Triple("status_paused", LiveTypeTokens.StatePaused, LiveTypeTokens.StatePausedContainer)
        }
        ConnState.CONNECTING -> Triple("status_connecting", LiveTypeTokens.StateWarning, LiveTypeTokens.StateWarningContainer)
        ConnState.RECONNECTING -> Triple("status_reconnecting", LiveTypeTokens.StateWarning, LiveTypeTokens.StateWarningContainer)
        ConnState.FAILED -> Triple("status_error", LiveTypeTokens.StateError, LiveTypeTokens.StateErrorContainer)
        else -> Triple("status_disconnected", LiveTypeTokens.TextSecondary, LiveTypeTokens.GlassBackdrop)
    }

    val label = s(labelKey) + if (conn == ConnState.RECONNECTING && reconnectAttempt > 0) " ($reconnectAttempt)" else ""
    val isPulsing = (conn == ConnState.PAIRED && streamingEnabled) || conn == ConnState.CONNECTING || conn == ConnState.RECONNECTING

    LiquidGlassCapsule(
        modifier = modifier.height(42.dp),
        backgroundColor = bgContainer,
        elevation = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            LiquidStatusDot(color = stateColor, pulsing = isPulsing, size = 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = stateColor,
                    fontSize = 13.sp,
                ),
            )
        }
    }
}
