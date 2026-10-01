package com.livetype.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livetype.remote.core.discovery.DiscoveredDevice
import com.livetype.remote.core.discovery.NetworkDiscoveryManager
import com.livetype.remote.core.network.BridgeConnection
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.pairing.PairingInfo
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.pairing.QrParseResult
import com.livetype.remote.core.pairing.QrPayloadParser
import com.livetype.remote.core.pairing.SavedPairing
import com.livetype.remote.core.protocol.Envelope
import com.livetype.remote.core.session.StreamSession
import com.livetype.remote.core.voice.VoiceRecognitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class Screen { WELCOME, MANUAL_ENTRY, SCANNER, CONNECTED, DIAGNOSTICS }

enum class InputMode { VOICE_STUDIO, KEYBOARD }

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
    val autoConnectEnabled: Boolean = true,
    val isAutoConnecting: Boolean = false,
    val trustedHost: String? = null,
    val trustedDeviceName: String? = null,
    val language: String = "ar",
    val vibrateOnPair: Boolean = true,
    val pairingJustSucceeded: Boolean = false,

    // Voice Studio State
    val inputMode: InputMode = InputMode.VOICE_STUDIO,
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val voiceRmsLevel: Float = 0f,
    val speechLanguage: String = "ar-SA",
    val partialSpeechText: String = "",

    // LAN Auto-Discovery State
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val isScanningDevices: Boolean = false,
    val selectedDeviceForPin: DiscoveredDevice? = null,
)

/**
 * Orchestrates permanent trusted pairing, auto-discovery, live stream and UI state.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PairingStore(app)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket: stay open
        .pingInterval(0, TimeUnit.SECONDS)    // our own heartbeat instead
        .build()

    private val deviceName = "Android · ${android.os.Build.MODEL.take(24)}"

    private val session = StreamSession(deviceName = deviceName)
    private val discoveryManager = NetworkDiscoveryManager(app, viewModelScope)
    private var connection: BridgeConnection? = null
    private var currentInfo: PairingInfo? = null
    private var currentPcName: String = ""
    private var vibratedThisConnection = false
    private var hasAutoConnectedThisSession = false

    private var committedVoiceText: String = ""
    private var voiceManager: VoiceRecognitionManager? = null

    private val _ui = MutableStateFlow(
        AppUiState(
            savedPairingAvailable = store.load() != null,
            rememberPairingEnabled = store.rememberPairing,
            autoConnectEnabled = store.autoConnectEnabled,
            trustedHost = store.load()?.host,
            trustedDeviceName = store.load()?.deviceName?.ifBlank { store.load()?.host },
            language = store.language,
            vibrateOnPair = store.vibrateOnPair,
        )
    )
    val ui: StateFlow<AppUiState> = _ui

    init {
        initVoiceManager(app)
        initDiscovery()
        tryDirectAutoConnect()
    }

    private fun initDiscovery() {
        val saved = store.load()
        _ui.update {
            it.copy(
                savedPairingAvailable = saved != null && (saved.token.isNotBlank() || saved.pin.isNotBlank()),
                trustedHost = saved?.host,
                trustedDeviceName = saved?.deviceName?.ifBlank { saved.host },
            )
        }

        viewModelScope.launch {
            discoveryManager.devices.collect { list ->
                _ui.update { it.copy(discoveredDevices = list) }
                checkAutoConnect(list)
            }
        }
        viewModelScope.launch {
            discoveryManager.isScanning.collect { scanning ->
                _ui.update { it.copy(isScanningDevices = scanning) }
            }
        }
        discoveryManager.startDiscovery()
    }

    private fun tryDirectAutoConnect() {
        if (hasAutoConnectedThisSession) return
        if (!store.autoConnectEnabled) return
        val saved = store.load() ?: return
        if (saved.token.isBlank() && saved.pin.isBlank()) return

        hasAutoConnectedThisSession = true
        _ui.update { it.copy(isAutoConnecting = true) }
        val wsUrl = "ws://${saved.host}:${saved.port}/livetype"
        beginPairing(
            PairingInfo(wsUrl, saved.host, saved.port, saved.sessionId, saved.pin, 0L),
            token = saved.token.takeIf { it.isNotBlank() },
            pcName = saved.deviceName
        )
    }

    private fun checkAutoConnect(devices: List<DiscoveredDevice>) {
        if (_ui.value.conn == ConnState.PAIRED) return
        if (_ui.value.screen != Screen.WELCOME && _ui.value.screen != Screen.CONNECTED) return
        if (!store.autoConnectEnabled) return
        val saved = store.load() ?: return
        if (saved.token.isBlank() && saved.pin.isBlank()) return
        if (devices.isEmpty()) return

        val match = devices.firstOrNull { it.host.equals(saved.host, ignoreCase = true) }
            ?: devices.firstOrNull()

        if (match != null) {
            hasAutoConnectedThisSession = true
            _ui.update { it.copy(isAutoConnecting = true) }
            val wsUrl = "ws://${match.host}:${match.port}/livetype"
            val sid = match.sessionId ?: saved.sessionId
            beginPairing(
                PairingInfo(wsUrl, match.host, match.port, sid, saved.pin, 0L),
                token = saved.token.takeIf { it.isNotBlank() },
                pcName = match.name
            )
        }
    }

    private fun initVoiceManager(app: Application) {
        val vm = VoiceRecognitionManager(
            context = app,
            onPartialText = { partial ->
                _ui.update { it.copy(partialSpeechText = partial) }
                val combined = buildStreamText(committedVoiceText, partial)
                val env = session.onTextChanged(combined)
                _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
                env?.let { connection?.send(it) }
            },
            onFinalText = { finalChunk ->
                committedVoiceText = buildStreamText(committedVoiceText, finalChunk)
                _ui.update { it.copy(partialSpeechText = "") }
                val env = session.onTextChanged(committedVoiceText)
                _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
                env?.let { connection?.send(it) }
            },
            onErrorOccurred = { errorKey ->
                _ui.update { it.copy(lastError = errorKey) }
            }
        )
        voiceManager = vm

        viewModelScope.launch {
            vm.isRecording.collect { rec ->
                _ui.update { it.copy(isRecording = rec) }
            }
        }
        viewModelScope.launch {
            vm.isListening.collect { listening ->
                _ui.update { it.copy(isListening = listening) }
            }
        }
        viewModelScope.launch {
            vm.rmsLevel.collect { rms ->
                _ui.update { it.copy(voiceRmsLevel = rms) }
            }
        }
    }

    private fun buildStreamText(committed: String, incoming: String): String {
        return if (committed.isEmpty()) {
            incoming
        } else {
            "$committed $incoming"
        }
    }

    /** One-shot flag consumed by the UI to vibrate on successful pairing. */
    val pairSucceeded = MutableStateFlow(false)

    // ---- navigation ---------------------------------------------------------

    fun goScanner() = setScreen(Screen.SCANNER)
    fun goManualEntry() = setScreen(Screen.MANUAL_ENTRY)
    fun goHome() {
        hasAutoConnectedThisSession = false
        discoveryManager.startDiscovery()
        setScreen(Screen.WELCOME)
    }
    fun goDiagnostics() = setScreen(Screen.DIAGNOSTICS)
    fun backFromDiagnostics() =
        setScreen(if (connection?.state?.value?.state == ConnState.PAIRED) Screen.CONNECTED else Screen.WELCOME)

    private fun setScreen(screen: Screen) = _ui.update { it.copy(screen = screen, lastError = null, isAutoConnecting = false) }

    // ---- auto-discovery & trusted pairing controls --------------------------

    fun refreshDiscovery() {
        hasAutoConnectedThisSession = false
        discoveryManager.refresh()
        tryDirectAutoConnect()
    }

    fun isDeviceTrusted(device: DiscoveredDevice): Boolean {
        return store.isDeviceTrusted(device.host, device.port)
    }

    fun onSelectDiscoveredDevice(device: DiscoveredDevice) {
        val saved = store.load()
        if (saved != null && (saved.token.isNotBlank() || saved.pin.isNotBlank())) {
            // Direct 1-tap connect for trusted device with token — zero PIN prompts!
            val wsUrl = "ws://${device.host}:${device.port}/livetype"
            val sid = device.sessionId ?: saved.sessionId
            beginPairing(
                PairingInfo(wsUrl, device.host, device.port, sid, saved.pin, 0L),
                token = saved.token.takeIf { it.isNotBlank() },
                pcName = device.name
            )
        } else {
            _ui.update { it.copy(selectedDeviceForPin = device) }
        }
    }

    fun dismissPinDialog() {
        _ui.update { it.copy(selectedDeviceForPin = null) }
    }

    fun connectDiscoveredWithPin(pin: String) {
        val device = _ui.value.selectedDeviceForPin ?: return
        _ui.update { it.copy(selectedDeviceForPin = null) }
        val sid = device.sessionId ?: "session_${System.currentTimeMillis()}"
        currentPcName = device.name
        connectManual(device.host, device.port, sid, pin)
    }

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
        val result = QrPayloadParser.fromManual(
            host, port, sessionId, pin,
            0L, // 0 = permanent trusted session
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
        beginPairing(
            PairingInfo(saved.wsUrl, saved.host, saved.port, saved.sessionId, saved.pin, 0L),
            token = saved.token.takeIf { it.isNotBlank() },
            pcName = saved.deviceName
        )
    }

    private fun beginPairing(info: PairingInfo, token: String? = null, pcName: String? = null) {
        discoveryManager.stopDiscovery()
        connection?.closeByUser(Envelope.Reasons.REPLACED)
        session.setStreamEnabled(true)
        vibratedThisConnection = false
        currentInfo = info
        if (!pcName.isNullOrBlank()) currentPcName = pcName

        val conn = BridgeConnection(client, viewModelScope, deviceName = deviceName, clientId = store.clientId)
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
                isAutoConnecting = false,
            )
        }
        conn.start(info.wsUrl, info.sessionId, info.pin, info.expiresAtMs, token = token)
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
                if (info != null) {
                    val tok = env.token ?: ""
                    val saved = SavedPairing(
                        wsUrl = info.wsUrl,
                        host = info.host,
                        port = info.port,
                        sessionId = info.sessionId,
                        pin = info.pin,
                        token = tok,
                        expiresAtMs = 0L, // permanent
                        deviceName = currentPcName.ifBlank { info.host },
                    )
                    store.save(saved)
                    _ui.update {
                        it.copy(
                            savedPairingAvailable = true,
                            pairingJustSucceeded = true,
                            trustedHost = saved.host,
                            trustedDeviceName = saved.deviceName
                        )
                    }
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
                if (env.errorCode == Envelope.Errors.BAD_TOKEN) {
                    // Token expired or revoked on PC: reset token to allow fresh PIN pairing
                    store.clear()
                    _ui.update {
                        it.copy(
                            lastError = env.errorMessage ?: "Token expired",
                            lastErrorCode = env.errorCode,
                            savedPairingAvailable = false,
                        )
                    }
                } else {
                    _ui.update { it.copy(lastError = env.errorCode, lastErrorCode = env.errorCode) }
                }
            }
        }
    }

    // ---- live text & voice ---------------------------------------------------

    /** Called from the TextField on every change (dictation included). */
    fun onTextChanged(newText: String) {
        committedVoiceText = newText
        val env = session.onTextChanged(newText)
        _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
        env?.let { connection?.send(it) }
    }

    fun clearText() {
        committedVoiceText = ""
        _ui.update { it.copy(partialSpeechText = "") }
        val env = session.clearText()
        _ui.update { it.copy(text = session.localText, opsSent = session.opsSent, revision = session.revision) }
        env?.let { connection?.send(it) }
    }

    // Voice Studio controls
    fun toggleVoiceRecording() {
        val vm = voiceManager ?: return
        if (vm.isRecording.value) {
            vm.stop()
        } else {
            committedVoiceText = session.localText
            vm.start()
        }
    }

    fun stopVoiceRecording() {
        voiceManager?.stop()
    }

    fun setSpeechLanguage(langCode: String) {
        voiceManager?.setLanguage(langCode)
        _ui.update { it.copy(speechLanguage = langCode) }
    }

    fun toggleSpeechLanguage() {
        val current = _ui.value.speechLanguage
        val next = if (current.startsWith("ar")) "en-US" else "ar-SA"
        setSpeechLanguage(next)
    }

    fun setInputMode(mode: InputMode) {
        if (mode == InputMode.KEYBOARD) {
            voiceManager?.stop()
        }
        _ui.update { it.copy(inputMode = mode) }
    }

    fun appendVoiceSpace() {
        committedVoiceText = if (committedVoiceText.isNotEmpty()) "$committedVoiceText " else " "
        _ui.update { it.copy(partialSpeechText = "") }
        onTextChanged(committedVoiceText)
    }

    fun appendVoiceNewline() {
        committedVoiceText = "$committedVoiceText\n"
        _ui.update { it.copy(partialSpeechText = "") }
        onTextChanged(committedVoiceText)
    }

    fun deleteVoiceLastWord() {
        if (_ui.value.partialSpeechText.isNotEmpty()) {
            _ui.update { it.copy(partialSpeechText = "") }
            onTextChanged(committedVoiceText)
            return
        }
        val trimmed = committedVoiceText.trimEnd()
        val lastBreak = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
        committedVoiceText = if (lastBreak >= 0) {
            trimmed.substring(0, lastBreak)
        } else {
            ""
        }
        onTextChanged(committedVoiceText)
    }

    fun toggleStream() {
        val enable = !session.isStreamEnabled()
        session.setStreamEnabled(enable)
        if (enable) connection?.sendAll(session.resyncAfterPause())
        _ui.update { it.copy(streamingEnabled = enable, opsSent = session.opsSent, revision = session.revision) }
    }

    fun disconnect() {
        hasAutoConnectedThisSession = true // Don't auto-reconnect immediately after explicit disconnect
        voiceManager?.stop()
        connection?.closeByUser()
        connection = null
        session.setStreamEnabled(true)
        discoveryManager.startDiscovery()
        _ui.update {
            it.copy(
                screen = Screen.WELCOME,
                conn = ConnState.DISCONNECTED,
                streamingEnabled = true,
                laptopAddress = "",
                laptopHost = "",
                pairingJustSucceeded = false,
                isRecording = false,
                isListening = false,
                isAutoConnecting = false,
                partialSpeechText = "",
            )
        }
    }

    /** Forgets the saved pairing and its secrets. */
    fun forgetPairing() {
        store.clear()
        _ui.update {
            it.copy(
                savedPairingAvailable = false,
                trustedHost = null,
                trustedDeviceName = null,
            )
        }
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

    fun setAutoConnect(on: Boolean) {
        store.autoConnectEnabled = on
        _ui.update { it.copy(autoConnectEnabled = on) }
    }

    fun consumePairSucceeded() {
        pairSucceeded.value = false
        _ui.update { it.copy(pairingJustSucceeded = false) }
    }

    override fun onCleared() {
        discoveryManager.stopDiscovery()
        voiceManager?.destroy()
        connection?.closeByUser(Envelope.Reasons.PHONE_CLOSED)
        super.onCleared()
    }
}
