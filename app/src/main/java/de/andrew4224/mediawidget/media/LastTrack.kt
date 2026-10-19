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
    private const val COVER_FILE = "last_cover.png"

    @Volatile
    private var savedKey: String? = null

    fun save(context: Context, state: MediaState.Active) {
        if (state.title == null) return
        val key = listOf(
            state.packageName,
            state.title,
            state.artist,
            state.cover != null
        ).joinToString("\u0000")
        if (key == savedKey) return
        synchronized(this) {
            val coverFile = coverFile(context)
            if (state.cover != null) {
                coverFile.outputStream()
                    .use { state.cover.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } else {
                coverFile.delete()
            }
            prefs(context).edit {
                putString(KEY_PACKAGE, state.packageName)
                putString(KEY_TITLE, state.title)
                putString(KEY_ARTIST, state.artist)
            }
            savedKey = key
        }
    }

    fun load(context: Context): MediaState.Active? {
        val prefs = prefs(context)
        val packageName = prefs.getString(KEY_PACKAGE, null) ?: return null
        val cover = synchronized(this) {
            coverFile(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        }
        return MediaState.Active(
            packageName = packageName,
            title = prefs.getString(KEY_TITLE, null),
            artist = prefs.getString(KEY_ARTIST, null),
            cover = cover,
            isPlaying = false,
            isRemembered = true,
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun coverFile(context: Context) = File(context.filesDir, COVER_FILE)
}
