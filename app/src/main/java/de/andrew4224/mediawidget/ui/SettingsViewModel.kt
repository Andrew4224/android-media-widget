package de.andrew4224.mediawidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.andrew4224.mediawidget.AppSettings
import de.andrew4224.mediawidget.media.CoverCache
import de.andrew4224.mediawidget.media.MediaListenerService
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaSessions
import de.andrew4224.mediawidget.media.MediaState
import de.andrew4224.mediawidget.widget.MediaWidget
import de.andrew4224.mediawidget.widget.MediaWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerOption(
    val packageName: String,
    val label: String,
    val isIgnored: Boolean,
    val isFavorite: Boolean = false,
)

data class SettingsUiState(
    val hasNotificationAccess: Boolean = false,
    val sessionPackage: String? = null,
    val sessionLabel: String? = null,
    val sessionTitle: String? = null,
    val sessionArtist: String? = null,
    val isRemembered: Boolean = false,
    val hasWidget: Boolean = false,
    val progressInterval: Int = AppSettings.DEFAULT_PROGRESS_INTERVAL,
    val players: List<PlayerOption> = emptyList(),
)

/** Dependencies are passed in as functions so the ViewModel can be unit-tested on the JVM. */
class SettingsViewModel(
    private val checkAccess: () -> Boolean,
    private val checkWidget: () -> Boolean,
    loadProgressInterval: () -> Int,
    private val saveProgressInterval: (Int) -> Unit,
    private val loadPlayers: () -> List<PlayerOption>,
    private val savePlayerIgnored: (String, Boolean) -> Unit,
    private val saveFavoritePlayer: (String?) -> Unit,
    private val forgetPlayer: (String) -> Unit,
    private val appLabel: (String) -> String?,
    mediaState: StateFlow<MediaState>,
    private val refreshMedia: () -> Unit,
    private val updateWidgets: suspend () -> Unit,
) : ViewModel() {

    private val hasAccess = MutableStateFlow(checkAccess())
    private val hasWidget = MutableStateFlow(checkWidget())
    private val progressInterval = MutableStateFlow(loadProgressInterval())
    private val players = MutableStateFlow(loadPlayers())

    val uiState: StateFlow<SettingsUiState> = combine(
        hasAccess,
        hasWidget,
        progressInterval,
        players,
        mediaState,
    ) { access, widget, interval, playerList, media ->
        val active = media as? MediaState.Active
        SettingsUiState(
            hasNotificationAccess = access,
            sessionPackage = active?.packageName,
            sessionLabel = active?.packageName?.let(appLabel),
            sessionTitle = active?.title,
            sessionArtist = active?.artist,
            isRemembered = active?.isRemembered == true,
            hasWidget = widget,
            progressInterval = interval,
            players = playerList,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            hasNotificationAccess = hasAccess.value,
            hasWidget = hasWidget.value,
            progressInterval = progressInterval.value,
            players = players.value,
        ),
    )

    fun refresh() {
        val access = checkAccess()
        val accessChanged = access != hasAccess.value
        hasAccess.value = access
        hasWidget.value = checkWidget()
        refreshMedia()
        players.value = loadPlayers()
        if (accessChanged) {
            // The widget may still show "grant access" (or vice versa).
            viewModelScope.launch { updateWidgets() }
        }
    }

    fun setProgressInterval(seconds: Int) {
        saveProgressInterval(seconds)
        progressInterval.value = seconds
        refreshMedia()
        viewModelScope.launch { updateWidgets() }
    }

    fun setPlayerEnabled(packageName: String, enabled: Boolean) =
        changePlayers { savePlayerIgnored(packageName, !enabled) }

    fun setFavorite(packageName: String, favorite: Boolean) =
        changePlayers { saveFavoritePlayer(packageName.takeIf { favorite }) }

    fun forget(packageName: String) = changePlayers { forgetPlayer(packageName) }

    private fun changePlayers(change: () -> Unit) {
        change()
        refreshMedia()
        players.value = loadPlayers()
        viewModelScope.launch { updateWidgets() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SettingsViewModel(
                    checkAccess = { MediaSessions.hasNotificationAccess(app) },
                    checkWidget = {
                        AppWidgetManager.getInstance(app)
                            .getAppWidgetIds(ComponentName(app, MediaWidgetReceiver::class.java))
                            .isNotEmpty()
                    },
                    loadProgressInterval = { AppSettings.progressInterval(app) },
                    saveProgressInterval = { AppSettings.setProgressInterval(app, it) },
                    loadPlayers = { loadPlayers(app) },
                    savePlayerIgnored = { packageName, ignored ->
                        AppSettings.setPlayerIgnored(app, packageName, ignored)
                    },
                    saveFavoritePlayer = { AppSettings.setFavoritePlayer(app, it) },
                    forgetPlayer = { forgetPlayers(app, listOf(it)) },
                    appLabel = { packageName ->
                        runCatching {
                            app.packageManager.getApplicationInfo(
                                packageName,
                                0
                            )
                        }.getOrNull()
                            ?.let { app.packageManager.getApplicationLabel(it).toString() }
                    },
                    mediaState = MediaRepository.state,
                    refreshMedia = {
                        MediaListenerService.ensureConnected(app)
                        MediaRepository.refresh(app)
                    },
                    updateWidgets = { MediaWidget().updateAll(app) },
                )
            }
        }

        /** Known players sorted by name; uninstalled ones are forgotten on the way. */
        private fun loadPlayers(context: Context): List<PlayerOption> {
            val packageManager = context.packageManager
            val ignored = AppSettings.ignoredPlayers(context)
            val favorite = AppSettings.favoritePlayer(context)
            val (installed, uninstalled) = AppSettings.knownPlayers(context)
                .map { it to runCatching { packageManager.getApplicationInfo(it, 0) }.getOrNull() }
                .partition { it.second != null }
            forgetPlayers(context, uninstalled.map { it.first })
            return installed.map { (packageName, info) ->
                PlayerOption(
                    packageName = packageName,
                    label = packageManager.getApplicationLabel(checkNotNull(info)).toString(),
                    isIgnored = packageName in ignored,
                    isFavorite = packageName == favorite,
                )
            }.sortedBy { it.label.lowercase() }
        }

        private fun forgetPlayers(context: Context, packageNames: List<String>) {
            AppSettings.forgetPlayers(context, packageNames)
            packageNames.forEach { CoverCache.forget(context, it) }
        }
    }
}
