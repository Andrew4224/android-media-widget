package de.andrew4224.mediawidget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** User preferences from the settings screen. */
object AppSettings {

    private const val PREFS = "settings"
    private const val KEY_PROGRESS_INTERVAL = "progress_interval"

    /** Selectable progress update intervals in seconds; 0 turns the progress off. */
    val PROGRESS_INTERVALS = listOf(0, 1, 2, 3, 5, 10)
    const val DEFAULT_PROGRESS_INTERVAL = 5

    fun progressInterval(context: Context): Int =
        prefs(context).getInt(KEY_PROGRESS_INTERVAL, DEFAULT_PROGRESS_INTERVAL)

    fun setProgressInterval(context: Context, seconds: Int) =
        prefs(context).edit { putInt(KEY_PROGRESS_INTERVAL, seconds) }

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
