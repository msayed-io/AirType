package com.livetype.remote

import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.session.StreamSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The dangerous part of the capsule's clear button: it must empty the phone-side field
 * without ever turning into a delete on the desktop.
 *
 * These tests run the real [StreamSession] — the same object the capsule drives through
 * `AirTypeSession.clearText()` — and prove that:
 *  - clearing produces no envelope at all,
 *  - the first character typed afterwards is an append with `deleteCount = 0`,
 *  - even a reconnect after a clear deletes only the tail this phone typed, never the
 *    text the desktop already holds.
 *
 * If any of these ever sends a delete for the pre-clear text, the acceptance criterion
 * "المسح المحلي لا يعني الحذف على الكمبيوتر" is broken and these tests fail.
 */
class LocalClearContractTest {

    private fun ackOf(revision: Int) = Envelope(
        type = Envelope.Types.ACK,
        revision = revision,
        applied = true,
    )

    private fun resetAck(baselineRevision: Int, appliedLength: Int) = Envelope(
        type = Envelope.Types.ACK,
        applied = true,
        baselineRevision = baselineRevision,
        appliedLength = appliedLength,
        reason = Envelope.Reasons.RESET,
    )

    private fun sessionWithText(): StreamSession {
        val session = StreamSession(deviceName = "test-phone") { 0L }
        session.onTextChanged("ABC123")
        return session
    }

    @Test
    fun `clearing the field returns no operation to send`() {
        val session = sessionWithText()
        assertNull(session.clearText())
        assertEquals("", session.localText)
    }

    @Test
    fun `typing after a local clear appends instead of deleting the desktop text`() {
        val session = sessionWithText()
        session.clearText()
        val op = session.onTextChanged("X")
        assertNotNull(op)
        assertEquals(0, op!!.deleteCount ?: -1)
        assertEquals("X", op.insertText)
    }

    @Test
    fun `a reconnect after a local clear only deletes the tail this phone typed`() {
        val session = sessionWithText()
        session.onAck(ackOf(1))
        session.clearText()
        session.onTextChanged("X")
        session.onAck(ackOf(2))
        session.resyncAfterReconnect()
        val rewrite = session.onResetAck(resetAck(baselineRevision = 0, appliedLength = 7))
        assertNotNull(rewrite)
        // One character (the tail) — never the seven characters already on the desktop.
        assertEquals(1, rewrite!!.deleteCount ?: -1)
    }
}
