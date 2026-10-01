package com.livetype.remote.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wire contract with the Windows app "LiveType Bridge".
 * One flat JSON envelope both ways, camelCase keys — the EXACT shape the C# side
 * produces via System.Text.Json(CamelCase). Unknown fields are ignored both ways
 * (forward compatibility); nulls are never serialized.
 */
@Serializable
data class Envelope(
    val type: String? = null,
    val sessionId: String? = null,
    val pin: String? = null,
    val deviceName: String? = null,
    val clientId: String? = null,
    val token: String? = null,
    val ok: Boolean? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val revision: Int? = null,
    val deleteCount: Int? = null,
    val insertText: String? = null,
    val timestamp: Long? = null,
    val lastRttMs: Int? = null,
    val heartbeatIntervalMs: Int? = null,
    val serverTimeUtc: Long? = null,
    val baselineRevision: Int? = null,
    val appliedLength: Int? = null,
    val reason: String? = null,
    val applied: Boolean? = null,
    val backspaces: Int? = null,
    val inserts: Int? = null,
    val receivedAtUtc: Long? = null,
) {
    object Types {
        const val PAIR_REQUEST = "pair_request"
        const val SESSION_READY = "session_ready"
        const val TEXT_EDIT = "text_edit"
        const val ACK = "ack"
        const val RESET_STREAM = "reset_stream"
        const val HEARTBEAT = "heartbeat"
        const val HEARTBEAT_ACK = "heartbeat_ack"
        const val DISCONNECT = "disconnect"
        const val ERROR = "error"
    }

    object Errors {
        const val BAD_PIN = "bad_pin"
        const val UNKNOWN_SESSION = "unknown_session"
        const val SESSION_EXPIRED = "session_expired"
        const val ALREADY_PAIRED = "already_paired"
        const val BAD_TOKEN = "bad_token"
        const val UNEXPECTED_MESSAGE = "unexpected_message"
        const val MALFORMED = "malformed_message"
        const val TOO_LARGE = "message_too_large"
        const val SERVER_CLOSING = "server_closing"
    }

    object Reasons {
        const val DUPLICATE = "duplicate"
        const val OLD = "old"
        const val RESYNC = "resync"
        const val PAUSED = "paused"
        const val LOST_SYNC = "lost_sync"
        const val PHONE_CLOSED = "phone_closed"
        const val REPLACED = "replaced"
        const val USER_DISCONNECT = "user_disconnect"
        const val STALE = "stale_connection"
    }

    companion object {
        fun pairRequest(
            sessionId: String,
            pin: String?,
            deviceName: String,
            token: String? = null,
            clientId: String? = null,
        ) = Envelope(
            type = Types.PAIR_REQUEST,
            sessionId = sessionId,
            pin = if (token.isNullOrBlank()) pin?.takeIf { it.isNotBlank() } else null,
            deviceName = deviceName,
            token = token?.takeIf { it.isNotBlank() },
            clientId = clientId?.takeIf { it.isNotBlank() },
        )

        fun textEdit(revision: Int, deleteCount: Int, insertText: String, nowMs: Long) = Envelope(
            type = Types.TEXT_EDIT,
            revision = revision,
            deleteCount = deleteCount,
            insertText = insertText,
            timestamp = nowMs,
        )

        fun resetStream(baselineRevision: Int, appliedLength: Int) = Envelope(
            type = Types.RESET_STREAM,
            baselineRevision = baselineRevision,
            appliedLength = appliedLength,
        )

        fun heartbeat(nowMs: Long, lastRttMs: Int?) = Envelope(
            type = Types.HEARTBEAT,
            timestamp = nowMs,
            lastRttMs = lastRttMs,
        )

        fun disconnect(reason: String) = Envelope(type = Types.DISCONNECT, reason = reason)
    }
}

object BridgeJson {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = false
    }

    fun encode(envelope: Envelope): String = json.encodeToString(Envelope.serializer(), envelope)

    /** Returns null for malformed frames; the caller treats null as a protocol violation. */
    fun decode(text: String): Envelope? = try {
        json.decodeFromString(Envelope.serializer(), text)
    } catch (_: Exception) {
        null
    }
}
