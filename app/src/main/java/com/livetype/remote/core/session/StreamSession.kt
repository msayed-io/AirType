package com.livetype.remote.core.session

import com.livetype.remote.core.protocol.BridgeJson
import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.textdiff.TextDiff

/**
 * The phone-side brain of the live stream. Owns:
 *  - the local text (authoritative source for diffs),
 *  - the last text confirmed applied by the server (lastSynced),
 *  - the monotonically increasing revision counter.
 *
 * All decisions here mirror the Windows-side rules: strictly increasing revisions,
 * duplicates/old revisions are never re-sent, any divergence is healed with a
 * reset_stream + full rewrite instead of risking wrong text.
 */
class StreamSession(
    private val deviceName: String = BuildInfo.DEVICE_NAME,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

    /** What the user sees in the phone's text field. */
    var localText: String = ""
        private set

    /** Last text the server confirmed as applied (ack applied=true). */
    var lastSyncedText: String = ""
        private set

    /** Last revision the server confirmed. */
    var lastSyncedRevision: Int = 0
        private set

    /** Total ops sent since pairing (for diagnostics). */
    var opsSent: Long = 0
        private set

    /** Current revision counter — the next text_edit will carry revision + 1. */
    var revision: Int = 0
        private set

    private var streamEnabled = true

    // ---- pairing -------------------------------------------------------------

    fun pairRequest(sessionId: String, pin: String): Envelope =
        Envelope.pairRequest(sessionId, pin, deviceName)

    fun onSessionReady(env: Envelope): Boolean =
        env.type == Envelope.Types.SESSION_READY && env.ok == true && !env.token.isNullOrBlank()

    // ---- live text -------------------------------------------------------------

    /** Call on every text change; returns the op to send, or null when nothing to send. */
    fun onTextChanged(newText: String): Envelope? {
        val op = TextDiff.diff(localText, newText)
        localText = newText
        if (op.isEmpty || !streamEnabled) return null
        revision += 1
        opsSent++
        return Envelope.textEdit(revision, op.deleteCount, op.insertText, nowMs())
    }

    /**
     * "مسح النص" — clears the local field AND erases the streamed text on the PC
     * (otherwise the two sides would diverge and the next edit would misfire).
     */
    fun clearText(): Envelope? {
        val op = TextDiff.diff(localText, "")
        localText = ""
        if (!streamEnabled) return null
        if (op.deleteCount == 0) return null
        revision += 1
        opsSent++
        return Envelope.textEdit(revision, op.deleteCount, op.insertText, nowMs())
    }

    fun setStreamEnabled(enabled: Boolean) {
        streamEnabled = enabled
    }

    fun isStreamEnabled(): Boolean = streamEnabled

    // ---- server feedback ---------------------------------------------------------

    /** Ack from the server for one of our ops. */
    fun onAck(env: Envelope) {
        if (env.applied != true) return
        env.revision?.let { if (it >= lastSyncedRevision) lastSyncedRevision = it }
        // Reconstruct the synced text from what we know we sent for that revision.
        if (env.revision == revision) lastSyncedText = localText
    }

    /**
     * The server detected a revision gap and asked us to reset (it tells us its
     * mirror: baselineRevision + appliedLength). The bridge refuses text_edit
     * messages until it receives our reset_stream adopting that baseline — so the
     * answer is always TWO messages: reset_stream, then the full rewrite.
     */
    fun onServerResetRequest(env: Envelope): List<Envelope> {
        val baseline = (env.baselineRevision ?: revision).coerceAtLeast(0)
        val mirrorLen = (env.appliedLength ?: 0).coerceAtLeast(0)
        val reset = Envelope.resetStream(baselineRevision = baseline, appliedLength = mirrorLen)
        revision = baseline + 1
        opsSent++
        val rewrite = Envelope.textEdit(revision, mirrorLen, localText, nowMs())
        return listOf(reset, rewrite)
    }

    // ---- reconnect / pause-resume resync -----------------------------------------

    /**
     * After a reconnect (fresh server pump with an empty mirror, but the PC field
     * still holds the old text): reset the server mirror to our last synced length,
     * then rewrite the whole local text. Restores both mirror and field.
     */
    fun resyncAfterReconnect(): List<Envelope> {
        revision = 0
        lastSyncedRevision = 0
        val mirrorLen = lastSyncedText.length
        val reset = Envelope.resetStream(baselineRevision = 0, appliedLength = mirrorLen)
        revision = 1
        opsSent++
        val rewrite = Envelope.textEdit(revision, mirrorLen, localText, nowMs())
        return listOf(reset, rewrite)
    }

    /** Resume after a local pause: server mirror == lastSynced, rewrite only the delta. */
    fun resyncAfterPause(): List<Envelope> {
        val op = TextDiff.diff(lastSyncedText, localText)
        if (op.isEmpty) return emptyList()
        val reset = Envelope.resetStream(baselineRevision = revision, appliedLength = lastSyncedText.length)
        revision += 1
        opsSent++
        val rewrite = Envelope.textEdit(revision, op.deleteCount, op.insertText, nowMs())
        return listOf(reset, rewrite)
    }

    fun encode(env: Envelope): String = BridgeJson.encode(env)
}

/** Device name reported during pairing; kept in one place for tests. */
object BuildInfo {
    const val DEVICE_NAME = "Android-LiveType"
}
