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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.errorText
import com.livetype.remote.ui.s
import com.livetype.remote.ui.theme.LiveTypeTokens

/**
 * Tactile Surface Card.
 * Pure white (#FFFFFF) physical module with 1dp border and soft ambient shadow.
 */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiveTypeTokens.CardShapeLarge,
    backgroundColor: Color = LiveTypeTokens.BgSurfaceWhite,
    borderColor: Color = LiveTypeTokens.BorderSubtle,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = LiveTypeTokens.ShadowCard,
                ambientColor = LiveTypeTokens.ShadowSubtle,
            )
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        content()
    }
}

/**
 * Primary CTA Button:
 * Solid graphite charcoal capsule (#1C1E22) with white text and icon.
 * Matches the reference's strongest accent button.
 */
@Composable
fun LiveTypePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.97f else 1.0f

    val backgroundModifier = if (enabled) {
        Modifier
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = LiveTypeTokens.ShadowDarkCta,
                ambientColor = Color.Black.copy(alpha = 0.2f),
            )
            .background(LiveTypeTokens.DarkCtaGradient, shape = CircleShape)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.15f),
                shape = CircleShape,
            )
    } else {
        Modifier
            .background(LiveTypeTokens.BgCardHighlight, shape = CircleShape)
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
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) LiveTypeTokens.TextOnDark else LiveTypeTokens.TextDisabled,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) LiveTypeTokens.TextOnDark else LiveTypeTokens.TextDisabled,
                    fontSize = 15.5.sp,
                ),
            )
        }
    }
}

/**
 * Secondary CTA Button / Card:
 * Tactile white module (#FFFFFF) with #E1E3E8 border and dark typography.
 */
@Composable
fun LiveTypeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.97f else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .shadow(
                elevation = 3.dp,
                shape = LiveTypeTokens.CardShapeMedium,
                spotColor = LiveTypeTokens.ShadowCard,
                ambientColor = LiveTypeTokens.ShadowSubtle,
            )
            .clip(LiveTypeTokens.CardShapeMedium)
            .background(if (enabled) LiveTypeTokens.BgSurfaceWhite else LiveTypeTokens.BgCardHighlight)
            .border(
                width = 1.dp,
                color = if (enabled) LiveTypeTokens.BorderSubtle else LiveTypeTokens.BorderSubtle.copy(alpha = 0.5f),
                shape = LiveTypeTokens.CardShapeMedium,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = LiveTypeTokens.AccentPeriwinkle.copy(alpha = 0.2f)),
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
                    tint = if (enabled) LiveTypeTokens.TextSecondary else LiveTypeTokens.TextDisabled,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) LiveTypeTokens.TextPrimary else LiveTypeTokens.TextDisabled,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

/**
 * Danger Action Button (Disconnect, clear) with crimson styling in soft container.
 */
@Composable
fun LiveTypeDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 50.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.97f else 1.0f

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(LiveTypeTokens.CardShapeMedium)
            .background(LiveTypeTokens.StateErrorContainer)
            .border(width = 1.dp, color = LiveTypeTokens.StateError.copy(alpha = 0.3f), shape = LiveTypeTokens.CardShapeMedium)
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
                Spacer(Modifier.width(8.dp))
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
 * Signature Hero Connection Card:
 * Transplanted from the periwinkle bank card in the reference image.
 * Houses the live connection status, node visualizer (Phone ↔ Laptop), Wi-Fi bridge, and security verify badge.
 */
@Composable
fun HeroConnectionCard(
    conn: ConnState,
    laptopAddress: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = LiveTypeTokens.CardShapeLarge,
                spotColor = LiveTypeTokens.ShadowHero,
                ambientColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(LiveTypeTokens.CardShapeLarge)
            .background(LiveTypeTokens.HeroPeriwinkleGradient)
            .border(width = 1.dp, color = LiveTypeTokens.BorderWhiteGlass, shape = LiveTypeTokens.CardShapeLarge),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Top Meta Bar inside Hero Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Status Badge Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.35f), shape = CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    LiquidStatusDot(
                        color = if (conn == ConnState.PAIRED) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                        pulsing = true,
                        size = 7.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (conn == ConnState.PAIRED) s("status_connected") else s("ready_to_pair"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                    )
                }

                Text(
                    text = "AIRTYPE LOCAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp,
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Main Status Title
            Text(
                text = if (conn == ConnState.PAIRED) s("status_connected") else s("status_disconnected"),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (conn == ConnState.PAIRED) s("sub_connected") else "بانتظار الاقتران بجهاز الكمبيوتر المكتبي",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Inner Translucent Bridge Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LiveTypeTokens.CardShapeMedium)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(width = 1.dp, color = LiveTypeTokens.BorderHeroNode, shape = LiveTypeTokens.CardShapeMedium)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Phone Node
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(76.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = LiveTypeTokens.SurfaceDark,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "الهاتف",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = s("active_now"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                ),
                            )
                        }
                    }

                    // Middle Wi-Fi Transmission Beam
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    ) {
                        // Wi-Fi Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.35f), shape = CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Wi-Fi محلي",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Visual Ping Dots Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)))
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                        }
                    }

                    // Laptop Node
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(76.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = null,
                                tint = LiveTypeTokens.SurfaceDark,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = s("laptop"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = laptopAddress.ifEmpty { "192.168.1.X" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 9.5.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Verified note row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFA7F3D0),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = s("pc_open_tip"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
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
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LiveTypeTokens.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LiveTypeTokens.TextSecondary,
                fontSize = 13.5.sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 14.sp,
            ),
        )
    }
}

/**
 * Polished Text Field matching the clean reference design.
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
            focusedContainerColor = LiveTypeTokens.BgSurfaceWhite,
            unfocusedContainerColor = LiveTypeTokens.BgInput,
            focusedBorderColor = LiveTypeTokens.AccentPeriwinkle,
            unfocusedBorderColor = LiveTypeTokens.BorderSubtle,
            focusedLabelColor = LiveTypeTokens.AccentPeriwinkleDeep,
            unfocusedLabelColor = LiveTypeTokens.TextSecondary,
            cursorColor = LiveTypeTokens.AccentPeriwinkleDeep,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Error Banner with clear localized message.
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
                    color = LiveTypeTokens.StateError.copy(alpha = 0.35f),
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
                        color = LiveTypeTokens.StateError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
