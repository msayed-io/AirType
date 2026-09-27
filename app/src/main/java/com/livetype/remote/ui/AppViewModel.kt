package com.livetype.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livetype.remote.core.network.BridgeConnection
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.pairing.PairingInfo
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.pairing.QrParseResult
import com.livetype.remote.core.pairing.QrPayloadParser
import com.livetype.remote.core.pairing.SavedPairing
import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.session.StreamSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class Screen { WELCOME, MANUAL_ENTRY, SCANNER, CONNECTED, DIAGNOSTICS }

/** Immutable UI state rendered by Compose. */
data class AppUiState(
    val screen: Screen = Screen.WELCOME,
    val conn: ConnState = ConnState.IDLE,
    val reconnectAttempt: Int = 0,
    val laptopAddress: String = "",
    val laptopHost: String = "",
    val streamingEnabled: Boolean = true,
    val text: String = "",
    val latencyMs: Int? = null,
    val opsSent: Long = 0,
    val revision: Int = 0,
    val sessionExpiresAtMs: Long = 0,
    val lastError: String? = null,
    val lastErrorCode: String? = null,
    val savedPairingAvailable: Boolean = false,
    val rememberPairingEnabled: Boolean = true,
    val language: String = "ar",
    val vibrateOnPair: Boolean = true,
    val pairingJustSucceeded: Boolean = false,
)

/**
 * Orchestrates pairing, the live stream and the UI state. The connection and the
 * session live here (not in composables) so rotation and backgrounding never lose
 * the connection or the text.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PairingStore(app)
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket: stay open
        .pingInterval(0, TimeUnit.SECONDS)    // our own heartbeat instead
        .build()

    private val deviceName = "Android · ${android.os.Build.MODEL.take(24)}"

    private val session = StreamSession(deviceName = deviceName)
    private var connection: BridgeConnection? = null
    private var currentInfo: PairingInfo? = null
    private var vibratedThisConnection = false

    private val _ui = MutableStateFlow(
        AppUiState(
            savedPairingAvailable = store.load() != null,
            language = store.language,
            vibrateOnPair = store.vibrateOnPair,
        )
    )
    val ui: StateFlow<AppUiState> = _ui

    /** One-shot flag consumed by the UI to vibrate on successful pairing. */
    val pairSucceeded = MutableStateFlow(false)

    // ---- navigation ---------------------------------------------------------

    fun goScanner() = setScreen(Screen.SCANNER)
    fun goManualEntry() = setScreen(Screen.MANUAL_ENTRY)
    fun goHome() = setScreen(Screen.WELCOME)
    fun goDiagnostics() = setScreen(Screen.DIAGNOSTICS)
    fun backFromDiagnostics() =
        setScreen(if (connection?.state?.value?.state == ConnState.PAIRED) Screen.CONNECTED else Screen.WELCOME)

    private fun setScreen(screen: Screen) = _ui.update { it.copy(screen = screen, lastError = null) }

    // ---- pairing ------------------------------------------------------------

    /** Entry point from the QR scanner. */
    fun onQrScanned(raw: String) {
        when (val result = QrPayloadParser.parse(raw)) {
            is QrParseResult.Ok -> beginPairing(result.info)
            is QrParseResult.Invalid -> _ui.update {
                it.copy(screen = Screen.SCANNER, lastError = result.reason.name)
            }
        }
    }

    fun connectManual(host: String, port: Int, sessionId: String, pin: String) {
        // Manual entry cannot know the server's real expiry; the server still
        // enforces it, so a generous local window only affects auto-reconnect.
        val result = QrPayloadParser.fromManual(
            host, port, sessionId, pin,
            System.currentTimeMillis() + 12 * 3600_000L,
        )
        when (result) {
            is QrParseResult.Ok -> beginPairing(result.info)
            is QrParseResult.Invalid -> _ui.update {
                it.copy(screen = Screen.MANUAL_ENTRY, lastError = result.reason.name)
            }
        }
    }

    fun reconnectSaved() {
        val saved = store.load() ?: return
        if (saved.isExpired) {
            store.clear()
            _ui.update { it.copy(savedPairingAvailable = false, lastError = "session_expired") }
            return
        }
        beginPairing(
            PairingInfo(saved.wsUrl, saved.host, saved.port, saved.sessionId, saved.pin, saved.expiresAtMs)
        )
    }

    private fun beginPairing(info: PairingInfo) {
        connection?.closeByUser(Envelope.Reasons.REPLACED)
        session.setStreamEnabled(true)
        vibratedThisConnection = false
        currentInfo = info

        val conn = BridgeConnection(client, viewModelScope, deviceName)
        connection = conn
        observeConnection(conn)

        _ui.update {
            it.copy(
                screen = Screen.CONNECTED,
                conn = ConnState.CONNECTING,
                laptopAddress = "${info.host}:${info.port}",
                laptopHost = info.host,
                sessionExpiresAtMs = info.expiresAtMs,
                lastError = null,
                lastErrorCode = null,
                text = session.localText,
                opsSent = session.opsSent,
                revision = session.revision,
                streamingEnabled = true,
                latencyMs = null,
                pairingJustSucceeded = false,
            )
        }
        conn.start(info.wsUrl, info.sessionId, info.pin, info.expiresAtMs)
    }

    private fun observeConnection(conn: BridgeConnection) {
        viewModelScope.launch {
            conn.state.collect { snap ->
                _ui.update {
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
        viewModelScope.launch {
            conn.messages.collect { env -> onServerMessage(env, conn) }
        }
        viewModelScope.launch {
            conn.reconnected.collect {
                // Back online inside the same session: heal mirror + PC field safely.
                conn.sendAll(session.resyncAfterReconnect())
                _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
            }
        }
    }

    private fun onServerMessage(env: Envelope, conn: BridgeConnection) {
        when (env.type) {
            Envelope.Types.SESSION_READY -> {
                if (!vibratedThisConnection) {
                    vibratedThisConnection = true
                    if (_ui.value.vibrateOnPair) pairSucceeded.value = true
                }
                val info = currentInfo
                if (info != null && store.rememberPairing) {
                    store.save(
                        SavedPairing(
                            wsUrl = info.wsUrl,
                            host = info.host,
                            port = info.port,
                            sessionId = info.sessionId,
                            pin = info.pin,
                            token = env.token ?: "",
                            expiresAtMs = info.expiresAtMs,
                        )
                    )
                    _ui.update { it.copy(savedPairingAvailable = true, pairingJustSucceeded = true) }
                }
            }

            Envelope.Types.ACK -> {
                session.onAck(env)
                _ui.update { it.copy(opsSent = session.opsSent, revision = session.revision) }
            }

            Envelope.Types.RESET_STREAM -> {
                // Server lost sync: answer with reset_stream + full rewrite.
                conn.sendAll(session.onServerResetRequest(env))
                _ui.update { it.copy(opsSent = session.opsSent, revision = session.revision) }
            }

            Envelope.Types.DISCONNECT -> {
                _ui.update { it.copy(lastError = "server_closing") }
            }

            Envelope.Types.ERROR -> {
                _ui.update { it.copy(lastError = env.errorCode, lastErrorCode = env.errorCode) }
            }
        }
    }

    // ---- live text ------------------------------------------------------------

    /** Called from the TextField on every change (dictation included). */
    fun onTextChanged(newText: String) {
        val env = session.onTextChanged(newText)
        _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
        env?.let { connection?.send(it) }
    }

    fun clearText() {
        val env = session.clearText()
        _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
        env?.let { connection?.send(it) }
    }

    fun toggleStream() {
        val enable = !session.isStreamEnabled()
        session.setStreamEnabled(enable)
        if (enable) connection?.sendAll(session.resyncAfterPause())
        _ui.update { it.copy(streamingEnabled = enable, opsSent = session.opsSent, revision = session.revision) }
    }

    fun disconnect() {
        connection?.closeByUser()
        connection = null
        session.setStreamEnabled(true)
        _ui.update {
            it.copy(
                screen = Screen.WELCOME,
                conn = ConnState.DISCONNECTED,
                streamingEnabled = true,
                laptopAddress = "",
                laptopHost = "",
                pairingJustSucceeded = false,
            )
        }
    }

    /** Forgets the saved pairing and its secrets. */
    fun forgetPairing() {
        store.clear()
        _ui.update { it.copy(savedPairingAvailable = false) }
    }

    // ---- settings -------------------------------------------------------------

    fun setLanguage(lang: String) {
        store.language = lang
        _ui.update { it.copy(language = lang) }
    }

    fun setVibrate(on: Boolean) {
        store.vibrateOnPair = on
        _ui.update { it.copy(vibrateOnPair = on) }
    }

    fun setRememberPairing(on: Boolean) {
        store.rememberPairing = on
        if (!on) store.clear()
        _ui.update { it.copy(savedPairingAvailable = if (on) it.savedPairingAvailable else false) }
    }

    fun consumePairSucceeded() {
        pairSucceeded.value = false
        _ui.update { it.copy(pairingJustSucceeded = false) }
    }

    override fun onCleared() {
        connection?.closeByUser(Envelope.Reasons.PHONE_CLOSED)
        super.onCleared()
    }
}
