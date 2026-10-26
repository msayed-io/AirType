package com.livetype.remote.core.session

import android.content.Context
import android.os.Build
import com.livetype.remote.core.network.BridgeConnection
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.pairing.PairingInfo
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.pairing.SavedPairing
import com.livetype.remote.core.protocol.Envelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Everything the screens and the floating capsule need to render one session. */
data class SessionSnapshot(
    val conn: ConnState = ConnState.IDLE,
    val reconnectAttempt: Int = 0,
    val latencyMs: Int? = null,
    val text: String = "",
    val opsSent: Long = 0,
    val revision: Int = 0,
    val streamingEnabled: Boolean = true,
    val lastError: String? = null,
    val lastErrorCode: String? = null,
    val host: String = "",
    val port: Int = 0,
    val laptopName: String = "",
    val sessionExpiresAtMs: Long = 0L,
) {
    /** A session the user started and did not cut: it may still be reconnecting. */
    val hasLiveSession: Boolean
        get() = conn == ConnState.PAIRED || conn == ConnState.CONNECTING || conn == ConnState.RECONNECTING

    val isPaired: Boolean get() = conn == ConnState.PAIRED
}

/** One-shot signals the UI reacts to (vibration, discovery, navigation). */
enum class SessionEvent { PAIRED, SESSION_LOST, BAD_TOKEN }

/**
 * The single owner of the live AirType session.
 *
 * Before this class existed the connection lived inside `AppViewModel`, i.e. inside the
 * activity-scoped view model: it was created with `viewModelScope` and closed in
 * `onCleared()`. Leaving the app therefore killed the socket, and there was no process
 * level component keeping it alive. Ownership now sits here (a process singleton), so the
 * foreground service, the screens and the floating capsule all read and drive exactly one
 * [BridgeConnection] and one [StreamSession]. No second socket is ever created for the
 * overlay, and a recreated activity simply re-subscribes to the state.
 */
object AirTypeSession {

    /** Same identity the app has always reported during pairing. */
    val DEVICE_NAME: String = "Android · " + Build.MODEL.take(24)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket: stay open
        .pingInterval(0, TimeUnit.SECONDS)    // our own heartbeat instead
        .build()

    /** The one stream state used by the phone screen, dictation and the capsule. */
    val stream: StreamSession = StreamSession(deviceName = DEVICE_NAME)

    private val _snapshot = MutableStateFlow(SessionSnapshot(text = stream.localText))
    val snapshot: StateFlow<SessionSnapshot> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SessionEvent> = _events

    private var store: PairingStore? = null
    private var connection: BridgeConnection? = null
    private var currentInfo: PairingInfo? = null
    private var currentPcName: String = ""
    private var resyncOnNextSessionReady = false

    @Volatile
    var initialized: Boolean = false
        private set

    /** Initialises storage once; safe to call from the activity and from the service. */
    fun init(context: Context) {
        if (initialized) return
        val pairingStore = PairingStore(context.applicationContext)
        store = pairingStore
        initialized = true
        _snapshot.update { it.copy(streamingEnabled = stream.isStreamEnabled()) }
    }

    val clientId: String get() = store?.clientId.orEmpty()

    var rememberPairing: Boolean
        get() = store?.rememberPairing ?: true
        set(value) {
            store?.rememberPairing = value
        }

    // ---- session control ----------------------------------------------------

    fun connect(info: PairingInfo, token: String? = null, pcName: String? = null) {
        // Replacing a session is the only place a second socket may be created, and the
        // previous one is closed first, so there is never more than one live connection.
        connection?.closeByUser(Envelope.Reasons.REPLACED)
        stream.setStreamEnabled(true)
        currentInfo = info
        if (!pcName.isNullOrBlank()) currentPcName = pcName
        // A freshly discovered trusted desktop has a new server-side mirror; a transient
        // websocket reconnect inside the same session does not.
        resyncOnNextSessionReady = !token.isNullOrBlank() && stream.localText.isNotEmpty()

        val conn = BridgeConnection(
            client = client,
            scope = scope,
            deviceName = DEVICE_NAME,
            clientId = clientId,
        )
        connection = conn
        observe(conn)

        _snapshot.update {
            it.copy(
                conn = ConnState.CONNECTING,
                host = info.host,
                port = info.port,
                laptopName = currentPcName,
                sessionExpiresAtMs = info.expiresAtMs,
                lastError = null,
                lastErrorCode = null,
                text = stream.localText,
                opsSent = stream.opsSent,
                revision = stream.revision,
                streamingEnabled = true,
                reconnectAttempt = 0,
                latencyMs = null,
            )
        }
        conn.start(info.wsUrl, info.sessionId, info.pin, info.expiresAtMs, token = token)
    }

    /** User-initiated disconnect: closes the socket and clears the session state. */
    fun disconnect(reason: String = Envelope.Reasons.USER_DISCONNECT) {
        connection?.closeByUser(reason)
        connection = null
        stream.setStreamEnabled(true)
        _snapshot.update {
            it.copy(
                conn = ConnState.DISCONNECTED,
                host = "",
                port = 0,
                lastError = null,
                lastErrorCode = null,
                reconnectAttempt = 0,
                latencyMs = null,
                streamingEnabled = true,
            )
        }
    }

    // ---- text ---------------------------------------------------------------

    /** The only entry point for edits, used by the screen text field and the capsule. */
    fun onTextChanged(newText: String) {
        val env = stream.onTextChanged(newText)
        publishStreamState()
        env?.let { connection?.send(it) }
    }

    fun clearText() {
        val env = stream.clearText()
        publishStreamState()
        env?.let { connection?.send(it) }
    }

    fun toggleStream() {
        val enable = !stream.isStreamEnabled()
        stream.setStreamEnabled(enable)
        if (enable) connection?.sendAll(stream.resyncAfterPause())
        publishStreamState()
    }

    fun sendUnacknowledged() {
        connection?.sendAll(stream.unacknowledgedEdits())
        publishStreamState()
    }

    private fun publishStreamState() {
        _snapshot.update {
            it.copy(
                text = stream.localText,
                opsSent = stream.opsSent,
                revision = stream.revision,
                streamingEnabled = stream.isStreamEnabled(),
            )
        }
    }

    // ---- wiring -------------------------------------------------------------

    private fun observe(conn: BridgeConnection) {
        scope.launch {
            conn.state.collect { snap ->
                _snapshot.update {
                    it.copy(
                        conn = snap.state,
                        reconnectAttempt = snap.reconnectAttempt,
                        latencyMs = snap.lastRttMs,
                        lastErrorCode = snap.lastErrorCode ?: it.lastErrorCode,
                        lastError = snap.lastErrorDetail ?: it.lastError,
                    )
                }
            }
        }
        scope.launch {
            conn.messages.collect { env -> onServerMessage(env, conn) }
        }
        scope.launch {
            conn.reconnected.collect {
                // The desktop keeps the pump for this runtime session, so replaying only
                // unacknowledged revisions is idempotent: applied ones are refused as
                // duplicates and genuinely lost ones apply exactly once.
                conn.sendAll(stream.unacknowledgedEdits())
                publishStreamState()
            }
        }
    }

    private fun onServerMessage(env: Envelope, conn: BridgeConnection) {
        when (env.type) {
            Envelope.Types.SESSION_READY -> {
                val info = currentInfo
                val token = env.token.orEmpty()
                if (info != null && token.isNotBlank() && rememberPairing) {
                    store?.save(
                        SavedPairing(
                            wsUrl = info.wsUrl,
                            host = info.host,
                            port = info.port,
                            sessionId = info.sessionId,
                            // The permanent token replaces first-time PIN authentication.
                            pin = "",
                            token = token,
                            expiresAtMs = 0L,
                            deviceName = currentPcName.ifBlank { info.host },
                        )
                    )
                }
                if (resyncOnNextSessionReady) {
                    resyncOnNextSessionReady = false
                    conn.sendAll(stream.resyncAfterReconnect())
                }
                publishStreamState()
                _events.tryEmit(SessionEvent.PAIRED)
            }

            Envelope.Types.ACK -> {
                stream.onAck(env)
                // The reset ack reports the mirror the PC really holds; only now can the
                // rewrite happen without leaving the un-acked tail in place.
                stream.onResetAck(env)?.let { rewrite -> conn.sendAll(listOf(rewrite)) }
                publishStreamState()
            }

            Envelope.Types.RESET_STREAM -> {
                conn.sendAll(stream.onServerResetRequest(env))
                publishStreamState()
            }

            Envelope.Types.DISCONNECT -> {
                _snapshot.update { it.copy(lastError = "server_closing") }
            }

            Envelope.Types.ERROR -> {
                when (env.errorCode) {
                    Envelope.Errors.BAD_TOKEN -> {
                        store?.clear()
                        _snapshot.update {
                            it.copy(
                                lastError = env.errorMessage ?: "Token expired",
                                lastErrorCode = env.errorCode,
                            )
                        }
                        _events.tryEmit(SessionEvent.BAD_TOKEN)
                    }
                    Envelope.Errors.UNKNOWN_SESSION, Envelope.Errors.SESSION_EXPIRED -> {
                        // The desktop restarted with a fresh runtime sessionId: the saved
                        // token is still good, only the session id is stale.
                        _snapshot.update { it.copy(lastError = null, lastErrorCode = null) }
                        _events.tryEmit(SessionEvent.SESSION_LOST)
                    }
                    else -> {
                        _snapshot.update {
                            it.copy(lastError = env.errorCode, lastErrorCode = env.errorCode)
                        }
                    }
                }
            }
        }
    }
}
