package com.livetype.remote.core.network

import com.livetype.remote.core.protocol.BridgeJson
import com.livetype.remote.core.protocol.Envelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** High-level connection state shown to the user. */
enum class ConnState { IDLE, CONNECTING, PAIRED, RECONNECTING, DISCONNECTED, FAILED }

/** One connection to one Windows bridge. Not reusable after a terminal failure — create fresh. */
class BridgeConnection(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val deviceName: String,
    private val clientId: String = "",
) {
    data class Snapshot(
        val state: ConnState = ConnState.IDLE,
        val wsUrl: String? = null,
        val token: String? = null,
        val heartbeatIntervalMs: Int = 2500,
        val reconnectAttempt: Int = 0,
        val lastErrorCode: String? = null,
        val lastErrorDetail: String? = null,
        val lastRttMs: Int? = null,
    )

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state

    private val _messages = MutableSharedFlow<Envelope>(extraBufferCapacity = 64)
    val messages: SharedFlow<Envelope> = _messages

    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val reconnected: SharedFlow<Unit> = _reconnected

    private val wsRef = AtomicReference<WebSocket?>(null)
    private val authRef = AtomicReference<String?>(null)
    private val closingByUser = AtomicBoolean(false)
    private val terminalFailure = AtomicBoolean(false)
    private var sessionId: String? = null
    private var pin: String? = null
    private var initialToken: String? = null
    private var sessionExpiresAtMs: Long = 0
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    // ---- lifecycle ---------------------------------------------------------

    fun start(
        url: String,
        sessionId: String,
        pin: String,
        expiresAtMs: Long,
        token: String? = null,
    ) {
        this.sessionId = sessionId
        this.pin = pin
        this.initialToken = token
        this.sessionExpiresAtMs = expiresAtMs
        closingByUser.set(false)
        terminalFailure.set(false)
        openSocket(url, isReconnect = false)
    }

    fun closeByUser(reason: String = Envelope.Reasons.USER_DISCONNECT) {
        closingByUser.set(true)
        stopHeartbeat()
        reconnectJob?.cancel()
        val ws = wsRef.getAndSet(null) ?: return
        runCatching { ws.send(BridgeJson.encode(Envelope.disconnect(reason))) }
        runCatching { ws.close(1000, reason) }
        _state.value = _state.value.copy(state = ConnState.DISCONNECTED)
    }

    fun send(env: Envelope): Boolean {
        val ws = wsRef.get() ?: return false
        val withToken = if (env.type == Envelope.Types.HEARTBEAT) env
        else env.copy(token = authRef.get())
        return runCatching { ws.send(BridgeJson.encode(withToken)) }.getOrDefault(false)
    }

    fun sendAll(envs: List<Envelope>): Boolean = envs.all { send(it) }

    // ---- internals ---------------------------------------------------------

    private fun openSocket(url: String, isReconnect: Boolean) {
        _state.value = _state.value.copy(
            state = if (isReconnect) ConnState.RECONNECTING else ConnState.CONNECTING,
            wsUrl = url,
        )
        // A malformed manual-entry URL must fail loudly, never crash the app.
        val request = runCatching { Request.Builder().url(url).build() }
            .getOrElse {
                _state.value = _state.value.copy(
                    state = ConnState.FAILED,
                    lastErrorCode = "bad_address",
                    lastErrorDetail = it.message?.take(140),
                )
                return
            }
        wsRef.set(client.newWebSocket(request, Listener(isReconnect)))
    }

    private inner class Listener(private val isReconnect: Boolean) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            val sid = sessionId ?: return
            val p = pin ?: ""
            val tok = initialToken ?: authRef.get()
            webSocket.send(BridgeJson.encode(Envelope.pairRequest(
                sessionId = sid,
                pin = p,
                deviceName = deviceName,
                token = tok,
                clientId = clientId.takeIf { it.isNotBlank() },
            )))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val env = BridgeJson.decode(text) ?: return
            when (env.type) {
                Envelope.Types.SESSION_READY -> {
                    authRef.set(env.token)
                    val wasReconnect = isReconnect
                    _state.value = _state.value.copy(
                        state = ConnState.PAIRED,
                        token = env.token,
                        heartbeatIntervalMs = env.heartbeatIntervalMs ?: 2500,
                        reconnectAttempt = 0,
                        lastErrorCode = null,
                        lastErrorDetail = null,
                    )
                    startHeartbeat(env.heartbeatIntervalMs ?: 2500)
                    // Pairing persistence and UI side effects are owned by AppViewModel;
                    // do not swallow this protocol message inside the transport layer.
                    _messages.tryEmit(env)
                    if (wasReconnect) _reconnected.tryEmit(Unit)
                }

                Envelope.Types.HEARTBEAT_ACK -> {
                    env.timestamp?.let { sent ->
                        val rtt = (System.currentTimeMillis() - sent).toInt().coerceAtLeast(0)
                        if (rtt < 60_000) _state.value = _state.value.copy(lastRttMs = rtt)
                    }
                }

                Envelope.Types.ERROR -> {
                    _state.value = _state.value.copy(
                        state = ConnState.FAILED,
                        lastErrorCode = env.errorCode,
                        lastErrorDetail = env.errorMessage,
                    )
                    stopHeartbeat()
                    _messages.tryEmit(env)
                    // A dead session cannot be retried: pairing data must be refreshed.
                    if (env.errorCode in TERMINAL_SESSION_ERRORS) {
                        terminalFailure.set(true)
                        reconnectJob?.cancel()
                    }
                }

                else -> _messages.tryEmit(env)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (closingByUser.get()) return
            stopHeartbeat()
            // Surface the transport reason (e.g. cleartext blocked, unreachable host)
            // instead of silently retrying forever with no feedback.
            _state.value = _state.value.copy(
                lastErrorCode = "transport",
                lastErrorDetail = t.message?.take(140) ?: "network error",
            )
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (closingByUser.get()) return
            stopHeartbeat()
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (closingByUser.get() || terminalFailure.get()) return
        if (sessionExpiresAtMs > 0 && System.currentTimeMillis() >= sessionExpiresAtMs) {
            _state.value = _state.value.copy(state = ConnState.DISCONNECTED)
            return
        }
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var attempt = 0
            while (isActive && !closingByUser.get() && (sessionExpiresAtMs <= 0 || System.currentTimeMillis() < sessionExpiresAtMs)) {
                attempt++
                val backoffMs = longArrayOf(1000, 2000, 4000, 5000)[(attempt - 1).coerceAtMost(3)]
                delay(backoffMs)
                val url = _state.value.wsUrl ?: break
                if (_state.value.state != ConnState.PAIRED) {
                    _state.value = _state.value.copy(state = ConnState.RECONNECTING, reconnectAttempt = attempt)
                    openSocket(url, isReconnect = true)
                }
                // Wait for either pairing or failure before next attempt.
                var waited = 0
                while (waited < 8000 && _state.value.state == ConnState.RECONNECTING && isActive) {
                    delay(250); waited += 250
                }
                if (_state.value.state == ConnState.PAIRED) return@launch
            }
        }
    }

    private fun startHeartbeat(intervalMs: Int) {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs.toLong().coerceIn(1000, 10_000))
                val rtt = _state.value.lastRttMs
                val ws = wsRef.get() ?: continue
                runCatching { ws.send(BridgeJson.encode(Envelope.heartbeat(System.currentTimeMillis(), rtt))) }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private companion object {
        /** Errors that mean the session data itself is dead — retrying is pointless. */
        val TERMINAL_SESSION_ERRORS = setOf(
            Envelope.Errors.BAD_PIN,
            Envelope.Errors.UNKNOWN_SESSION,
            Envelope.Errors.SESSION_EXPIRED,
            Envelope.Errors.ALREADY_PAIRED,
            Envelope.Errors.BAD_TOKEN,
        )
    }
}
