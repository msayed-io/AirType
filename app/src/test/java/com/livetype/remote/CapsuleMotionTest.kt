package com.livetype.remote

import com.livetype.remote.ui.overlay.CapsuleAction
import com.livetype.remote.ui.overlay.CapsuleCommand
import com.livetype.remote.ui.overlay.CapsuleMotion
import com.livetype.remote.ui.overlay.CapsulePhase
import com.livetype.remote.ui.overlay.CapsuleRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the two behaviours the capsule was asked for:
 *  - the single trailing button is a close button on an empty field and a local clear
 *    button as soon as there is text,
 *  - the open/close motion is a critically damped spring: it starts from rest, never
 *    overshoots, and is finished by its own response time.
 */
class CapsuleMotionTest {

    @Test
    fun `an empty field keeps the close button`() {
        assertEquals(CapsuleAction.CLOSE, CapsuleMotion.actionFor(null))
        assertEquals(CapsuleAction.CLOSE, CapsuleMotion.actionFor(""))
    }

    @Test
    fun `any typed text turns the same button into a local clear`() {
        assertEquals(CapsuleAction.CLEAR, CapsuleMotion.actionFor("A"))
        assertEquals(CapsuleAction.CLEAR, CapsuleMotion.actionFor(" "))
        assertEquals(CapsuleAction.CLEAR, CapsuleMotion.actionFor("ABC123"))
    }

    @Test
    fun `the spring leaves zero and is settled by its response time`() {
        assertEquals(0f, CapsuleMotion.springFraction(CapsuleMotion.OPEN_RESPONSE_MS, 0))
        assertTrue(CapsuleMotion.springFraction(CapsuleMotion.OPEN_RESPONSE_MS, CapsuleMotion.OPEN_RESPONSE_MS) >= 0.99f)
        assertEquals(1f, CapsuleMotion.springFraction(CapsuleMotion.OPEN_RESPONSE_MS, 2000))
    }

    @Test
    fun `the curve never overshoots and never travels backwards`() {
        var previous = -1f
        for (elapsed in 0..600 step 4) {
            val value = CapsuleMotion.springFraction(CapsuleMotion.OPEN_RESPONSE_MS, elapsed)
            assertTrue("must stay inside 0..1 but was $value", value in 0f..1f)
            assertTrue("must never move backwards at $elapsed", value >= previous)
            previous = value
        }
    }

    @Test
    fun `the capsule starts slightly smaller and lands exactly on its final size`() {
        assertEquals(CapsuleMotion.FROM_SCALE, CapsuleMotion.scaleFor(0f), 0.0001f)
        assertEquals(1f, CapsuleMotion.scaleFor(1f), 0.0001f)
        assertEquals(1f, CapsuleMotion.alphaFor(1f), 0.0001f)
        assertEquals(0f, CapsuleMotion.alphaFor(0f), 0.0001f)
        assertEquals(0f, CapsuleMotion.translationFor(1f), 0.0001f)
    }

    @Test
    fun `a second close request while closing is ignored`() {
        assertEquals(
            CapsuleCommand.NONE,
            CapsuleMotion.command(CapsulePhase.HIDING, CapsuleRequest.HIDE),
        )
        assertEquals(
            CapsuleCommand.NONE,
            CapsuleMotion.command(CapsulePhase.HIDING, CapsuleRequest.TOGGLE),
        )
        assertEquals(
            CapsuleCommand.NONE,
            CapsuleMotion.command(CapsulePhase.HIDDEN, CapsuleRequest.HIDE),
        )
    }

    @Test
    fun `one open and one close are the only transitions allowed while visible`() {
        assertEquals(
            CapsuleCommand.CLOSE,
            CapsuleMotion.command(CapsulePhase.SHOWN, CapsuleRequest.HIDE),
        )
        assertEquals(
            CapsuleCommand.CLOSE,
            CapsuleMotion.command(CapsulePhase.SHOWING, CapsuleRequest.HIDE),
        )
        assertEquals(
            CapsuleCommand.NONE,
            CapsuleMotion.command(CapsulePhase.SHOWN, CapsuleRequest.SHOW),
        )
        assertEquals(
            CapsuleCommand.NONE,
            CapsuleMotion.command(CapsulePhase.SHOWING, CapsuleRequest.SHOW),
        )
    }

    @Test
    fun `a tap during the exit reverses it instead of starting a second close`() {
        assertEquals(
            CapsuleCommand.OPEN,
            CapsuleMotion.command(CapsulePhase.HIDING, CapsuleRequest.SHOW),
        )
        assertEquals(
            CapsuleCommand.OPEN,
            CapsuleMotion.command(CapsulePhase.HIDDEN, CapsuleRequest.TOGGLE),
        )
    }

    @Test
    fun `closing is quicker than opening`() {
        assertTrue(
            CapsuleMotion.responseMillis(opening = false) <
                CapsuleMotion.responseMillis(opening = true)
        )
    }
}
