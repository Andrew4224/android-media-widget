package de.andrew4224.mediawidget.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import java.io.File

/** Persists the last shown track for when the player's session is gone. */
object LastTrack {

    private const val PREFS = "last_track"
    private const val KEY_PACKAGE = "package"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val LEGACY_COVER_FILE = "last_cover.png"

    @Volatile
    private var savedKey: String? = null

    fun save(context: Context, state: MediaState.Active) {
        if (state.title == null) return
        val key = listOf(state.packageName, state.title, state.artist).joinToString("\u0000")
        if (key == savedKey) return
        synchronized(this) {
            prefs(context).edit {
                putString(KEY_PACKAGE, state.packageName)
                putString(KEY_TITLE, state.title)
                putString(KEY_ARTIST, state.artist)
            }
            File(context.filesDir, LEGACY_COVER_FILE).delete()
            savedKey = key
        }
    }

    fun load(context: Context): MediaState.Active? {
        val prefs = prefs(context)
        val packageName = prefs.getString(KEY_PACKAGE, null) ?: return null
        return MediaState.Active(
            packageName = packageName,
            title = prefs.getString(KEY_TITLE, null),
            artist = prefs.getString(KEY_ARTIST, null),
            cover = CoverCache.get(context, packageName) ?: migrateLegacyCover(
                context,
                packageName
            ),
            isPlaying = false,
            isRemembered = true,
        )
    }

    /** Moves the single cover file of version 0.2 into the [CoverCache]. */
    @Synchronized
    private fun migrateLegacyCover(context: Context, packageName: String): Bitmap? {
        val file = File(context.filesDir, LEGACY_COVER_FILE)
        if (!file.exists()) return null
        val cover = BitmapFactory.decodeFile(file.path)
        file.delete()
        return CoverCache.resolve(context, packageName, cover)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
