package com.livetype.remote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.livetype.remote.MainActivity
import com.livetype.remote.R
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.session.AirTypeSession
import com.livetype.remote.core.session.SessionSnapshot
import com.livetype.remote.core.network.ConnState
import com.livetype.remote.ui.Strings
import com.livetype.remote.ui.overlay.FloatingOverlayController
import com.livetype.remote.ui.overlay.OverlayPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the AirType session alive independently of any screen.
 *
 * The connection itself lives in [AirTypeSession]; this service exists so Android keeps the
 * process running (and reports the connection honestly in the notification shade) while the
 * user works in another app. It also owns the floating bubble, because windows must outlive
 * the activity that started them.
 *
 * The type is `connectedDevice`: the app maintains a network link to the user's own paired
 * computer, which is exactly what that type describes, and unlike `dataSync` it has no
 * six-hour daily cap (see docs/android-background-overlay-baseline.md).
 */
class AirTypeSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: FloatingOverlayController? = null
    private var collector: Job? = null
    private var everHadSession = false
    private var foregroundStarted = false

    private val store by lazy { PairingStore(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        AirTypeSession.init(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AirTypeSession.init(applicationContext)
        // Every entry point arrives through startForegroundService (the notification action
        // included), so the foreground state must be entered before anything else happens.
        startAsForeground()
        when (intent?.action ?: ACTION_START) {
            ACTION_DISCONNECT -> {
                AirTypeSession.disconnect()
                hideOverlay()
                stopSession()
                return START_NOT_STICKY
            }
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_REFRESH_OVERLAY -> overlay?.applyAppearance()
            else -> Unit
        }
        observeSession()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        collector?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    // ---- foreground ------------------------------------------------------------

    private fun startAsForeground() {
        val notification = buildNotification(AirTypeSession.snapshot.value)
        if (!foregroundStarted) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                },
            )
            foregroundStarted = true
        } else {
            notificationManager()?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun observeSession() {
        if (collector?.isActive == true) return
        collector = scope.launch {
            AirTypeSession.snapshot.collect { snapshot ->
                if (snapshot.hasLiveSession) everHadSession = true
                notificationManager()?.notify(NOTIFICATION_ID, buildNotification(snapshot))
                overlay?.updateFromSnapshot(snapshot)
                val ended = !snapshot.hasLiveSession &&
                    snapshot.conn != ConnState.CONNECTING &&
                    (everHadSession || snapshot.conn == ConnState.DISCONNECTED || snapshot.conn == ConnState.FAILED)
                if (ended) {
                    // The session is over: never leave a bubble pretending otherwise.
                    hideOverlay()
                    stopSession()
                }
            }
        }
    }

    private fun stopSession() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    // ---- overlay ----------------------------------------------------------------

    private fun showOverlay() {
        if (!OverlayPermission.canDraw(this)) return
        val controller = overlay ?: FloatingOverlayController(applicationContext).also { overlay = it }
        controller.showBubble(AirTypeSession.snapshot.value)
    }

    private fun hideOverlay() {
        overlay?.hideBubble()
    }

    // ---- notification ------------------------------------------------------------

    private fun notificationManager(): NotificationManager? =
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val strings = Strings.forLang(store.language)
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings["notif_channel_name"] ?: "AirType session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = strings["notif_channel_desc"] ?: ""
            setShowBadge(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun statusText(snapshot: SessionSnapshot): String {
        val strings = Strings.forLang(store.language)
        val key = when (snapshot.conn) {
            ConnState.PAIRED -> if (snapshot.streamingEnabled) "notif_connected" else "notif_paused"
            ConnState.CONNECTING -> "notif_connecting"
            ConnState.RECONNECTING -> "notif_reconnecting"
            else -> "notif_connecting"
        }
        return strings[key] ?: key
    }

    private fun buildNotification(snapshot: SessionSnapshot): Notification {
        val strings = Strings.forLang(store.language)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // getForegroundService (API 26+, our minSdk) rather than getService: the tap lands
        // while the app is in the background, and onStartCommand re-enters the foreground
        // state immediately, so no background-start restriction is tripped.
        val disconnect = PendingIntent.getForegroundService(
            this,
            1,
            Intent(this, AirTypeSessionService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val laptop = snapshot.laptopName.ifBlank { snapshot.host }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_floating_keyboard)
            .setContentTitle(statusText(snapshot))
            .setContentText(laptop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .addAction(0, strings["notif_disconnect_action"] ?: "Disconnect", disconnect)
            .build()
    }

    companion object {
        const val ACTION_START = "com.livetype.remote.action.START_SESSION"
        const val ACTION_SHOW_OVERLAY = "com.livetype.remote.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.livetype.remote.action.HIDE_OVERLAY"
        const val ACTION_REFRESH_OVERLAY = "com.livetype.remote.action.REFRESH_OVERLAY"
        const val ACTION_DISCONNECT = "com.livetype.remote.action.DISCONNECT"

        private const val CHANNEL_ID = "airtype_session"
        private const val NOTIFICATION_ID = 4101

        /**
         * Starts (or re-reaches) the foreground session service. Only ever called from a
         * visible activity, so it never trips the background-start restrictions.
         */
        fun start(context: Context) = send(context, ACTION_START)

        fun showOverlay(context: Context) = send(context, ACTION_SHOW_OVERLAY)

        fun hideOverlay(context: Context) = send(context, ACTION_HIDE_OVERLAY)

        fun refreshOverlay(context: Context) = send(context, ACTION_REFRESH_OVERLAY)

        fun disconnect(context: Context) = send(context, ACTION_DISCONNECT)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, AirTypeSessionService::class.java).setAction(action)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }
}
