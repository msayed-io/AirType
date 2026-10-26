package com.livetype.remote.core.pairing

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createEncryptedPreferences(appContext)

    init {
        migrateLegacyPreferences(appContext, prefs)
    }

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
        return saved.host.equals(host, ignoreCase = true)
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

    // ---- floating bubble ------------------------------------------------------

    /** Bubble diameter in dp; clamped so the icon stays readable and draggable. */
    var bubbleSizeDp: Int
        get() = prefs.getInt(KEY_BUBBLE_SIZE, DEFAULT_BUBBLE_SIZE_DP).coerceIn(MIN_BUBBLE_SIZE_DP, MAX_BUBBLE_SIZE_DP)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_SIZE, value.coerceIn(MIN_BUBBLE_SIZE_DP, MAX_BUBBLE_SIZE_DP)).apply()

    /** Solid bubble colour as ARGB. */
    var bubbleColor: Int
        get() = prefs.getInt(KEY_BUBBLE_COLOR, DEFAULT_BUBBLE_COLOR)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_COLOR, value).apply()

    /** -1 means "never moved": the bubble starts at the trailing edge. */
    var bubbleX: Int
        get() = prefs.getInt(KEY_BUBBLE_X, -1)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_X, value).apply()

    var bubbleY: Int
        get() = prefs.getInt(KEY_BUBBLE_Y, -1)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_Y, value).apply()

    private companion object {
        const val LEGACY_PREFS_NAME = "livetype_trusted_pairing"
        const val SECURE_PREFS_NAME = "livetype_trusted_pairing_secure"
        const val KEY_MIGRATION_COMPLETE = "secure_migration_complete"

        fun createEncryptedPreferences(context: Context): SharedPreferences {
            return runCatching {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrElse {
                context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }

        fun migrateLegacyPreferences(context: Context, secure: SharedPreferences) {
            runCatching {
                if (secure.getBoolean(KEY_MIGRATION_COMPLETE, false)) return
                val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                val editor = secure.edit()
                legacy.all.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
                editor.putBoolean(KEY_MIGRATION_COMPLETE, true).apply()
                legacy.edit().clear().apply()
            }
        }

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
        const val KEY_BUBBLE_SIZE = "overlay_bubble_size"
        const val KEY_BUBBLE_COLOR = "overlay_bubble_color"
        const val KEY_BUBBLE_X = "overlay_bubble_x"
        const val KEY_BUBBLE_Y = "overlay_bubble_y"

        const val DEFAULT_BUBBLE_SIZE_DP = 54
        const val MIN_BUBBLE_SIZE_DP = 44
        const val MAX_BUBBLE_SIZE_DP = 66

        /** Periwinkle deep — the app's signature accent. */
        const val DEFAULT_BUBBLE_COLOR = 0xFF5F7ABF.toInt()
    }
}
