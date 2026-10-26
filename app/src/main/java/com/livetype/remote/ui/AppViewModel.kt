package com.livetype.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livetype.remote.core.discovery.DiscoveredDevice
import com.livetype.remote.core.discovery.NetworkDiscoveryManager
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.core.pairing.PairingInfo
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.pairing.QrParseResult
import com.livetype.remote.core.pairing.QrPayloadParser
import com.livetype.remote.core.session.AirTypeSession
import com.livetype.remote.core.session.SessionEvent
import com.livetype.remote.core.session.StreamSession
import com.livetype.remote.core.voice.VoiceRecognitionManager
import com.livetype.remote.service.AirTypeSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { WELCOME, SCANNER, CONNECTED, SETTINGS }

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

    // Floating mode (bubble + writing capsule)
    val bubbleSizeDp: Int = 54,
    val bubbleColor: Int = 0xFF5F7ABF.toInt(),
)

/**
 * Orchestrates permanent trusted pairing, auto-discovery, live stream and UI state.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Application = app
    private val store = PairingStore(app)
    private val discoveryManager = NetworkDiscoveryManager(app, viewModelScope)

    /**
     * The socket and the stream belong to [AirTypeSession] (process scope), not to this
     * view model: that is what lets the connection outlive the screen.
     */
    private val session: StreamSession get() = AirTypeSession.stream
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
            bubbleSizeDp = store.bubbleSizeDp,
            bubbleColor = store.bubbleColor,
        )
    )
    val ui: StateFlow<AppUiState> = _ui

    init {
        AirTypeSession.init(app)
        observeSessionState()
        initVoiceManager(app)
        initDiscovery()
        tryDirectAutoConnect()
    }

    /**
     * Mirrors the process-owned session into screen state. This never opens a socket, so
     * re-creating the activity (rotation, returning from another app) cannot double-send
     * or double-connect.
     */
    private fun observeSessionState() {
        viewModelScope.launch {
            AirTypeSession.snapshot.collect { snap ->
                val saved = store.load()
                _ui.update {
                    it.copy(
                        conn = snap.conn,
                        reconnectAttempt = snap.reconnectAttempt,
                        latencyMs = snap.latencyMs,
                        text = snap.text,
                        opsSent = snap.opsSent,
                        revision = snap.revision,
                        streamingEnabled = snap.streamingEnabled,
                        lastError = snap.lastError,
                        lastErrorCode = snap.lastErrorCode,
                        laptopAddress = if (snap.host.isBlank()) "" else "${snap.host}:${snap.port}",
                        laptopHost = snap.host,
                        sessionExpiresAtMs = snap.sessionExpiresAtMs,
                        savedPairingAvailable = saved != null,
                        trustedHost = saved?.host,
                        trustedDeviceName = saved?.deviceName?.ifBlank { saved.host },
                    )
                }
                if (!snap.hasLiveSession && snap.conn == ConnState.DISCONNECTED &&
                    _ui.value.screen == Screen.CONNECTED
                ) {
                    _ui.update { it.copy(screen = Screen.WELCOME) }
                }
            }
        }
        viewModelScope.launch {
            AirTypeSession.events.collect { event ->
                when (event) {
                    SessionEvent.PAIRED -> {
                        if (!vibratedThisConnection) {
                            vibratedThisConnection = true
                            if (_ui.value.vibrateOnPair) pairSucceeded.value = true
                        }
                        _ui.update { it.copy(pairingJustSucceeded = store.load() != null) }
                    }
                    SessionEvent.SESSION_LOST -> {
                        // The desktop restarted with a fresh runtime sessionId: rediscover
                        // and reconnect from the live beacon (saved token still valid).
                        hasAutoConnectedThisSession = false
                        discoveryManager.startDiscovery()
                        _ui.update { it.copy(isAutoConnecting = true) }
                    }
                    SessionEvent.BAD_TOKEN -> _ui.update { it.copy(savedPairingAvailable = false) }
                }
            }
        }
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
        if (hasAutoConnectedThisSession || !store.autoConnectEnabled) return
        val saved = store.load() ?: return
        if (saved.token.isBlank() && saved.pin.isBlank()) return

        // A desktop restart publishes a fresh runtime sessionId. Connecting with the
        // saved sessionId races discovery and is guaranteed to fail with unknown_session.
        // Keep scanning and let checkAutoConnect use the current beacon instead.
        _ui.update { it.copy(isAutoConnecting = true) }
        discoveryManager.startDiscovery()
    }

    private fun checkAutoConnect(devices: List<DiscoveredDevice>) {
        if (hasAutoConnectedThisSession) return
        if (_ui.value.conn in setOf(ConnState.CONNECTING, ConnState.RECONNECTING, ConnState.PAIRED)) return
        if (_ui.value.screen != Screen.WELCOME && _ui.value.screen != Screen.CONNECTED) return
        if (!store.autoConnectEnabled) return
        val saved = store.load() ?: return
        if (saved.token.isBlank() && saved.pin.isBlank()) return

        // Never offer a trusted bearer token to an arbitrary LAN beacon. Only the
        // previously trusted host may receive it, and the current beacon supplies the
        // fresh runtime sessionId required after Desktop restarts.
        val match = devices.firstOrNull { it.host.equals(saved.host, ignoreCase = true) } ?: return
        val sid = match.sessionId ?: return

        hasAutoConnectedThisSession = true
        _ui.update { it.copy(isAutoConnecting = true) }
        val wsUrl = "ws://${match.host}:${match.port}/livetype"
        beginPairing(
            PairingInfo(
                wsUrl,
                match.host,
                match.port,
                sid,
                pin = saved.pin.takeIf { saved.token.isBlank() }.orEmpty(),
                expiresAtMs = 0L,
            ),
            token = saved.token.takeIf { it.isNotBlank() },
            pcName = match.name
        )
    }

    private fun initVoiceManager(app: Application) {
        val vm = VoiceRecognitionManager(
            context = app,
            onPartialText = { partial ->
                _ui.update { it.copy(partialSpeechText = partial) }
                AirTypeSession.onTextChanged(buildStreamText(committedVoiceText, partial))
            },
            onFinalText = { finalChunk ->
                committedVoiceText = buildStreamText(committedVoiceText, finalChunk)
                _ui.update { it.copy(partialSpeechText = "") }
                AirTypeSession.onTextChanged(committedVoiceText)
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
    fun goHome() {
        hasAutoConnectedThisSession = false
        discoveryManager.startDiscovery()
        setScreen(Screen.WELCOME)
    }
    fun goSettings() = setScreen(Screen.SETTINGS)
    fun backFromSettings() =
        setScreen(if (AirTypeSession.snapshot.value.conn == ConnState.PAIRED) Screen.CONNECTED else Screen.WELCOME)

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
        if (saved != null
            && saved.token.isNotBlank()
            && saved.host.equals(device.host, ignoreCase = true)
            && !device.sessionId.isNullOrBlank()
        ) {
            // Direct 1-tap connect only to the host that owns this trusted token.
            val wsUrl = "ws://${device.host}:${device.port}/livetype"
            beginPairing(
                PairingInfo(wsUrl, device.host, device.port, device.sessionId.orEmpty(), pin = "", expiresAtMs = 0L),
                token = saved.token,
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
        val wsUrl = "ws://${device.host}:${device.port}/livetype"
        beginPairing(
            PairingInfo(wsUrl, device.host, device.port, sid, pin, 0L),
            token = null,
            pcName = device.name
        )
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

    fun reconnectSaved() {
        val saved = store.load() ?: return
        val liveDevice = _ui.value.discoveredDevices.firstOrNull { it.host.equals(saved.host, ignoreCase = true) }
        val sid = liveDevice?.sessionId?.takeIf { it.isNotBlank() } ?: saved.sessionId
        val port = liveDevice?.port ?: saved.port
        val wsUrl = "ws://${saved.host}:$port/livetype"
        beginPairing(
            PairingInfo(wsUrl, saved.host, port, sid, saved.pin, 0L),
            token = saved.token.takeIf { it.isNotBlank() },
            pcName = liveDevice?.name ?: saved.deviceName
        )
    }

    private fun beginPairing(info: PairingInfo, token: String? = null, pcName: String? = null) {
        discoveryManager.stopDiscovery()
        vibratedThisConnection = false
        AirTypeSession.rememberPairing = store.rememberPairing
        AirTypeSession.connect(info, token, pcName)
        // The session service starts here, from a visible user action (pairing/connect),
        // which is the supported moment to start a foreground service. From now on the
        // connection no longer depends on this screen being alive.
        AirTypeSessionService.start(appContext)

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
    }

    // ---- live text & voice ---------------------------------------------------

    /**
     * Called from the phone's text field on every change (dictation included) and, through
     * the capsule's own watcher, from the floating window. Both paths end up in the same
     * stream, so an edit is streamed once no matter where it was typed.
     */
    fun onTextChanged(newText: String) {
        committedVoiceText = newText
        AirTypeSession.onTextChanged(newText)
    }

    fun clearText() {
        committedVoiceText = ""
        _ui.update { it.copy(partialSpeechText = "") }
        AirTypeSession.clearText()
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

    fun toggleStream() = AirTypeSession.toggleStream()

    fun disconnect() {
        hasAutoConnectedThisSession = true // Don't auto-reconnect immediately after explicit disconnect
        voiceManager?.stop()
        AirTypeSession.disconnect()
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
        AirTypeSession.rememberPairing = on
        if (!on) store.clear()
        _ui.update { it.copy(savedPairingAvailable = if (on) it.savedPairingAvailable else false) }
    }

    fun setAutoConnect(on: Boolean) {
        store.autoConnectEnabled = on
        _ui.update { it.copy(autoConnectEnabled = on) }
    }

    /** Bubble diameter in dp; applied to a live bubble without touching the session. */
    fun setBubbleSize(sizeDp: Int) {
        store.bubbleSizeDp = sizeDp
        _ui.update { it.copy(bubbleSizeDp = store.bubbleSizeDp) }
        AirTypeSessionService.refreshOverlay(appContext)
    }

    /** Solid bubble colour; stored locally and applied live. */
    fun setBubbleColor(color: Int) {
        store.bubbleColor = color
        _ui.update { it.copy(bubbleColor = color) }
        AirTypeSessionService.refreshOverlay(appContext)
    }

    fun stopFloatingMode() {
        AirTypeSessionService.hideOverlay(appContext)
    }

    fun consumePairSucceeded() {
        pairSucceeded.value = false
        _ui.update { it.copy(pairingJustSucceeded = false) }
    }

    override fun onCleared() {
        // The connection is deliberately NOT closed here: it belongs to AirTypeSession and
        // the foreground service, so leaving or rotating the screen keeps it alive.
        discoveryManager.stopDiscovery()
        voiceManager?.destroy()
        super.onCleared()
    }
}
