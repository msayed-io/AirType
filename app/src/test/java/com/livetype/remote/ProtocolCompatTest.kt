package com.livetype.remote

import com.livetype.remote.core.pairing.QrError
import com.livetype.remote.core.pairing.QrParseResult
import com.livetype.remote.core.pairing.QrPayloadParser
import com.livetype.remote.core.protocol.BridgeJson
import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.session.StreamSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the wire contract to the exact shape the Windows "LiveType Bridge" app
 * (System.Text.Json, camelCase) emits and parses. If either side drifts, these fail.
 */
class ProtocolCompatTest {

    private fun keysOf(json: String): Set<String> =
        Regex("\"([a-zA-Z_]+)\":").findAll(json).map { it.groupValues[1] }.toSet()

    @Test
    fun `text_edit carries exactly the Windows-side camelCase keys`() {
        val json = BridgeJson.encode(Envelope.textEdit(3, 2, "نص", 123L))
        val keys = keysOf(json)
        assertTrue(keys.containsAll(listOf("type", "revision", "deleteCount", "insertText", "timestamp")))
        // The Windows side uses camelCase "deleteCount"/"insertText" — NOT snake_case.
        assertTrue(json.contains("\"deleteCount\""))
        assertTrue(json.contains("\"insertText\""))
    }

    @Test
    fun `pair_request shape`() {
        val json = BridgeJson.encode(Envelope.pairRequest("sid123", "431552", "Pixel"))
        assertTrue(json.contains("\"type\":\"pair_request\""))
        assertTrue(json.contains("\"sessionId\":\"sid123\""))
        assertTrue(json.contains("\"pin\":\"431552\""))
        assertTrue(json.contains("\"deviceName\":\"Pixel\""))
    }

    @Test
    fun `reset_stream shape`() {
        val json = BridgeJson.encode(Envelope.resetStream(2, 11))
        assertTrue(json.contains("\"baselineRevision\":2"))
        assertTrue(json.contains("\"appliedLength\":11"))
    }

    @Test
    fun `pair_request advertises protocol version 1`() {
        val json = BridgeJson.encode(Envelope.pairRequest("sid123", "431552", "Pixel", clientId = "android_x"))
        assertTrue(json.contains("\"protocolVersion\":1"))
        // The field is additive: a build that predates it simply never sees it.
        val decoded = BridgeJson.decode("""{"type":"pair_request","sessionId":"s","pin":"123456","deviceName":"Pixel"}""")
        assertNull(decoded?.protocolVersion)
    }

    @Test
    fun `session_ready without protocolVersion is treated as protocol 1`() {
        val env = BridgeJson.decode("""{"type":"session_ready","ok":true,"token":"abc","heartbeatIntervalMs":2500}""")
        assertNotNull(env)
        assertEquals(
            Envelope.ProtocolVersion.CURRENT,
            env!!.protocolVersion ?: Envelope.ProtocolVersion.CURRENT,
        )
    }

    @Test
    fun `unsupported_version error is decoded and recognised`() {
        val env = BridgeJson.decode("""{"type":"error","errorCode":"unsupported_version","errorMessage":"newer"}""")
        assertNotNull(env)
        assertEquals(Envelope.Errors.UNSUPPORTED_VERSION, env!!.errorCode)
        assertEquals("unsupported_version", Envelope.Errors.UNSUPPORTED_VERSION)
    }

    @Test
    fun `qr without a version field is accepted as protocol 1`() {
        val result = QrPayloadParser.parse(
            """{"app":"LiveTypeBridge","host":"192.168.1.5","port":53017,"sid":"abc","pin":"123456","exp":9999999999999}""",
            nowMs = 1_000_000L,
        )
        assertTrue(result is QrParseResult.Ok)
    }

    @Test
    fun `qr with the current version is accepted`() {
        val result = QrPayloadParser.parse(
            """{"v":1,"app":"LiveTypeBridge","host":"192.168.1.5","port":53017,"sid":"abc","pin":"123456","exp":9999999999999}""",
            nowMs = 1_000_000L,
        )
        assertTrue(result is QrParseResult.Ok)
    }

    @Test
    fun `qr from a newer protocol is rejected with a clear error`() {
        val result = QrPayloadParser.parse(
            """{"v":2,"app":"LiveTypeBridge","host":"192.168.1.5","port":53017,"sid":"abc","pin":"123456","exp":9999999999999}""",
            nowMs = 1_000_000L,
        )
        assertEquals(QrError.UNSUPPORTED_VERSION, (result as QrParseResult.Invalid).reason)
    }

    @Test
    fun `decode ignores unknown fields like the Windows side does`() {
        val incoming = """
            {"type":"ack","revision":5,"applied":true,"reason":"duplicate",
             "backspaces":0,"inserts":0,"receivedAtUtc":1700000000000,
             "futureFieldFromNewerServer":42}
        """.trimIndent()
        val env = BridgeJson.decode(incoming)
        assertNotNull(env)
        assertEquals("ack", env!!.type)
        assertEquals(5, env.revision)
        assertEquals(true, env.applied)
    }

    @Test
    fun `decode of the exact QR payload Windows generates`() {
        // This is the literal JSON produced by QrPayload.Build on the Windows side.
        val windowsQr = """
            {"v":1,"app":"LiveTypeBridge","ws":"ws://192.168.1.123:53017/livetype",
             "host":"192.168.1.123","port":53017,"sid":"c34d6bb93a15a931",
             "pin":"432048","exp":9999999999999}
        """.trimIndent()
        val result = QrPayloadParser.parse(windowsQr, nowMs = 1_000_000L)
        assertTrue(result is QrParseResult.Ok)
        val info = (result as QrParseResult.Ok).info
        assertEquals("ws://192.168.1.123:53017/livetype", info.wsUrl)
        assertEquals(53017, info.port)
        assertEquals("c34d6bb93a15a931", info.sessionId)
        assertEquals("432048", info.pin)
    }

    @Test
    fun `qr from another app is rejected`() {
        val result = QrPayloadParser.parse("""{"app":"OtherApp","host":"1.2.3.4","port":80,"sid":"x","pin":"123456","exp":9999999999999}""")
        assertEquals(QrError.WRONG_APP, (result as QrParseResult.Invalid).reason)
    }

    @Test
    fun `expired qr is rejected`() {
        val result = QrPayloadParser.parse(
            """{"app":"LiveTypeBridge","host":"1.2.3.4","port":80,"sid":"x","pin":"123456","exp":1000}""",
            nowMs = 2000L,
        )
        assertEquals(QrError.EXPIRED, (result as QrParseResult.Invalid).reason)
    }

    @Test
    fun `garbage qr is rejected safely`() {
        assertNull(BridgeJson.decode("not json {{"))
        val result = QrPayloadParser.parse("随便 nonsense")
        assertTrue(result is QrParseResult.Invalid)
    }
}

class RevisionOrderingTest {

    private fun session() = StreamSession(nowMs = { 42L })

    @Test
    fun `revisions increase monotonically with text changes`() {
        val s = session()
        s.onTextChanged("a")
        s.onTextChanged("ab")
        s.onTextChanged("abc")
        assertEquals(3, s.revision)
    }

    @Test
    fun `no-op changes do not consume revisions`() {
        val s = session()
        s.onTextChanged("text")
        s.onTextChanged("text") // identical — nothing sent
        assertEquals(1, s.revision)
    }

    @Test
    fun `delayed acknowledgements advance the exact confirmed text`() {
        val s = session()
        s.onTextChanged("ا")
        s.onTextChanged("اب")
        s.onTextChanged("ابت")

        // The UI is already at revision 3 when ACK 1 arrives.
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        assertEquals("ا", s.lastSyncedText)
        assertEquals(1, s.lastSyncedRevision)

        s.onAck(Envelope(type = "ack", revision = 2, applied = true))
        assertEquals("اب", s.lastSyncedText)
        s.onAck(Envelope(type = "ack", revision = 3, applied = true))
        assertEquals("ابت", s.lastSyncedText)
    }

    @Test
    fun `transient reconnect replays only unacknowledged revisions`() {
        val s = session()
        s.onTextChanged("a")
        s.onTextChanged("ab")
        s.onTextChanged("abc")
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))

        val replay = s.unacknowledgedEdits()
        assertEquals(listOf(2, 3), replay.map { it.revision })
        assertEquals(listOf("b", "c"), replay.map { it.insertText })
    }

    @Test
    fun `reconnect after lagging ack replaces exactly the mirror the PC reports`() {
        val s = session()
        s.onTextChanged("نص")
        s.onTextChanged("نص طويل")
        s.onTextChanged("نص طويل جدا")
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        s.onAck(Envelope(type = "ack", revision = 2, applied = true))

        // Two-step: the reset asks, the ack answers with what the PC really holds.
        val reset = s.resyncAfterReconnect().single()
        assertEquals("reset_stream", reset.type)
        assertEquals("نص طويل".length, reset.appliedLength)

        val rewrite = s.onResetAck(Envelope(
            type = "ack",
            applied = true,
            baselineRevision = 0,
            appliedLength = "نص طويل جدا".length,
            reason = "reset",
        ))
        assertEquals("نص طويل جدا".length, rewrite!!.deleteCount)
        assertEquals("نص طويل جدا", rewrite.insertText)
    }

    @Test
    fun `local clear prevents stale acknowledgements restoring old baseline`() {
        val s = session()
        s.onTextChanged("قديم")
        s.clearText()
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        assertEquals("", s.lastSyncedText)
    }

    @Test
    fun `server reset request is answered with reset + rewrite pair`() {
        val s = session()
        s.onTextChanged("مرحبا Hello")
        val answer = s.onServerResetRequest(Envelope(type = "reset_stream", baselineRevision = 2, appliedLength = 11))
        assertEquals(2, answer.size)
        assertEquals("reset_stream", answer[0].type)
        assertEquals(2, answer[0].baselineRevision)
        assertEquals(11, answer[0].appliedLength)
        assertEquals("text_edit", answer[1].type)
        assertEquals(3, answer[1].revision)
        assertEquals(11, answer[1].deleteCount)
        assertEquals("مرحبا Hello", answer[1].insertText)
    }

    @Test
    fun `reconnect resync restores mirror and field`() {
        val s = session()
        s.onTextChanged("كامل النص المكتوب")
        // Only server-confirmed text counts: the ack makes revision 1 synced.
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        val reset = s.resyncAfterReconnect().single()
        assertEquals("reset_stream", reset.type)
        assertEquals(0, reset.baselineRevision)

        val rewrite = s.onResetAck(Envelope(
            type = "ack",
            applied = true,
            baselineRevision = 0,
            appliedLength = "كامل النص المكتوب".length,
            reason = "reset",
        ))
        // The PC field still holds the synced text; rewrite must delete that length first.
        assertEquals("كامل النص المكتوب".length, rewrite!!.deleteCount)
        assertEquals("كامل النص المكتوب", rewrite.insertText)
        assertEquals(1, rewrite.revision)
    }

    @Test
    fun `reconnect with an unacked op deletes what the PC typed, not the local belief`() {
        val s = session()
        s.onTextChanged("نص مؤكد")
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        // Sent but the ack died on the way back; the PC may well have typed it.
        s.onTextChanged("نص مؤكد وغير مؤكد")
        val reset = s.resyncAfterReconnect().single()
        // The hint is still only what we know for sure.
        assertEquals("نص مؤكد".length, reset.appliedLength)

        // The PC answers with its real mirror (both ops applied): delete that, not the hint.
        val rewrite = s.onResetAck(Envelope(
            type = "ack",
            applied = true,
            baselineRevision = 0,
            appliedLength = "نص مؤكد وغير مؤكد".length,
            reason = "reset",
        ))
        assertEquals("نص مؤكد وغير مؤكد".length, rewrite!!.deleteCount)
        assertEquals("نص مؤكد وغير مؤكد", rewrite.insertText)
    }

    @Test
    fun `pause then resume sends only the delta`() {
        val s = session()
        s.onTextChanged("نص")
        s.onAck(Envelope(type = "ack", revision = 1, applied = true))
        s.setStreamEnabled(false)
        s.onTextChanged("نص زيادة كتير بعدين")
        val msgs = s.resyncAfterPause()
        assertEquals(2, msgs.size)
        assertEquals("reset_stream", msgs[0].type)
        assertEquals(1, msgs[0].baselineRevision)
        assertEquals("نص".length, msgs[0].appliedLength)
        assertEquals(0, msgs[1].deleteCount)
        assertEquals(" زيادة كتير بعدين", msgs[1].insertText)
    }

    @Test
    fun `clear text clears only local phone text without deleting remote laptop text`() {
        val s = session()
        s.onTextChanged("hello")
        val env = s.clearText()
        assertNull(env)
        assertEquals("", s.localText)
        // Next edit sends new text with deleteCount = 0 (no backspaces on laptop)
        val nextEnv = s.onTextChanged("world")
        assertNotNull(nextEnv)
        assertEquals(0, nextEnv!!.deleteCount)
        assertEquals("world", nextEnv.insertText)
    }

    @Test
    fun `streaming disabled swallows edits but keeps local text`() {
        val s = session()
        s.onTextChanged("بداية")
        s.setStreamEnabled(false)
        val env = s.onTextChanged("بداية وزيادة")
        assertNull(env)
        assertEquals("بداية وزيادة", s.localText)
    }
}
