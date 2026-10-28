package de.andrew4224.mediawidget.media

import android.graphics.Bitmap

/** Everything the widget needs to render. */
sealed interface MediaState {
    /** Notification access has not been granted, so sessions cannot be read. */
    data object NoAccess : MediaState

    /** Access granted, but no player exposes a media session and no track was remembered yet. */
    data object NoSession : MediaState

    data class Active(
        val packageName: String,
        val title: String?,
        val artist: String?,
        val cover: Bitmap?,
        val isPlaying: Boolean,
        /** Play was tapped or the player is buffering, but it does not play yet. */
        val isStarting: Boolean = false,
        val isRemembered: Boolean = false,
        /** A turned-off player holds the media keys, so play has to open [packageName] instead. */
        val isIgnoredPlayerConnected: Boolean = false,
        /** More than one allowed player has a session, so the widget offers to switch. */
        val canSwitchPlayer: Boolean = false,
        val progress: Float? = null,
    ) : MediaState
}
