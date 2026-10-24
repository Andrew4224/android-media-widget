package de.andrew4224.mediawidget.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.andrew4224.mediawidget.AppSettings
import de.andrew4224.mediawidget.R
import de.andrew4224.mediawidget.media.MediaSessions.SPOTIFY_PACKAGE
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onProgressIntervalChange: (Int) -> Unit,
    onOpenPlayers: () -> Unit,
) {
    AppTheme {
        Scaffold(containerColor = colorScheme.surfaceContainer) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.app_name),
                        style = typography.headlineMedium,
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Section(stringResource(R.string.settings_access_title)) {
                    Text(
                        stringResource(
                            if (state.hasNotificationAccess) R.string.settings_access_granted else R.string.settings_access_missing,
                        ),
                    )
                    Text(stringResource(R.string.settings_access_reason), style = typography.bodySmall)
                    Text(
                        stringResource(
                            if (state.hasNotificationAccess) R.string.settings_access_revoke else R.string.settings_access_restricted,
                        ),
                        style = typography.bodySmall,
                    )
                    SettingsButton(
                        stringResource(R.string.settings_open_access),
                        onOpenNotificationSettings
                    )
                }

                if (state.hasNotificationAccess) {
                    Section(stringResource(R.string.settings_session_title)) {
                        if (state.sessionPackage == null) {
                            Text(stringResource(R.string.settings_session_none))
                        } else {
                            if (state.isRemembered) {
                                Text(stringResource(R.string.settings_session_remembered))
                            }
                            Text(
                                stringResource(
                                    R.string.settings_session_package,
                                    state.sessionLabel ?: state.sessionPackage,
                                ),
                            )
                            if (state.sessionLabel != null) {
                                Text(
                                    state.sessionPackage,
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.settings_session_track,
                                    state.sessionTitle
                                        ?: stringResource(R.string.widget_unknown_title),
                                ),
                            )
                            if (state.sessionArtist != null) {
                                Text(
                                    stringResource(
                                        R.string.settings_session_artist,
                                        state.sessionArtist
                                    )
                                )
                            }
                        }
                    }
                }

                if (state.hasNotificationAccess) {
                    Section(stringResource(R.string.settings_players_title)) {
                        if (state.players.isEmpty()) {
                            Text(stringResource(R.string.settings_players_none))
                        } else {
                            val enabled = state.players.count { !it.isIgnored }
                            Text(
                                stringResource(
                                    R.string.settings_players_summary,
                                    enabled,
                                    state.players.size
                                )
                            )
                            Text(
                                state.players.firstOrNull { it.isFavorite }
                                    ?.let {
                                        stringResource(
                                            R.string.settings_players_favorite_summary,
                                            it.label
                                        )
                                    }
                                    ?: stringResource(R.string.settings_players_no_favorite),
                            )
                            if (enabled == 0) {
                                AllPlayersOffWarning()
                            }
                            SettingsButton(
                                stringResource(R.string.settings_manage_players),
                                onOpenPlayers
                            )
                        }
                    }
                }

                Section(stringResource(R.string.settings_progress_title)) {
                    Text(stringResource(R.string.settings_progress_interval))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppSettings.PROGRESS_INTERVALS.forEachIndexed { index, seconds ->
                            SegmentedButton(
                                selected = state.progressInterval == seconds,
                                onClick = { onProgressIntervalChange(seconds) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index,
                                    AppSettings.PROGRESS_INTERVALS.size
                                ),
                                icon = {},
                            ) {
                                Text(
                                    if (seconds == 0) {
                                        stringResource(R.string.settings_progress_off)
                                    } else {
                                        stringResource(R.string.settings_progress_seconds, seconds)
                                    },
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Text(
                        if (state.progressInterval == 0) {
                            stringResource(R.string.settings_progress_cost_off)
                        } else {
                            stringResource(
                                R.string.settings_progress_cost,
                                cpuLoadPercent(state.progressInterval)
                            )
                        },
                        style = typography.bodySmall,
                        color = colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.settings_progress_text),
                        style = typography.bodySmall
                    )
                }

                Section(stringResource(R.string.settings_battery_title)) {
                    Text(stringResource(R.string.settings_battery_text))
                    SettingsButton(stringResource(R.string.settings_open_app_info), onOpenAppInfo)
                }

                if (!state.hasWidget) {
                    Section(stringResource(R.string.settings_howto_title)) {
                        Text(stringResource(R.string.settings_howto_text))
                    }
                }
            }
        }
    }
}

// Measured on a test device: CPU time of app, launcher and system per widget refresh.
private const val REFRESH_CPU_MS = 156

private fun cpuLoadPercent(intervalSeconds: Int): Int =
    (REFRESH_CPU_MS / (intervalSeconds * 10f)).roundToInt().coerceAtLeast(1)

@Composable
internal fun SettingsButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(text)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = typography.titleMedium)
            content()
        }
    }
}

@Composable
internal fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        SDK_INT >= S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

internal val typography: Typography
    @Composable get() = MaterialTheme.typography

internal val colorScheme: ColorScheme
    @Composable get() = MaterialTheme.colorScheme

@Preview(name = "Access granted", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(
        state = SettingsUiState(
            hasNotificationAccess = true,
            sessionPackage = SPOTIFY_PACKAGE,
            sessionLabel = "Spotify",
            sessionTitle = stringResource(R.string.preview_song),
            sessionArtist = stringResource(R.string.preview_artist),
        ),
        onOpenNotificationSettings = {},
        onOpenAppInfo = {},
        onProgressIntervalChange = {},
        onOpenPlayers = {},
    )
}

@Preview(name = "No access", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenNoAccessPreview() {
    SettingsScreen(
        state = SettingsUiState(hasNotificationAccess = false),
        onOpenNotificationSettings = {},
        onOpenAppInfo = {},
        onProgressIntervalChange = {},
        onOpenPlayers = {},
    )
}

@Preview(name = "Remembered track", showBackground = true)
@Composable
private fun SettingsScreenRememberedPreview() {
    SettingsScreen(
        state = SettingsUiState(
            hasNotificationAccess = true,
            sessionPackage = SPOTIFY_PACKAGE,
            sessionLabel = "Spotify",
            sessionTitle = stringResource(R.string.preview_song),
            sessionArtist = stringResource(R.string.preview_artist),
            isRemembered = true,
        ),
        onOpenNotificationSettings = {},
        onOpenAppInfo = {},
        onProgressIntervalChange = {},
        onOpenPlayers = {},
    )
}

@Preview(name = "Widget placed", showBackground = true)
@Composable
private fun SettingsScreenWidgetPlacedPreview() {
    SettingsScreen(
        state = SettingsUiState(
            hasNotificationAccess = true,
            sessionPackage = SPOTIFY_PACKAGE,
            sessionLabel = "Spotify",
            sessionTitle = stringResource(R.string.preview_song),
            sessionArtist = stringResource(R.string.preview_artist),
            hasWidget = true,
            progressInterval = 0,
            players = listOf(
                PlayerOption(SPOTIFY_PACKAGE, "Spotify", isIgnored = false, isFavorite = true),
                PlayerOption("com.google.android.youtube", "YouTube", isIgnored = true),
            ),
        ),
        onOpenNotificationSettings = {},
        onOpenAppInfo = {},
        onProgressIntervalChange = {},
        onOpenPlayers = {},
    )
}
