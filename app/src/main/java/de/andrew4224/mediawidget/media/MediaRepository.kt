package de.andrew4224.mediawidget.media

import android.content.Context
import androidx.glance.appwidget.updateAll
import de.andrew4224.mediawidget.widget.MediaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-memory holder of the current [MediaState]. The state is always re-read from the framework,
 * so losing it on process death is harmless.
 */
object MediaRepository {

    private const val UPDATE_DEBOUNCE_MS = 250L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingUpdate: Job? = null

    private val _state = MutableStateFlow<MediaState>(MediaState.NoSession)
    val state: StateFlow<MediaState> = _state.asStateFlow()

    fun refresh(context: Context) {
        _state.value = MediaSessions.readState(context)
    }

    /** Re-reads the state and pushes it to all widgets; bursts of calls are coalesced. */
    fun refreshAndUpdateWidgets(context: Context) {
        val appContext = context.applicationContext
        synchronized(this) {
            pendingUpdate?.cancel()
            pendingUpdate = scope.launch {
                delay(UPDATE_DEBOUNCE_MS.milliseconds)
                refresh(appContext)
                MediaWidget().updateAll(appContext)
            }
        }
    }
}
