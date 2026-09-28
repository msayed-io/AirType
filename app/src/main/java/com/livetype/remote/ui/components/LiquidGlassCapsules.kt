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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
 * Floating Liquid Glass Capsule container.
 * Simulates Apple's Liquid Glass floating pill design:
 * - Independent pill cluster floating above content
 * - Semi-translucent tinted backdrop
 * - Dual-layer specular border highlight (brighter top reflection, subtle bottom edge)
 * - Deep diffuse floating shadow
 */
@Composable
fun LiquidGlassCapsule(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LiveTypeTokens.GlassBackdrop,
    elevation: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                spotColor = LiveTypeTokens.GlassShadow,
                ambientColor = Color(0x336366F1),
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                brush = LiveTypeTokens.GlassSpecularBorderBrush,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Interactive Liquid Glass Button with tactile spring feedback.
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
                indication = ripple(bounded = true, radius = size / 2, color = LiveTypeTokens.IndigoElectric),
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
 * Floating Liquid Glass Top Navigation Bar:
 * - Detached independent capsules:
 *   1. Circular Back capsule (if onBack provided)
 *   2. Title pill capsule in center
 *   3. Action pill capsule on trailing side
 * - Floats strictly above content without taking full width background
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null) {
            LiquidGlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = s("back"),
                onClick = onBack,
            )
        }

        Spacer(Modifier.weight(1f))

        // Title Capsule and any trailing actions on the opposite side, matching the main page layout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiquidGlassCapsule(
                modifier = Modifier.height(44.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = LiveTypeTokens.TextPrimary,
                            fontSize = 14.5.sp,
                        ),
                    )
                }
            }

            if (trailingActions != null) {
                trailingActions()
            }
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
    size: Dp = 10.dp,
) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 0.3f,
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
                width = 1.5.dp,
                color = color.copy(alpha = if (pulsing) alpha * 0.8f else 0.5f),
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
            Triple("status_connected", LiveTypeTokens.StatePaired, LiveTypeTokens.StatePairedContainer)
        } else {
            Triple("status_paused", LiveTypeTokens.StatePaused, LiveTypeTokens.StatePausedContainer)
        }
        ConnState.CONNECTING -> Triple("status_connecting", LiveTypeTokens.StateConnecting, LiveTypeTokens.StateConnectingContainer)
        ConnState.RECONNECTING -> Triple("status_reconnecting", LiveTypeTokens.StateConnecting, LiveTypeTokens.StateConnectingContainer)
        ConnState.FAILED -> Triple("status_error", LiveTypeTokens.StateError, LiveTypeTokens.StateErrorContainer)
        else -> Triple("status_disconnected", LiveTypeTokens.TextTertiary, Color(0x1F6B7280))
    }

    val label = s(labelKey) + if (conn == ConnState.RECONNECTING && reconnectAttempt > 0) " ($reconnectAttempt)" else ""
    val isPulsing = conn == ConnState.PAIRED && streamingEnabled || conn == ConnState.CONNECTING || conn == ConnState.RECONNECTING

    LiquidGlassCapsule(
        modifier = modifier.height(38.dp),
        backgroundColor = bgContainer,
        elevation = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            LiquidStatusDot(color = stateColor, pulsing = isPulsing, size = 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = stateColor,
                    fontSize = 12.5.sp,
                ),
            )
        }
    }
}
