package com.livetype.remote.core.pairing

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/** A pairing the user chose to remember as a permanent trusted device. */
data class SavedPairing(
    val wsUrl: String,
    val host: String,
    val port: Int,
    val sessionId: String,
    val pin: String,
    val token: String,
    val expiresAtMs: Long = 0L,
    val deviceName: String = "",
) {
    /** In permanent pairing mode, credentials do not expire unless explicitly forgotten. */
    val isExpired: Boolean get() = expiresAtMs > 0 && System.currentTimeMillis() >= expiresAtMs
}

/**
 * Stores pairing credentials and trusted device tokens reliably on device.
 * Manages permanent trusted pairing and instant zero-click auto-connect.
 */
class PairingStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("livetype_trusted_pairing", Context.MODE_PRIVATE)

    /** Unique persistent client ID for this Android device. */
    val clientId: String by lazy {
        val existing = prefs.getString(KEY_CLIENT_ID, null)
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val newId = "android_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_CLIENT_ID, newId).apply()
            newId
        }
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
            .putString(KEY_DEVICE_NAME, pairing.deviceName)
            .apply()
    }

    fun load(): SavedPairing? {
        val wsUrl = prefs.getString(KEY_WS_URL, null)?.takeIf { it.isNotBlank() } ?: return null
        val host = prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: return null
        val port = prefs.getInt(KEY_PORT, 0)
        val sid = prefs.getString(KEY_SESSION_ID, "") ?: ""
        val pin = prefs.getString(KEY_PIN, "") ?: ""
        val token = prefs.getString(KEY_TOKEN, "") ?: ""
        val exp = prefs.getLong(KEY_EXPIRES, 0L)
        val devName = prefs.getString(KEY_DEVICE_NAME, "") ?: ""
        if (port !in 1..65535) return null
        return SavedPairing(wsUrl, host, port, sid, pin, token, exp, devName)
    }

    fun isDeviceTrusted(host: String, port: Int = 0): Boolean {
        val saved = load() ?: return false
        if (saved.token.isBlank() && saved.pin.isBlank()) return false
        return saved.host.equals(host, ignoreCase = true) || saved.token.isNotBlank()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_WS_URL)
            .remove(KEY_HOST)
            .remove(KEY_PORT)
            .remove(KEY_SESSION_ID)
            .remove(KEY_PIN)
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES)
            .remove(KEY_DEVICE_NAME)
            .apply()
    }

    // ---- settings -------------------------------------------------------------

    var language: String
        get() = prefs.getString(KEY_LANG, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    var vibrateOnPair: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var rememberPairing: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    var autoConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    private companion object {
        const val KEY_WS_URL = "ws_url"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_PIN = "pin"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_LANG = "language"
        const val KEY_VIBRATE = "vibrate_on_pair"
        const val KEY_REMEMBER = "remember_pairing"
        const val KEY_AUTO_CONNECT = "auto_connect_trusted"
    }
}
