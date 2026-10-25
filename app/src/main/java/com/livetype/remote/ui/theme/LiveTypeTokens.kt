package com.livetype.remote.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Design Tokens for AirType.
 * Meticulously derived from the Reference Design DNA:
 * - Tactile airy neutral canvas (#E8E9ED / #F2F3F6)
 * - Ultra-refined Liquid Glass floating capsules with specular lens sheen and frosted depth
 * - Pure white tactile surface cards (#FFFFFF) with subtle diffuse depth
 * - Hero signature periwinkle gradient (#8EA6DB -> #6E88CE)
 * - Graphite charcoal primary anchor buttons (#1C1E22)
 * - Full capsule geometry and balanced bilingual typography
 */
object LiveTypeTokens {

    // --- Background & Canvas Surfaces ---
    val BgCanvas = Color(0xFFE8E9ED)
    val BgContainer = Color(0xFFF2F3F6)
    val BgSurfaceWhite = Color(0xFFFFFFFF)
    val BgCard = Color(0xFFFFFFFF)
    val BgCardElevated = Color(0xFFFFFFFF)
    val BgCardHighlight = Color(0xFFF4F5F8)
    val BgInput = Color(0xFFF6F7F9)

    // --- Liquid Glass Capsule Material ---
    val GlassBackdrop = Color(0xF5FFFFFF)
    val GlassBackdropPressed = Color(0xFFECEEF2)
    val GlassSpecularTop = Color(0xB3FFFFFF)
    val GlassSpecularBottom = Color(0x33CDD1D9)
    val GlassShadow = Color(0x1C182030)

    val GlassSpecularBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xE6FFFFFF),
            Color(0x59CDD1D9)
        )
    )

    val GlassShimmerBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.50f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        )
    )

    // --- Dark Anchors (Primary CTAs, Top Bar Action Button, Nav Anchors) ---
    val SurfaceDark = Color(0xFF1C1E22)
    val SurfaceDarkHover = Color(0xFF2A2D33)
    val SurfaceDarkMuted = Color(0xFF32363E)

    // --- Periwinkle Hero Card Gradients & Accents ---
    val AccentPeriwinkle = Color(0xFF7C93D6)
    val AccentPeriwinkleDeep = Color(0xFF5F7ABF)
    val AccentPeriwinkleLight = Color(0xFF8EA6DB)
    val AccentPeriwinkleBg = Color(0x267C93D6)

    val HeroPeriwinkleGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8EA6DB),
            Color(0xFF6E88CE),
        )
    )

    val HeroNodeContainerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x3DFFFFFF),
            Color(0x24FFFFFF),
        )
    )

    val DarkCtaGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF22252A),
            Color(0xFF181A1E),
        )
    )

    // --- Borders & Strokes ---
    val BorderSubtle = Color(0xFFE1E3E8)
    val BorderStrong = Color(0xFFCDD1D9)
    val BorderFocused = Color(0xFF7C93D6)
    val BorderWhiteGlass = Color(0x4DFFFFFF)
    val BorderHeroNode = Color(0x3BFFFFFF)

    // --- Floating Capsule & Glass Shadows ---
    val ShadowSubtle = Color(0x0D121928)
    val ShadowCard = Color(0x142D374B)
    val ShadowDarkCta = Color(0x3B1C1E22)
    val ShadowHero = Color(0x4A647EC4)

    // --- Semantic States ---
    val StateSuccess = Color(0xFF10B981)
    val StateSuccessContainer = Color(0xFFE6F4EA)
    val StateWarning = Color(0xFFF59E0B)
    val StateWarningContainer = Color(0xFFFEF7E0)
    val StateError = Color(0xFFEF4444)
    val StateErrorContainer = Color(0xFFFCE8E6)
    val StatePaused = Color(0xFF7A808C)
    val StatePausedContainer = Color(0xFFF1F3F4)

    // --- Text Hierarchy ---
    val TextPrimary = Color(0xFF181A1F)
    val TextSecondary = Color(0xFF7A808C)
    val TextTertiary = Color(0xFF9DA3AF)
    val TextDisabled = Color(0xFFB5BAC4)
    val TextOnDark = Color(0xFFFFFFFF)
    val TextOnDarkSecondary = Color(0xD9FFFFFF)
    val TextOnDarkMuted = Color(0xA6FFFFFF)

    // --- Shapes & Geometry ---
    val CapsuleShape = CircleShape
    val CardShapeLarge = RoundedCornerShape(28.dp)
    val CardShapeMedium = RoundedCornerShape(20.dp)
    val CardShapeSmall = RoundedCornerShape(14.dp)
    val InputShape = RoundedCornerShape(16.dp)

    // --- Spacing Scale ---
    val Space4: Dp = 4.dp
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space32: Dp = 32.dp

    // --- Motion Specs ---
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    const val MotionFast = 150
    const val MotionNormal = 260
    const val MotionSlow = 400
}
