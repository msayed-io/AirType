package com.livetype.remote.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * On-device / cloud voice speech recognition manager using Android's native SpeechRecognizer.
 */
class VoiceRecognitionManager(
    private val context: Context,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private var currentLanguage: String = "ar-SA"

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _isRecording.value = false
            _rmsLevel.value = 0f
            val errCode = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "err_no_speech_match"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "err_network"
                SpeechRecognizer.ERROR_AUDIO -> "err_audio"
                SpeechRecognizer.ERROR_SERVER -> "err_server_closing"
                // The dictation path needs the microphone, not the camera.
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "err_mic_denied"
                else -> "err_generic"
            }
            onErrorOccurred(errCode)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _isRecording.value = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topMatch = matches?.firstOrNull()
            if (!topMatch.isNullOrBlank()) {
                onFinalText(topMatch)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                onPartialText(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorOccurred("err_generic")
            return
        }

        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(listener)
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            _isRecording.value = true
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            _isRecording.value = false
            _isListening.value = false
            onErrorOccurred("err_generic")
        }
    }

    fun stop() {
        _isRecording.value = false
        _isListening.value = false
        _rmsLevel.value = 0f
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun setLanguage(langCode: String) {
        currentLanguage = langCode
        if (_isRecording.value) {
            stop()
            start()
        }
    }

    fun destroy() {
        stop()
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }
}
