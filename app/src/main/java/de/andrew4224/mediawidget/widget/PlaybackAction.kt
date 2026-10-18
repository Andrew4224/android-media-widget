package de.andrew4224.mediawidget.widget

import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.andrew4224.mediawidget.media.MediaListenerService
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaSessions
import de.andrew4224.mediawidget.media.MediaSessions.isPlaying
import de.andrew4224.mediawidget.media.MediaState

/** Handles the previous / play-pause / next buttons. */
class PlaybackAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // If the system unbound our listener (e.g. after a long idle period), ask for a rebind
        // so widget updates resume. Transport controls below work regardless.
        if (!MediaListenerService.isConnected && MediaSessions.hasNotificationAccess(context)) {
            NotificationListenerService.requestRebind(MediaSessions.listenerComponent(context))
        }

        val shownPackage = (MediaRepository.state.value as? MediaState.Active)?.packageName
        val controller = MediaSessions.find(context, shownPackage)
        if (controller == null) {
            Log.i(TAG, "No media session for command ${parameters[COMMAND]}")
        } else {
            val controls = controller.transportControls
            when (parameters[COMMAND]) {
                PREVIOUS -> controls.skipToPrevious()
                NEXT -> controls.skipToNext()
                PLAY_PAUSE -> if (controller.isPlaying()) controls.pause() else controls.play()
            }
        }
        MediaRepository.refreshAndUpdateWidgets(context)
    }

    companion object {
        private const val TAG = "PlaybackAction"
        val COMMAND = ActionParameters.Key<String>("command")
        const val PREVIOUS = "previous"
        const val PLAY_PAUSE = "play_pause"
        const val NEXT = "next"
    }
}
