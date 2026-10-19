package de.andrew4224.mediawidget.media

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import androidx.core.content.ContextCompat
import de.andrew4224.mediawidget.AppSettings
import de.andrew4224.mediawidget.media.MediaSessions.isPlaying

/**
 * Exists only so the app may call [MediaSessionManager.getActiveSessions]; notifications
 * themselves are ignored. While connected it follows the selected session and refreshes the widget.
 */
class MediaListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private var sessionManager: MediaSessionManager? = null
    private var trackedController: MediaController? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        track(MediaSessions.select(controllers.orEmpty()))
        MediaRepository.refreshAndUpdateWidgets(this)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)
            updateProgressTicker()
        }

        override fun onSessionDestroyed() {
            track(null)
            MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)
        }
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)
            updateProgressTicker()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            syncTracking()
            MediaRepository.refreshAndUpdateWidgets(context)
            updateProgressTicker()
        }
    }

    private val settingsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> updateProgressTicker() }

    override fun onListenerConnected() {
        isConnected = true
        AppSettings.registerListener(this, settingsListener)
        val screenEvents = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            screenEvents,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val manager = getSystemService(MediaSessionManager::class.java)
        sessionManager = manager
        manager.addOnActiveSessionsChangedListener(sessionsListener, MediaSessions.listenerComponent(this), handler)
        syncTracking()
        handler.postDelayed(::syncTracking, RESYNC_DELAY_MS)
        MediaRepository.refreshAndUpdateWidgets(this)
    }

    override fun onListenerDisconnected() {
        isConnected = false
        AppSettings.unregisterListener(this, settingsListener)
        runCatching { unregisterReceiver(screenReceiver) }
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        sessionManager = null
        track(null)
        MediaRepository.refreshAndUpdateWidgets(this)
        ensureConnected(this)
    }

    private fun syncTracking() {
        if (!isConnected) return
        track(MediaSessions.select(MediaSessions.activeControllers(this)))
    }

    private fun track(controller: MediaController?) {
        if (controller?.sessionToken == trackedController?.sessionToken) return
        trackedController?.unregisterCallback(controllerCallback)
        trackedController = controller
        controller?.registerCallback(controllerCallback, handler)
        updateProgressTicker()
    }

    private fun updateProgressTicker() {
        handler.removeCallbacks(progressTicker)
        val interval = AppSettings.progressInterval(this)
        val unlocked = getSystemService(PowerManager::class.java).isInteractive &&
                !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (interval > 0 && unlocked && trackedController?.isPlaying() == true) {
            handler.postDelayed(progressTicker, interval * 1_000L)
        }
    }

    companion object {
        private const val RESYNC_DELAY_MS = 1_000L

        @Volatile
        var isConnected = false
            private set

        fun ensureConnected(context: Context) {
            if (!isConnected && MediaSessions.hasNotificationAccess(context)) {
                requestRebind(MediaSessions.listenerComponent(context))
            }
        }
    }
}
