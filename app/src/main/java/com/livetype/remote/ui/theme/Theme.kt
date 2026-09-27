package com.livetype.remote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Indigo = Color(0xFF4F46E5)
private val IndigoDark = Color(0xFF818CF8)
private val BgLight = Color(0xFFF4F6FB)
private val BgDark = Color(0xFF12141C)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9),
    background = BgLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFEDEFF7),
    onSurface = Color(0xFF1E2433),
    onSurfaceVariant = Color(0xFF667085),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF10123A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),
    background = BgDark,
    surface = Color(0xFF1B1E29),
    surfaceVariant = Color(0xFF262A38),
    onSurface = Color(0xFFE5E7F0),
    onSurfaceVariant = Color(0xFF9AA1B5),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

@Composable
fun LiveTypeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
