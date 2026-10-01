package com.livetype.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AirTypeColorScheme = lightColorScheme(
    primary = LiveTypeTokens.SurfaceDark,
    onPrimary = LiveTypeTokens.TextOnDark,
    primaryContainer = LiveTypeTokens.BgCardHighlight,
    onPrimaryContainer = LiveTypeTokens.TextPrimary,
    secondary = LiveTypeTokens.AccentPeriwinkle,
    onSecondary = LiveTypeTokens.TextOnDark,
    secondaryContainer = LiveTypeTokens.AccentPeriwinkleBg,
    onSecondaryContainer = LiveTypeTokens.AccentPeriwinkleDeep,
    tertiary = LiveTypeTokens.SurfaceDarkMuted,
    onTertiary = LiveTypeTokens.TextOnDark,
    background = LiveTypeTokens.BgContainer,
    onBackground = LiveTypeTokens.TextPrimary,
    surface = LiveTypeTokens.BgSurfaceWhite,
    onSurface = LiveTypeTokens.TextPrimary,
    surfaceVariant = LiveTypeTokens.BgCardHighlight,
    onSurfaceVariant = LiveTypeTokens.TextSecondary,
    outline = LiveTypeTokens.BorderSubtle,
    outlineVariant = LiveTypeTokens.BorderStrong,
    error = LiveTypeTokens.StateError,
    onError = Color.White,
    errorContainer = LiveTypeTokens.StateErrorContainer,
    onErrorContainer = LiveTypeTokens.StateError,
)

@Composable
fun LiveTypeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AirTypeColorScheme,
        typography = LiveTypeTypography,
        content = content,
    )
}
