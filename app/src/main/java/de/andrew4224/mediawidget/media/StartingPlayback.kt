package de.andrew4224.mediawidget.media

import android.os.SystemClock

/** Remembers a play tap until a player actually plays, so the widget can show a spinner meanwhile. */
object StartingPlayback {

    const val TIMEOUT_MS = 15_000L

    @Volatile
    private var since: Long? = null

    fun start() {
        since = SystemClock.elapsedRealtime()
    }

    fun stop() {
        since = null
    }

    fun apply(state: MediaState): MediaState {
        if (state !is MediaState.Active) return state
        if (state.isPlaying && !state.isStarting) stop()
        return if (isActive()) state.copy(isStarting = true) else state
    }

    private fun isActive(): Boolean {
        val start = since ?: return false
        if (SystemClock.elapsedRealtime() - start <= TIMEOUT_MS) return true
        since = null
        return false
    }
}
