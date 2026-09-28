package com.livetype.remote.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Design Tokens for LiveType Remote.
 * Adheres strictly to the dark depth, luminous accents, and liquid glass capsule language.
 */
object LiveTypeTokens {

    // --- Color Hierarchy ---
    val BgDeep = Color(0xFF090A10)
    val BgSurface = Color(0xFF11131B)
    val BgCard = Color(0xFF171A26)
    val BgCardElevated = Color(0xFF1E2232)
    val BgCardHighlight = Color(0xFF272B3E)

    // Borders & Hairlines
    val BorderSubtle = Color(0x33444C66)
    val BorderStrong = Color(0x66586386)
    val BorderGlow = Color(0x667C3AED)

    // Glass Mappings (Liquid Glass Navigation Capsules)
    val GlassBackdrop = Color(0xD9141724) // 85% opacity tinted glass
    val GlassBackdropPressed = Color(0xE6202438)
    val GlassSpecularTop = Color(0x40FFFFFF)      // 25% specular top border reflection
    val GlassSpecularBottom = Color(0x0FFFFFFF)   // 6% subtle bottom contour
    val GlassShadow = Color(0x80000000)

    // Brand Gradients & Accents
    val IndigoElectric = Color(0xFF6366F1)
    val VioletNeon = Color(0xFF8B5CF6)
    val PurpleLuminous = Color(0xFFA855F7)
    val CyanPulse = Color(0xFF06B6D4)

    val BrandGradient = Brush.horizontalGradient(
        colors = listOf(IndigoElectric, VioletNeon, PurpleLuminous)
    )

    val TopAuroraGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0x336366F1),
            Color(0x188B5CF6),
            Color.Transparent
        )
    )

    val GlassSpecularBorderBrush = Brush.verticalGradient(
        colors = listOf(GlassSpecularTop, GlassSpecularBottom)
    )

    // Semantic States
    val StatePaired = Color(0xFF10B981)          // Emerald green
    val StatePairedContainer = Color(0x2410B981)
    val StateConnecting = Color(0xFFF59E0B)      // Amber gold
    val StateConnectingContainer = Color(0x24F59E0B)
    val StatePaused = Color(0xFF818CF8)          // Calm lavender
    val StatePausedContainer = Color(0x24818CF8)
    val StateError = Color(0xFFF43F5E)           // Crimson rose
    val StateErrorContainer = Color(0x2CF43F5E)

    // Text Hierarchy
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA6ACBE)
    val TextTertiary = Color(0xFF697089)
    val TextDisabled = Color(0xFF4A4E61)

    // --- Shapes ---
    val CapsuleShape = CircleShape
    val CardShapeLarge = RoundedCornerShape(26.dp)
    val CardShapeMedium = RoundedCornerShape(20.dp)
    val CardShapeSmall = RoundedCornerShape(14.dp)
    val InputShape = RoundedCornerShape(18.dp)

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
    const val MotionFast = 180
    const val MotionNormal = 280
    const val MotionSlow = 450
}
