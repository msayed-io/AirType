package com.livetype.remote.core.pairing

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** A pairing the user chose to remember (encrypted at rest). */
data class SavedPairing(
    val wsUrl: String,
    val host: String,
    val port: Int,
    val sessionId: String,
    val pin: String,
    val token: String,
    val expiresAtMs: Long,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAtMs
}

/**
 * Stores pairing secrets with EncryptedSharedPreferences (AES-256 key in the
 * Android Keystore). Only data needed to re-pair is kept; "forget" wipes it.
 */
class PairingStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "livetype_pairing_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("livetype_settings", Context.MODE_PRIVATE)
    }

    fun save(pairing: SavedPairing) {
        prefs.edit()
            .putString(KEY_WS_URL, pairing.wsUrl)
            .putString(KEY_HOST, pairing.host)
            .putInt(KEY_PORT, pairing.port)
            .putString(KEY_SESSION_ID, pairing.sessionId)
            .putString(KEY_PIN, pairing.pin)
            .putString(KEY_TOKEN, pairing.token)
            .putLong(KEY_EXPIRES, pairing.expiresAtMs)
            .apply()
    }

    fun load(): SavedPairing? {
        val wsUrl = prefs.getString(KEY_WS_URL, null) ?: return null
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val port = prefs.getInt(KEY_PORT, 0)
        val sid = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val pin = prefs.getString(KEY_PIN, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val exp = prefs.getLong(KEY_EXPIRES, 0L)
        if (port !in 1..65535) return null
        return SavedPairing(wsUrl, host, port, sid, pin, token, exp)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // ---- plain (non-secret) settings -----------------------------------------

    var language: String
        get() = plainPrefs.getString(KEY_LANG, "ar") ?: "ar"
        set(value) = plainPrefs.edit().putString(KEY_LANG, value).apply()

    var vibrateOnPair: Boolean
        get() = plainPrefs.getBoolean(KEY_VIBRATE, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var rememberPairing: Boolean
        get() = plainPrefs.getBoolean(KEY_REMEMBER, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    private companion object {
        const val KEY_WS_URL = "ws_url"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_PIN = "pin"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_LANG = "language"
        const val KEY_VIBRATE = "vibrate_on_pair"
        const val KEY_REMEMBER = "remember_pairing"
    }
}
