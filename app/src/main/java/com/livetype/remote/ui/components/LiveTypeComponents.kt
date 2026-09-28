package com.livetype.remote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.ui.errorText
import com.livetype.remote.ui.theme.LiveTypeTokens

/**
 * Confident primary action button with electric violet-indigo gradient and tactile feedback.
 */
@Composable
fun LiveTypePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.96f else 1.0f

    val backgroundModifier = if (enabled) {
        Modifier
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = LiveTypeTokens.IndigoElectric.copy(alpha = 0.5f),
                ambientColor = LiveTypeTokens.VioletNeon.copy(alpha = 0.3f),
            )
            .background(LiveTypeTokens.BrandGradient, shape = CircleShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.1f))
                ),
                shape = CircleShape,
            )
    } else {
        Modifier
            .background(LiveTypeTokens.BgCardElevated, shape = CircleShape)
            .border(width = 1.dp, color = LiveTypeTokens.BorderSubtle, shape = CircleShape)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(CircleShape)
            .then(backgroundModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else LiveTypeTokens.TextDisabled,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else LiveTypeTokens.TextDisabled,
                    fontSize = 15.5.sp,
                ),
            )
        }
    }
}

/**
 * Secondary dark surface pill button with luminous border.
 */
@Composable
fun LiveTypeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 50.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.96f else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(CircleShape)
            .background(LiveTypeTokens.BgCardElevated, shape = CircleShape)
            .border(
                width = 1.dp,
                color = if (enabled) LiveTypeTokens.BorderStrong else LiveTypeTokens.BorderSubtle,
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = LiveTypeTokens.VioletNeon),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) LiveTypeTokens.TextPrimary else LiveTypeTokens.TextDisabled,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) LiveTypeTokens.TextPrimary else LiveTypeTokens.TextDisabled,
                    fontSize = 14.5.sp,
                ),
            )
        }
    }
}

/**
 * Danger Action Button (Disconnect, clear) with crimson styling.
 */
@Composable
fun LiveTypeDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1.0f

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(CircleShape)
            .background(LiveTypeTokens.StateErrorContainer, shape = CircleShape)
            .border(width = 1.dp, color = LiveTypeTokens.StateError.copy(alpha = 0.4f), shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = LiveTypeTokens.StateError),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LiveTypeTokens.StateError,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = LiveTypeTokens.StateError,
                    fontSize = 13.5.sp,
                ),
            )
        }
    }
}

/**
 * Tactile Surface Card (Solid dark block, generous 24dp radius, hairline border).
 * strictly avoids "Glass Sandwich" (solid surface, not glass on glass).
 */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiveTypeTokens.CardShapeLarge,
    backgroundColor: Color = LiveTypeTokens.BgCard,
    borderColor: Color = LiveTypeTokens.BorderSubtle,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        content()
    }
}

/**
 * Metric Stat Row for the telemetry dashboard (latency, ops, revisions, session expiry).
 */
@Composable
fun MetricStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LiveTypeTokens.TextPrimary,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LiveTypeTokens.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LiveTypeTokens.TextSecondary,
                fontSize = 13.sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 13.5.sp,
            ),
        )
    }
}

/**
 * Polished Text Field with 18dp rounded corners, dark container, glowing focus border.
 */
@Composable
fun LiveTypeInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        singleLine = singleLine,
        shape = LiveTypeTokens.InputShape,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = LiveTypeTokens.TextSecondary, modifier = Modifier.size(20.dp)) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = LiveTypeTokens.TextPrimary,
            unfocusedTextColor = LiveTypeTokens.TextPrimary,
            focusedContainerColor = LiveTypeTokens.BgCardElevated,
            unfocusedContainerColor = LiveTypeTokens.BgCard,
            focusedBorderColor = LiveTypeTokens.IndigoElectric,
            unfocusedBorderColor = LiveTypeTokens.BorderSubtle,
            focusedLabelColor = LiveTypeTokens.IndigoElectric,
            unfocusedLabelColor = LiveTypeTokens.TextSecondary,
            cursorColor = LiveTypeTokens.VioletNeon,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Liquid Error Banner with crimson glow and friendly message.
 */
@Composable
fun LiquidErrorBanner(
    message: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        if (message.isNullOrBlank()) return@AnimatedVisibility

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(LiveTypeTokens.CardShapeMedium)
                .background(LiveTypeTokens.StateErrorContainer)
                .border(
                    width = 1.dp,
                    color = LiveTypeTokens.StateError.copy(alpha = 0.45f),
                    shape = LiveTypeTokens.CardShapeMedium,
                )
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = LiveTypeTokens.StateError,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = errorText(message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFFD2D9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
