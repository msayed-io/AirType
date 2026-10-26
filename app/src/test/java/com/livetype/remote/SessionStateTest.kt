package com.livetype.remote

import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.session.SessionSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The screens, the notification and the floating bubble all decide "is there a session to
 * keep alive?" from this one mapping. If it drifts, the bubble can survive a dead session
 * (lying to the user) or disappear during a normal reconnect (losing the typing surface).
 */
class SessionStateTest {

    @Test
    fun `paired, connecting and reconnecting all count as a live session`() {
        assertTrue(SessionSnapshot(conn = ConnState.PAIRED).hasLiveSession)
        assertTrue(SessionSnapshot(conn = ConnState.CONNECTING).hasLiveSession)
        assertTrue(SessionSnapshot(conn = ConnState.RECONNECTING).hasLiveSession)
    }

    @Test
    fun `idle, disconnected and failed do not count as live`() {
        assertFalse(SessionSnapshot(conn = ConnState.IDLE).hasLiveSession)
        assertFalse(SessionSnapshot(conn = ConnState.DISCONNECTED).hasLiveSession)
        assertFalse(SessionSnapshot(conn = ConnState.FAILED).hasLiveSession)
    }

    @Test
    fun `only the paired state is reported as connected`() {
        assertTrue(SessionSnapshot(conn = ConnState.PAIRED).isPaired)
        assertFalse(SessionSnapshot(conn = ConnState.RECONNECTING).isPaired)
        assertFalse(SessionSnapshot(conn = ConnState.CONNECTING).isPaired)
    }
}
