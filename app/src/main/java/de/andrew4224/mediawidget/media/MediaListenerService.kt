package de.andrew4224.mediawidget.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService

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

        override fun onPlaybackStateChanged(state: PlaybackState?) = MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)

        override fun onSessionDestroyed() {
            track(null)
            MediaRepository.refreshAndUpdateWidgets(this@MediaListenerService)
        }
    }

    override fun onListenerConnected() {
        isConnected = true
        val manager = getSystemService(MediaSessionManager::class.java)
        sessionManager = manager
        manager.addOnActiveSessionsChangedListener(sessionsListener, MediaSessions.listenerComponent(this), handler)
        track(MediaSessions.select(MediaSessions.activeControllers(this)))
        MediaRepository.refreshAndUpdateWidgets(this)
    }

    override fun onListenerDisconnected() {
        isConnected = false
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        sessionManager = null
        track(null)
        MediaRepository.refreshAndUpdateWidgets(this)
    }

    private fun track(controller: MediaController?) {
        if (controller?.sessionToken == trackedController?.sessionToken) return
        trackedController?.unregisterCallback(controllerCallback)
        trackedController = controller
        controller?.registerCallback(controllerCallback, handler)
    }

    companion object {
        @Volatile
        var isConnected = false
            private set
    }
}
