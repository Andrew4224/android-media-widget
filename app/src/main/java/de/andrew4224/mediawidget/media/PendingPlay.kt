package de.andrew4224.mediawidget.media

import android.media.session.MediaController
import android.os.SystemClock

/** Starts playback as soon as a player opened from the widget has created its session. */
object PendingPlay {

    private const val TIMEOUT_MS = 15_000L

    private data class Request(val packageName: String, val time: Long)

    @Volatile
    private var request: Request? = null

    fun request(packageName: String) {
        request = Request(packageName, SystemClock.elapsedRealtime())
    }

    fun startIfReady(controllers: List<MediaController>) {
        val pending = request ?: return
        if (SystemClock.elapsedRealtime() - pending.time > TIMEOUT_MS) {
            request = null
            return
        }
        val controller = controllers.firstOrNull { it.packageName == pending.packageName } ?: return
        request = null
        controller.transportControls.play()
    }
}
