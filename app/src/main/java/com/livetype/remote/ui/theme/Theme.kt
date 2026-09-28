package com.livetype.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LiveTypeTokens.IndigoElectric,
    onPrimary = Color.White,
    primaryContainer = LiveTypeTokens.BgCardHighlight,
    onPrimaryContainer = Color.White,
    secondary = LiveTypeTokens.VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = LiveTypeTokens.BgCardElevated,
    onSecondaryContainer = LiveTypeTokens.TextPrimary,
    tertiary = LiveTypeTokens.CyanPulse,
    background = LiveTypeTokens.BgDeep,
    onBackground = LiveTypeTokens.TextPrimary,
    surface = LiveTypeTokens.BgCard,
    onSurface = LiveTypeTokens.TextPrimary,
    surfaceVariant = LiveTypeTokens.BgCardElevated,
    onSurfaceVariant = LiveTypeTokens.TextSecondary,
    outline = LiveTypeTokens.BorderSubtle,
    outlineVariant = LiveTypeTokens.BorderStrong,
    error = LiveTypeTokens.StateError,
    onError = Color.White,
    errorContainer = LiveTypeTokens.StateErrorContainer,
    onErrorContainer = Color(0xFFFFD2D9),
)

@Composable
fun LiveTypeTheme(
    darkTheme: Boolean = true, // LiveType Remote is crafted with deep obsidian & luminous glow as its signature identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = LiveTypeTypography,
        content = content,
    )
}
