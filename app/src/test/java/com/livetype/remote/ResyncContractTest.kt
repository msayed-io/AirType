package com.livetype.remote

import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.session.StreamSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reported defect: text appeared twice after a re-sync. The phone used to send
 * reset_stream and the full rewrite in the same breath using the length it *believed*
 * the PC had synced. When an ack had been lost, that belief was shorter than the real
 * field, so the PC deleted only the stale part and the rewrite landed on top of the rest.
 *
 * These tests lock the two-step contract: ask for the reset, learn the real mirror from
 * the server's reset ack, then rewrite exactly once.
 */
class ResyncContractTest {

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

    /** Session whose second edit was typed on the PC but never acked back. */
    private fun sessionWithLostAck(): StreamSession {
        val session = StreamSession(deviceName = "test-phone") { 0L }
        session.onTextChanged("مرحبا")
        session.onAck(ackOf(1))
        session.onTextChanged("مرحبا عالم")
        assertEquals("مرحبا", session.lastSyncedText)
        assertEquals("مرحبا عالم", session.localText)
        return session
    }

    @Test
    fun `reconnect resync asks for the reset first and sends no rewrite yet`() {
        val session = sessionWithLostAck()

        val out = session.resyncAfterReconnect()

        assertEquals(1, out.size)
        assertEquals(Envelope.Types.RESET_STREAM, out[0].type)
        assertEquals(0, out[0].baselineRevision)
        assertEquals("مرحبا".length, out[0].appliedLength)
        assertTrue(session.isAwaitingResetAck)
    }

    @Test
    fun `rewrite deletes the mirror the server reports, not the stale local belief`() {
        val session = sessionWithLostAck()
        session.resyncAfterReconnect()

        val rewrite = session.onResetAck(resetAck(baselineRevision = 0, appliedLength = "مرحبا عالم".length))

        assertNotNull(rewrite)
        assertEquals(Envelope.Types.TEXT_EDIT, rewrite!!.type)
        assertEquals(1, rewrite.revision)
        // 10, the length the PC actually typed — not 5 from lastSyncedText.
        assertEquals("مرحبا عالم".length, rewrite.deleteCount)
        assertEquals("مرحبا عالم", rewrite.insertText)
        assertFalse(session.isAwaitingResetAck)
    }

    @Test
    fun `an old desktop that echoes the stale length keeps the old single-op behaviour`() {
        val session = sessionWithLostAck()
        session.resyncAfterReconnect()

        val rewrite = session.onResetAck(resetAck(baselineRevision = 0, appliedLength = "مرحبا".length))

        assertNotNull(rewrite)
        assertEquals("مرحبا".length, rewrite!!.deleteCount)
        assertEquals("مرحبا عالم", rewrite.insertText)
    }

    @Test
    fun `after a local clear the resync keeps the PC prefix instead of wiping it`() {
        val session = StreamSession(deviceName = "test-phone") { 0L }
        session.onTextChanged("ملاحظة قديمة")
        session.onAck(ackOf(1))

        // "مسح النص" clears the phone only; the PC keeps its text.
        session.clearText()
        session.onTextChanged("سطر جديد")
        session.onAck(ackOf(2))
        assertEquals("سطر جديد", session.localText)
        assertEquals("سطر جديد", session.lastSyncedText)

        session.resyncAfterReconnect()
        val rewrite = session.onResetAck(
            resetAck(baselineRevision = 0, appliedLength = "ملاحظة قديمة".length + "سطر جديد".length)
        )

        assertNotNull(rewrite)
        // Delete only our own tail, never the unknown prefix that is already on the PC.
        assertEquals("سطر جديد".length, rewrite!!.deleteCount)
        assertEquals("سطر جديد", rewrite.insertText)
    }

    /**
     * Known limit, stated instead of hidden: after a local clear the phone only owns the
     * tail it typed, and it cannot learn how much of an *un-acked* tail the PC applied.
     * It therefore deletes just the acked tail (here: nothing) and appends. Fixing this
     * residual case would need a new field on the wire, which this change is not allowed
     * to add.
     */
    @Test
    fun `an unacked tail after a local clear deletes only what was acked`() {
        val session = StreamSession(deviceName = "test-phone") { 0L }
        session.onTextChanged("ملاحظة قديمة")
        session.onAck(ackOf(1))
        session.clearText()
        session.onTextChanged("سطر جديد")

        session.resyncAfterReconnect()
        val rewrite = session.onResetAck(
            resetAck(baselineRevision = 0, appliedLength = "ملاحظة قديمة".length + "سطر جديد".length)
        )

        assertEquals(0, rewrite!!.deleteCount)
        assertEquals("سطر جديد", rewrite.insertText)
    }

    @Test
    fun `a normal edit ack never triggers a resync rewrite`() {
        val session = sessionWithLostAck()

        assertNull(session.onResetAck(ackOf(2)))
        assertNull(session.onResetAck(resetAck(baselineRevision = 0, appliedLength = "مرحبا عالم".length)))
    }

    @Test
    fun `a rejected reset ack is not treated as the mirror`() {
        val session = sessionWithLostAck()
        session.resyncAfterReconnect()

        val notApplied = resetAck(baselineRevision = 0, appliedLength = "مرحبا عالم".length)
            .copy(applied = false)
        assertNull(session.onResetAck(notApplied))
        assertTrue(session.isAwaitingResetAck)
    }
}
