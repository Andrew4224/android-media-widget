package de.andrew4224.mediawidget.media

import android.graphics.Bitmap

/** Everything the widget needs to render. */
sealed interface MediaState {
    /** Notification access has not been granted, so sessions cannot be read. */
    data object NoAccess : MediaState

    /** Access granted, but no player currently exposes a media session. */
    data object NoSession : MediaState

    data class Active(
        val packageName: String,
        val title: String?,
        val artist: String?,
        val cover: Bitmap?,
        val isPlaying: Boolean,
    ) : MediaState
}
