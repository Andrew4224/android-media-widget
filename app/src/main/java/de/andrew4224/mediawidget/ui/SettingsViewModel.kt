package de.andrew4224.mediawidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.andrew4224.mediawidget.AppSettings
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

data class SettingsUiState(
    val hasNotificationAccess: Boolean = false,
    val sessionPackage: String? = null,
    val sessionTitle: String? = null,
    val sessionArtist: String? = null,
    val isRemembered: Boolean = false,
    val hasWidget: Boolean = false,
    val progressInterval: Int = AppSettings.DEFAULT_PROGRESS_INTERVAL,
)

/** Dependencies are passed in as functions so the ViewModel can be unit-tested on the JVM. */
class SettingsViewModel(
    private val checkAccess: () -> Boolean,
    private val checkWidget: () -> Boolean,
    loadProgressInterval: () -> Int,
    private val saveProgressInterval: (Int) -> Unit,
    mediaState: StateFlow<MediaState>,
    private val refreshMedia: () -> Unit,
    private val updateWidgets: suspend () -> Unit,
) : ViewModel() {

    private val hasAccess = MutableStateFlow(checkAccess())
    private val hasWidget = MutableStateFlow(checkWidget())
    private val progressInterval = MutableStateFlow(loadProgressInterval())

    val uiState: StateFlow<SettingsUiState> = combine(
        hasAccess,
        hasWidget,
        progressInterval,
        mediaState,
    ) { access, widget, interval, media ->
        val active = media as? MediaState.Active
        SettingsUiState(
            hasNotificationAccess = access,
            sessionPackage = active?.packageName,
            sessionTitle = active?.title,
            sessionArtist = active?.artist,
            isRemembered = active?.isRemembered == true,
            hasWidget = widget,
            progressInterval = interval,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            hasNotificationAccess = hasAccess.value,
            hasWidget = hasWidget.value,
            progressInterval = progressInterval.value,
        ),
    )

    fun refresh() {
        val access = checkAccess()
        val accessChanged = access != hasAccess.value
        hasAccess.value = access
        hasWidget.value = checkWidget()
        refreshMedia()
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
                    mediaState = MediaRepository.state,
                    refreshMedia = {
                        MediaListenerService.ensureConnected(app)
                        MediaRepository.refresh(app)
                    },
                    updateWidgets = { MediaWidget().updateAll(app) },
                )
            }
        }
    }
}
