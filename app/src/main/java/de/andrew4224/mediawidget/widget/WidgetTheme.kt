package de.andrew4224.mediawidget.widget

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.annotation.StringRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider as GlanceColorProvider

private val SpotifyGreen = Color(0xFF1DB954)
private val OnSpotifyGreen = Color(0xFF000000)

/** Used when the device has no dynamic (Material You) colors, i.e. below Android 12. */
private val fallbackColors = ColorProviders(
    light = lightColorScheme(primaryContainer = SpotifyGreen, onPrimaryContainer = OnSpotifyGreen),
    dark = darkColorScheme(primaryContainer = SpotifyGreen, onPrimaryContainer = OnSpotifyGreen),
)

/** Darkens whatever is behind it, independent of the color scheme. */
internal val DarkenOverlay = ColorProvider(day = Color(0x29000000), night = Color(0x29000000))

/**
 * Widget colors: the user's system colors (Material You) where available, otherwise Spotify green.
 * The widget uses primaryContainer as background and onPrimaryContainer for content.
 */
@Composable
internal fun WidgetTheme(content: @Composable () -> Unit) {
    if (SDK_INT >= S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(colors = fallbackColors, content = content)
    }
}

/** Widget background. */
internal val primaryContainer: GlanceColorProvider
    @Composable get() = GlanceTheme.colors.primaryContainer

/** Text and icons on [primaryContainer]. */
internal val onPrimaryContainer: GlanceColorProvider
    @Composable get() = GlanceTheme.colors.onPrimaryContainer

/** Glance has no stringResource(); this mirrors the Compose helper. */
@Composable
internal fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String =
    LocalContext.current.getString(id, *formatArgs)
