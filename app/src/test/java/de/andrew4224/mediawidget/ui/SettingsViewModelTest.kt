package de.andrew4224.mediawidget.ui

import de.andrew4224.mediawidget.media.MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var access = false
    private val mediaState = MutableStateFlow<MediaState>(MediaState.NoAccess)
    private var refreshMediaCalls = 0
    private var widgetUpdates = 0

    private fun createViewModel() = SettingsViewModel(
        checkAccess = { access },
        mediaState = mediaState,
        refreshMedia = { refreshMediaCalls++ },
        updateWidgets = { widgetUpdates++ },
    )

    /** uiState only updates while collected (WhileSubscribed), like in the real UI. */
    private fun TestScope.collect(viewModel: SettingsViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
    }

    @Test
    fun `initial state reflects missing access and no session`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        assertEquals(SettingsUiState(hasNotificationAccess = false), viewModel.uiState.value)
    }

    @Test
    fun `initial state reflects granted access`() = runTest {
        access = true
        mediaState.value = MediaState.NoSession
        val viewModel = createViewModel()
        collect(viewModel)

        assertTrue(viewModel.uiState.value.hasNotificationAccess)
        assertNull(viewModel.uiState.value.sessionPackage)
    }

    @Test
    fun `active session is exposed with package and title`() = runTest {
        access = true
        val viewModel = createViewModel()
        collect(viewModel)

        mediaState.value = MediaState.Active(
            packageName = "com.spotify.music",
            title = "Song",
            artist = "Artist",
            cover = null,
            isPlaying = true,
        )

        assertEquals(
            SettingsUiState(hasNotificationAccess = true, sessionPackage = "com.spotify.music", sessionTitle = "Song"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `session disappears when playback ends`() = runTest {
        access = true
        mediaState.value = MediaState.Active("com.example.player", "Song", null, null, isPlaying = false)
        val viewModel = createViewModel()
        collect(viewModel)

        mediaState.value = MediaState.NoSession

        assertNull(viewModel.uiState.value.sessionPackage)
        assertNull(viewModel.uiState.value.sessionTitle)
    }

    @Test
    fun `refresh picks up newly granted access and updates widgets`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        access = true
        viewModel.refresh()

        assertTrue(viewModel.uiState.value.hasNotificationAccess)
        assertEquals(1, refreshMediaCalls)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `refresh picks up revoked access`() = runTest {
        access = true
        val viewModel = createViewModel()/** Dependencies are passed in as functions so the ViewModel can be unit-tested on the JVM. */

        collect(viewModel)

        access = false
        viewModel.refresh()

        assertFalse(viewModel.uiState.value.hasNotificationAccess)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `refresh without access change does not update widgets`() = runTest {
        access = true
        val viewModel = createViewModel()

        viewModel.refresh()
        viewModel.refresh()

        assertEquals(2, refreshMediaCalls)
        assertEquals(0, widgetUpdates)
    }
}
