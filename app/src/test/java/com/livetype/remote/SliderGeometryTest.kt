package com.livetype.remote

import com.livetype.remote.ui.components.SliderGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The slider used to mirror its axis in Arabic layouts (RTL), so dragging the thumb to the
 * right lowered the value and the bubble shrank while the finger moved the other way.
 *
 * These tests lock the physical axis instead: the minimum is always at the left edge and the
 * maximum at the right edge, the thumb sits under the finger, and moving right always
 * increases the value.
 */
class SliderGeometryTest {

    private val width = 1000f
    private val thumb = 40f
    private val min = 44
    private val max = 66

    @Test
    fun `the left edge is the minimum and the right edge is the maximum`() {
        assertEquals(min, SliderGeometry.valueAt(0f, width, thumb, min, max))
        assertEquals(max, SliderGeometry.valueAt(width, width, thumb, min, max))
    }

    @Test
    fun `moving the finger right always increases the value`() {
        var previous = Int.MIN_VALUE
        var x = 0f
        while (x <= width) {
            val value = SliderGeometry.valueAt(x, width, thumb, min, max)
            assertTrue("value dropped from $previous to $value at x=$x", value >= previous)
            previous = value
            x += 20f
        }
        assertEquals(max, previous)
    }

    @Test
    fun `the thumb centre follows the fraction of the value, left to right`() {
        val atMin = SliderGeometry.thumbCentrePx(width, thumb, 0f)
        val atMax = SliderGeometry.thumbCentrePx(width, thumb, 1f)
        assertEquals(thumb / 2f, atMin, 0.01f)
        assertEquals(width - thumb / 2f, atMax, 0.01f)
        assertTrue(atMax > atMin)
    }

    @Test
    fun `touching under the middle of the thumb keeps the value it is showing`() {
        val fraction = SliderGeometry.fractionOf(55, min, max)
        val centre = SliderGeometry.thumbCentrePx(width, thumb, fraction)
        assertEquals(55, SliderGeometry.valueAt(centre, width, thumb, min, max))
    }

    @Test
    fun `touches outside the track clamp instead of overflowing`() {
        assertEquals(min, SliderGeometry.valueAt(-500f, width, thumb, min, max))
        assertEquals(max, SliderGeometry.valueAt(width + 500f, width, thumb, min, max))
        assertEquals(0f, SliderGeometry.fractionAt(-500f, width, thumb), 0.0001f)
        assertEquals(1f, SliderGeometry.fractionAt(width + 500f, width, thumb), 0.0001f)
    }

    @Test
    fun `the stored value maps back to the fraction that produced it`() {
        for (value in min..max) {
            val fraction = SliderGeometry.fractionOf(value, min, max)
            val centre = SliderGeometry.thumbCentrePx(width, thumb, fraction)
            assertEquals(value, SliderGeometry.valueAt(centre, width, thumb, min, max))
        }
    }
}
