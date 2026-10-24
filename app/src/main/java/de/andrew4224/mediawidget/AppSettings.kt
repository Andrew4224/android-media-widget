package de.andrew4224.mediawidget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import de.andrew4224.mediawidget.media.MediaSessions.SPOTIFY_PACKAGE

/** User preferences from the settings screen. */
object AppSettings {

    private const val PREFS = "settings"
    private const val KEY_PROGRESS_INTERVAL = "progress_interval"
    private const val KEY_KNOWN_PLAYERS = "known_players"
    private const val KEY_IGNORED_PLAYERS = "ignored_players"
    private const val KEY_FAVORITE_PLAYER = "favorite_player"
    private const val KEY_SELECTED_PLAYER = "selected_player"

    /** Selectable progress update intervals in seconds; 0 turns the progress off. */
    val PROGRESS_INTERVALS = listOf(0, 1, 2, 3, 5, 10)
    const val DEFAULT_PROGRESS_INTERVAL = 5

    fun progressInterval(context: Context): Int =
        prefs(context).getInt(KEY_PROGRESS_INTERVAL, DEFAULT_PROGRESS_INTERVAL)

    fun setProgressInterval(context: Context, seconds: Int) =
        prefs(context).edit { putInt(KEY_PROGRESS_INTERVAL, seconds) }

    fun knownPlayers(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_KNOWN_PLAYERS, null).orEmpty()

    @Synchronized
    fun addKnownPlayers(context: Context, packageNames: Collection<String>) {
        val known = knownPlayers(context)
        if (known.containsAll(packageNames)) return
        prefs(context).edit { putStringSet(KEY_KNOWN_PLAYERS, known + packageNames) }
    }

    fun ignoredPlayers(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_IGNORED_PLAYERS, null).orEmpty()

    /** Turning off the favorite also clears it. */
    @Synchronized
    fun setPlayerIgnored(context: Context, packageName: String, ignored: Boolean) {
        val current = ignoredPlayers(context)
        prefs(context).edit {
            putStringSet(
                KEY_IGNORED_PLAYERS,
                if (ignored) current + packageName else current - packageName
            )
            if (ignored && packageName == favoritePlayer(context)) putString(
                KEY_FAVORITE_PLAYER,
                ""
            )
        }
    }

    /** Spotify until the user picks another favorite or none (stored as an empty string). */
    fun favoritePlayer(context: Context): String? =
        prefs(context).getString(KEY_FAVORITE_PLAYER, SPOTIFY_PACKAGE)?.takeIf { it.isNotEmpty() }

    fun setFavoritePlayer(context: Context, packageName: String?) =
        prefs(context).edit { putString(KEY_FAVORITE_PLAYER, packageName.orEmpty()) }

    /** Forgotten players show up again as soon as they create a media session. */
    @Synchronized
    fun forgetPlayers(context: Context, packageNames: Collection<String>) {
        if (packageNames.isEmpty()) return
        val favorite = favoritePlayer(context)
        val selected = selectedPlayer(context)
        prefs(context).edit {
            putStringSet(KEY_KNOWN_PLAYERS, knownPlayers(context) - packageNames.toSet())
            putStringSet(KEY_IGNORED_PLAYERS, ignoredPlayers(context) - packageNames.toSet())
            if (favorite in packageNames) putString(KEY_FAVORITE_PLAYER, "")
            if (selected in packageNames) remove(KEY_SELECTED_PLAYER)
        }
    }

    /** Player picked with the switch button in the widget; it wins as long as it has a session. */
    fun selectedPlayer(context: Context): String? =
        prefs(context).getString(KEY_SELECTED_PLAYER, null)

    fun setSelectedPlayer(context: Context, packageName: String?) =
        prefs(context).edit { putString(KEY_SELECTED_PLAYER, packageName) }

    fun registerListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) =
        prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
