package de.andrew4224.mediawidget.ui

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaSessions
import de.andrew4224.mediawidget.media.MediaState
import de.andrew4224.mediawidget.widget.MediaWidget
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
)

/** Dependencies are passed in as functions so the ViewModel can be unit-tested on the JVM. */
class SettingsViewModel(
    private val checkAccess: () -> Boolean,
    mediaState: StateFlow<MediaState>,
    private val refreshMedia: () -> Unit,
    private val updateWidgets: suspend () -> Unit,
) : ViewModel() {

    private val hasAccess = MutableStateFlow(checkAccess())

    val uiState: StateFlow<SettingsUiState> = combine(hasAccess, mediaState) { access, media ->
        val active = media as? MediaState.Active
        SettingsUiState(
            hasNotificationAccess = access,
            sessionPackage = active?.packageName,
            sessionTitle = active?.title,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(hasAccess.value))

    fun refresh() {
        val access = checkAccess()
        val accessChanged = access != hasAccess.value
        hasAccess.value = access
        refreshMedia()
        if (accessChanged) {
            // The widget may still show "grant access" (or vice versa).
            viewModelScope.launch { updateWidgets() }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SettingsViewModel(
                    checkAccess = { MediaSessions.hasNotificationAccess(app) },
                    mediaState = MediaRepository.state,
                    refreshMedia = { MediaRepository.refresh(app) },
                    updateWidgets = { MediaWidget().updateAll(app) },
                )
            }
        }
    }
}
