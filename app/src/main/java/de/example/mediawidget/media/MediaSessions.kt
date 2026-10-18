package de.example.mediawidget.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale

/** Stateless helpers around [MediaSessionManager]. */
object MediaSessions {

    private const val TAG = "MediaSessions"
    const val SPOTIFY_PACKAGE = "com.spotify.music"
    private const val MAX_COVER_PX = 128

    fun listenerComponent(context: Context) = ComponentName(context, MediaListenerService::class.java)

    fun hasNotificationAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun activeControllers(context: Context): List<MediaController> {
        if (!hasNotificationAccess(context)) return emptyList()
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return emptyList()
        return try {
            manager.getActiveSessions(listenerComponent(context))
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to read active sessions", e)
            emptyList()
        }
    }

    /**
     * Picks the session to show: Spotify first, then any playing session, then the most
     * recently active one.
     */
    fun select(controllers: List<MediaController>): MediaController? =
        controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE }
            ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            // NOTE: The framework orders active sessions by priority and recent activity,
            // so the first entry is treated as the "last active" session.
            ?: controllers.firstOrNull()

    /** Prefers the session of [packageName] (what the widget currently shows), else [select]. */
    fun find(context: Context, packageName: String?): MediaController? {
        val controllers = activeControllers(context)
        return controllers.firstOrNull { it.packageName == packageName } ?: select(controllers)
    }

    fun readState(context: Context): MediaState {
        if (!hasNotificationAccess(context)) return MediaState.NoAccess
        val controller = select(activeControllers(context)) ?: return MediaState.NoSession
        return controller.toState()
    }

    fun MediaController.isPlaying(): Boolean = when (playbackState?.state) {
        PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING -> true
        else -> false
    }

    private fun MediaController.toState(): MediaState.Active {
        val meta = metadata
        return MediaState.Active(
            packageName = packageName,
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            cover = (meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART))?.downscale(),
            isPlaying = isPlaying(),
        )
    }

    /** Keeps the bitmap small enough for the RemoteViews transaction size limit. */
    private fun Bitmap.downscale(): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= MAX_COVER_PX) return this
        val factor = MAX_COVER_PX.toFloat() / largest
        return scale((width * factor).toInt().coerceAtLeast(1), (height * factor).toInt().coerceAtLeast(1))
    }
}
