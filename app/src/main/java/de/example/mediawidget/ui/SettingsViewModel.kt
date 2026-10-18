package de.example.mediawidget.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.example.mediawidget.media.MediaRepository
import de.example.mediawidget.media.MediaSessions
import de.example.mediawidget.media.MediaState
import de.example.mediawidget.widget.MediaWidget
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

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val hasAccess = MutableStateFlow(MediaSessions.hasNotificationAccess(application))

    val uiState: StateFlow<SettingsUiState> = combine(hasAccess, MediaRepository.state) { access, media ->
        val active = media as? MediaState.Active
        SettingsUiState(
            hasNotificationAccess = access,
            sessionPackage = active?.packageName,
            sessionTitle = active?.title,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(hasAccess.value))

    fun refresh() {
        val app = getApplication<Application>()
        val access = MediaSessions.hasNotificationAccess(app)
        val accessChanged = access != hasAccess.value
        hasAccess.value = access
        MediaRepository.refresh(app)
        if (accessChanged) {
            viewModelScope.launch { MediaWidget().updateAll(app) }
        }
    }
}
