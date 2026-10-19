package de.andrew4224.mediawidget.widget

import android.content.Context
import android.util.Log
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
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
        MediaListenerService.ensureConnected(context)

        val shownPackage = (MediaRepository.state.value as? MediaState.Active)?.packageName
        val controller = MediaSessions.find(context, shownPackage)
        if (controller == null) {
            Log.i(TAG, "No media session for command ${parameters[COMMAND]}, sending media key")
            val keyCode = when (parameters[COMMAND]) {
                PREVIOUS -> KEYCODE_MEDIA_PREVIOUS
                NEXT -> KEYCODE_MEDIA_NEXT
                else -> KEYCODE_MEDIA_PLAY
            }
            MediaSessions.dispatchMediaKey(context, keyCode)
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
