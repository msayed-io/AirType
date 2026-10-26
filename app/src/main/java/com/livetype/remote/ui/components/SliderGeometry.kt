package com.livetype.remote.ui.components

import kotlin.math.roundToInt

/**
 * Geometry of the bubble-size slider, kept as plain maths so it can be unit tested.
 *
 * The axis is deliberately **not mirrored** in right-to-left layouts: the minimum always
 * sits at the physical left edge and the maximum at the physical right edge, and the fill
 * always grows away from the left. That is what makes the thumb follow the finger in the
 * same direction in both languages — the earlier version flipped the axis for Arabic and
 * therefore moved the value the opposite way to the finger.
 */
object SliderGeometry {

    /** Fraction (0..1) of [value] inside [min]..[max]. */
    fun fractionOf(value: Int, min: Int, max: Int): Float {
        val span = max - min
        if (span <= 0) return 0f
        return ((value - min).toFloat() / span).coerceIn(0f, 1f)
    }

    /** Centre of the thumb, in pixels, for a track of [widthPx] holding a [thumbPx] pill. */
    fun thumbCentrePx(widthPx: Float, thumbPx: Float, fraction: Float): Float {
        val travel = (widthPx - thumbPx).coerceAtLeast(0f)
        return thumbPx / 2f + fraction.coerceIn(0f, 1f) * travel
    }

    /** Fraction under a touch at [x]: the pill can travel only between its two end stops. */
    fun fractionAt(x: Float, widthPx: Float, thumbPx: Float): Float {
        val usable = (widthPx - thumbPx).coerceAtLeast(1f)
        return ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
    }

    /**
     * Value under a touch at [x]. Moving the finger right always raises the value and
     * moving it left always lowers it, whatever the interface language is.
     */
    fun valueAt(x: Float, widthPx: Float, thumbPx: Float, min: Int, max: Int): Int {
        val span = max - min
        if (span <= 0) return min
        return (min + (fractionAt(x, widthPx, thumbPx) * span).roundToInt()).coerceIn(min, max)
    }
}
