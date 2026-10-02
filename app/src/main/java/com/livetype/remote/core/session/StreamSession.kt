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

    private data class PendingEdit(
        val deleteCount: Int,
        val insertText: String,
        val resultingText: String? = null,
    )

    /**
     * Edits sent but not yet confirmed. Keeping the operation for each revision lets
     * delayed ACKs advance [lastSyncedText] to the exact text that revision produced;
     * using the current UI text here would lose confirmation whenever typing outruns
     * the network round trip.
     */
    private val pendingEdits = sortedMapOf<Int, PendingEdit>()
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
        pendingEdits[revision] = PendingEdit(op.deleteCount, op.insertText)
        trimPendingEdits()
        return Envelope.textEdit(revision, op.deleteCount, op.insertText, nowMs())
    }

    /**
     * "مسح النص" — clears the local text in the phone app ONLY,
     * without erasing the text already typed on the laptop.
     * Future edits append from an empty baseline with deleteCount = 0.
     */
    fun clearText(): Envelope? {
        localText = ""
        lastSyncedText = ""
        lastSyncedRevision = revision
        pendingEdits.clear()
        return null
    }

    fun setStreamEnabled(enabled: Boolean) {
        streamEnabled = enabled
    }

    fun isStreamEnabled(): Boolean = streamEnabled

    // ---- server feedback ---------------------------------------------------------

    /** Ack from the server for one of our ops. */
    fun onAck(env: Envelope) {
        if (env.applied != true) return
        val acknowledgedRevision = env.revision ?: return
        if (acknowledgedRevision <= lastSyncedRevision) return

        // ACKs can lag many revisions behind fast typing/dictation. Advance through
        // the exact queued operations instead of assigning the current localText,
        // which may already represent a much newer revision.
        while (lastSyncedRevision < acknowledgedRevision) {
            val nextRevision = lastSyncedRevision + 1
            val pending = pendingEdits.remove(nextRevision) ?: return
            lastSyncedText = pending.resultingText
                ?: TextDiff.apply(
                    lastSyncedText,
                    TextDiff.Op(pending.deleteCount, pending.insertText),
                )
            lastSyncedRevision = nextRevision
        }
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
        lastSyncedRevision = baseline
        pendingEdits.clear()
        opsSent++
        pendingEdits[revision] = PendingEdit(mirrorLen, localText, resultingText = localText)
        val rewrite = Envelope.textEdit(revision, mirrorLen, localText, nowMs())
        return listOf(reset, rewrite)
    }

    // ---- reconnect / pause-resume resync -----------------------------------------

    /**
     * Replays only unacknowledged revisions after a transient socket replacement.
     * Desktop keeps the same stream mirror, so already-applied revisions are ignored
     * as duplicates and genuinely lost revisions are applied exactly once.
     */
    fun unacknowledgedEdits(): List<Envelope> = pendingEdits.map { (pendingRevision, edit) ->
        Envelope.textEdit(
            pendingRevision,
            edit.deleteCount,
            edit.insertText,
            nowMs(),
        )
    }

    /**
     * After a reconnect (fresh server pump with an empty mirror, but the PC field
     * still holds the old text): reset the server mirror to our last synced length,
     * then rewrite the whole local text. Restores both mirror and field.
     */
    fun resyncAfterReconnect(): List<Envelope> {
        revision = 0
        lastSyncedRevision = 0
        pendingEdits.clear()
        val mirrorLen = lastSyncedText.length
        val reset = Envelope.resetStream(baselineRevision = 0, appliedLength = mirrorLen)
        revision = 1
        opsSent++
        pendingEdits[revision] = PendingEdit(mirrorLen, localText, resultingText = localText)
        val rewrite = Envelope.textEdit(revision, mirrorLen, localText, nowMs())
        return listOf(reset, rewrite)
    }

    /** Resume after a local pause: server mirror == lastSynced, rewrite only the delta. */
    fun resyncAfterPause(): List<Envelope> {
        val op = TextDiff.diff(lastSyncedText, localText)
        if (op.isEmpty) return emptyList()
        val reset = Envelope.resetStream(baselineRevision = revision, appliedLength = lastSyncedText.length)
        lastSyncedRevision = revision
        pendingEdits.clear()
        revision += 1
        opsSent++
        pendingEdits[revision] = PendingEdit(op.deleteCount, op.insertText, resultingText = localText)
        val rewrite = Envelope.textEdit(revision, op.deleteCount, op.insertText, nowMs())
        return listOf(reset, rewrite)
    }

    private fun trimPendingEdits() {
        // Bound memory while offline without compromising normal ACK round trips.
        while (pendingEdits.size > MAX_PENDING_EDITS) {
            pendingEdits.remove(pendingEdits.firstKey())
        }
    }

    fun encode(env: Envelope): String = BridgeJson.encode(env)

    private companion object {
        const val MAX_PENDING_EDITS = 1024
    }
}

/** Device name reported during pairing; kept in one place for tests. */
object BuildInfo {
    const val DEVICE_NAME = "Android-LiveType"
}
