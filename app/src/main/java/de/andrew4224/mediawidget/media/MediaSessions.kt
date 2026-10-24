package de.andrew4224.mediawidget.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata.METADATA_KEY_ALBUM_ART
import android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST
import android.media.MediaMetadata.METADATA_KEY_ART
import android.media.MediaMetadata.METADATA_KEY_ARTIST
import android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE
import android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE
import android.media.MediaMetadata.METADATA_KEY_DURATION
import android.media.MediaMetadata.METADATA_KEY_TITLE
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState.STATE_BUFFERING
import android.media.session.PlaybackState.STATE_CONNECTING
import android.media.session.PlaybackState.STATE_PLAYING
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import de.andrew4224.mediawidget.AppSettings
import de.andrew4224.mediawidget.media.MediaSessions.select

/** Stateless helpers around [MediaSessionManager]. */
object MediaSessions {

    private const val TAG = "MediaSessions"
    const val SPOTIFY_PACKAGE = "com.spotify.music"
    private const val MAX_COVER_PX = 128

    fun listenerComponent(context: Context) = ComponentName(context, MediaListenerService::class.java)

    fun hasNotificationAccess(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun activeControllers(context: Context): List<MediaController> =
        withoutIgnored(context, allActiveControllers(context))

    /** Whether a player turned off in the settings has a session, which would receive media keys. */
    fun hasIgnoredSession(context: Context): Boolean {
        val ignored = AppSettings.ignoredPlayers(context)
        return allActiveControllers(context).any { it.packageName in ignored }
    }

    private fun allActiveControllers(context: Context): List<MediaController> {
        if (!hasNotificationAccess(context)) return emptyList()
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return emptyList()
        return try {
            manager.getActiveSessions(listenerComponent(context))
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to read active sessions", e)
            emptyList()
        }
    }

    /** Remembers every player seen so far and drops the ones turned off in the settings. */
    fun withoutIgnored(
        context: Context,
        controllers: List<MediaController>
    ): List<MediaController> {
        AppSettings.addKnownPlayers(context, controllers.map { it.packageName })
        val ignored = AppSettings.ignoredPlayers(context)
        return controllers.filter { it.packageName !in ignored }
    }

    /** See [selectSession]. */
    fun select(context: Context, controllers: List<MediaController>): MediaController? =
        selectSession(controllers.map { it.toSession() }, selectionSettings(context))?.handle

    /** Prefers the session of [packageName] (what the widget currently shows), else [select]. */
    fun find(context: Context, packageName: String?): MediaController? {
        val controllers = activeControllers(context)
        return controllers.firstOrNull { it.packageName == packageName } ?: select(
            context,
            controllers
        )
    }

    fun switchPlayer(context: Context, current: String?) {
        nextPlayer(activeControllers(context).map { it.packageName }, current)
            ?.let { AppSettings.setSelectedPlayer(context, it) }
    }

    fun readState(context: Context): MediaState {
        if (!hasNotificationAccess(context)) return MediaState.NoAccess
        val sessions = allActiveControllers(context).map { it.toSession() }
        AppSettings.addKnownPlayers(context, sessions.map { it.packageName })
        val settings = selectionSettings(context)
        val selection = chooseSession(sessions, settings)
        if (!selection.keepSelected) AppSettings.setSelectedPlayer(context, null)
        val controller = selection.session?.handle
            ?: return fallbackState(LastTrack.load(context), selection, settings.ignored)
        return controller.toState(context, withProgress = AppSettings.progressInterval(context) > 0)
            .copy(canSwitchPlayer = selection.canSwitch)
            .also { LastTrack.save(context, it) }
    }

    private fun selectionSettings(context: Context) = SelectionSettings(
        ignored = AppSettings.ignoredPlayers(context),
        favorite = AppSettings.favoritePlayer(context),
        selected = AppSettings.selectedPlayer(context),
    )

    private fun MediaController.toSession() =
        Session(packageName, isPlaying = playbackState?.state == STATE_PLAYING, handle = this)

    fun dispatchMediaKey(context: Context, keyCode: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun MediaController.isPlaying(): Boolean = when (playbackState?.state) {
        STATE_PLAYING, STATE_BUFFERING, STATE_CONNECTING -> true
        else -> false
    }

    private fun MediaController.toState(
        context: Context,
        withProgress: Boolean
    ): MediaState.Active {
        val meta = metadata
        return MediaState.Active(
            packageName = packageName,
            title = meta?.getString(METADATA_KEY_TITLE)
                ?: meta?.getString(METADATA_KEY_DISPLAY_TITLE),
            artist = meta?.getString(METADATA_KEY_ARTIST)
                ?: meta?.getString(METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(METADATA_KEY_DISPLAY_SUBTITLE),
            cover = CoverCache.resolve(
                context,
                packageName,
                (meta?.getBitmap(METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(METADATA_KEY_ART))?.downscale(),
            ),
            isPlaying = isPlaying(),
            progress = if (withProgress) progress() else null,
        )
    }

    private fun MediaController.progress(): Float? {
        val state = playbackState ?: return null
        return playbackProgress(
            position = state.position,
            duration = metadata?.getLong(METADATA_KEY_DURATION) ?: 0,
            elapsedSinceUpdate = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime,
            speed = state.playbackSpeed,
            isPlaying = state.state == STATE_PLAYING,
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
