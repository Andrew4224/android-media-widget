package de.andrew4224.mediawidget.media

import android.media.session.MediaController
import android.media.session.PlaybackState.STATE_PLAYING
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import de.andrew4224.mediawidget.media.MediaSessions.isPlaying

/** Asks a player to play once it has a session and keeps asking until it actually plays. */
object PendingPlay {

    private const val RETRY_MS = 2_000L

    private data class Request(val packageName: String, val time: Long)

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var request: Request? = null

    @Volatile
    private var retry: Runnable? = null

    fun request(packageName: String) {
        cancel()
        request = Request(packageName, SystemClock.elapsedRealtime())
    }

    fun cancel() {
        request = null
        retry?.let(handler::removeCallbacks)
        retry = null
    }

    fun startIfReady(controllers: List<MediaController>) {
        val pending = request ?: return
        val controller = controllers.firstOrNull { it.packageName == pending.packageName } ?: return
        handler.post { play(pending, controller) }
    }

    private fun play(pending: Request, controller: MediaController) {
        if (request !== pending) return
        retry?.let(handler::removeCallbacks)
        if (SystemClock.elapsedRealtime() - pending.time > StartingPlayback.TIMEOUT_MS ||
            controller.playbackState?.state == STATE_PLAYING
        ) {
            cancel()
            return
        }
        if (!controller.isPlaying()) controller.transportControls.play()
        retry = Runnable { play(pending, controller) }.also { handler.postDelayed(it, RETRY_MS) }
    }
}
