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
    private var widgetPlaced = false
    private var progressInterval = 5
    private val mediaState = MutableStateFlow<MediaState>(MediaState.NoAccess)
    private var refreshMediaCalls = 0
    private var widgetUpdates = 0
    private val knownPlayers = mutableListOf<String>()
    private val ignoredPlayers = mutableSetOf<String>()
    private var favoritePlayer: String? = null
    private val appLabels = mutableMapOf<String, String>()

    private fun createViewModel() = SettingsViewModel(
        checkAccess = { access },
        checkWidget = { widgetPlaced },
        loadProgressInterval = { progressInterval },
        saveProgressInterval = { progressInterval = it },
        loadPlayers = {
            knownPlayers.map {
                PlayerOption(
                    it,
                    it,
                    it in ignoredPlayers,
                    it == favoritePlayer
                )
            }
        },
        savePlayerIgnored = { packageName, ignored ->
            if (ignored) ignoredPlayers += packageName else ignoredPlayers -= packageName
        },
        saveFavoritePlayer = { favoritePlayer = it },
        forgetPlayer = { knownPlayers -= it },
        appLabel = { appLabels[it] },
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
    fun `active session is exposed with package, title and artist`() = runTest {
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
            SettingsUiState(
                hasNotificationAccess = true,
                sessionPackage = "com.spotify.music",
                sessionTitle = "Song",
                sessionArtist = "Artist",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `remembered track is exposed as remembered`() = runTest {
        access = true
        val viewModel = createViewModel()
        collect(viewModel)

        mediaState.value = MediaState.Active(
            "com.spotify.music",
            "Song",
            null,
            null,
            isPlaying = false,
            isRemembered = true
        )

        assertEquals(
            SettingsUiState(
                hasNotificationAccess = true,
                sessionPackage = "com.spotify.music",
                sessionTitle = "Song",
                isRemembered = true,
            ),
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
        val viewModel = createViewModel()
        collect(viewModel)

        access = false
        viewModel.refresh()

        assertFalse(viewModel.uiState.value.hasNotificationAccess)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `refresh picks up a newly placed widget`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        assertFalse(viewModel.uiState.value.hasWidget)

        widgetPlaced = true
        viewModel.refresh()

        assertTrue(viewModel.uiState.value.hasWidget)
    }

    @Test
    fun `changing the progress interval saves it and updates widgets`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        assertEquals(5, viewModel.uiState.value.progressInterval)

        viewModel.setProgressInterval(2)

        assertEquals(2, progressInterval)
        assertEquals(2, viewModel.uiState.value.progressInterval)
        assertEquals(1, refreshMediaCalls)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `progress can be turned off`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.setProgressInterval(0)

        assertEquals(0, progressInterval)
        assertEquals(0, viewModel.uiState.value.progressInterval)
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

    @Test
    fun `refresh picks up newly detected players`() = runTest {
        access = true
        val viewModel = createViewModel()
        collect(viewModel)
        assertTrue(viewModel.uiState.value.players.isEmpty())

        knownPlayers += "com.example.player"
        viewModel.refresh()

        assertEquals(
            listOf(PlayerOption("com.example.player", "com.example.player", isIgnored = false)),
            viewModel.uiState.value.players,
        )
    }

    @Test
    fun `turning a player off ignores it and updates widgets`() = runTest {
        access = true
        knownPlayers += listOf("com.example.player", "com.spotify.music")
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.setPlayerEnabled("com.example.player", enabled = false)

        assertEquals(setOf("com.example.player"), ignoredPlayers)
        assertEquals(
            listOf(true, false),
            viewModel.uiState.value.players.map { it.isIgnored },
        )
        assertEquals(1, refreshMediaCalls)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `turning a player back on stops ignoring it`() = runTest {
        knownPlayers += "com.example.player"
        ignoredPlayers += "com.example.player"
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.setPlayerEnabled("com.example.player", enabled = true)

        assertTrue(ignoredPlayers.isEmpty())
        assertFalse(viewModel.uiState.value.players.single().isIgnored)
    }

    @Test
    fun `choosing a favorite saves it and updates widgets`() = runTest {
        knownPlayers += listOf("com.example.player", "com.spotify.music")
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.setFavorite("com.example.player", favorite = true)

        assertEquals("com.example.player", favoritePlayer)
        assertEquals(listOf(true, false), viewModel.uiState.value.players.map { it.isFavorite })
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `removing the favorite clears it`() = runTest {
        knownPlayers += "com.example.player"
        favoritePlayer = "com.example.player"
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.setFavorite("com.example.player", favorite = false)

        assertNull(favoritePlayer)
        assertFalse(viewModel.uiState.value.players.single().isFavorite)
    }

    @Test
    fun `forgetting a player removes it and updates widgets`() = runTest {
        knownPlayers += listOf("com.example.player", "com.spotify.music")
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.forget("com.example.player")

        assertEquals(
            listOf("com.spotify.music"),
            viewModel.uiState.value.players.map { it.packageName })
        assertEquals(1, refreshMediaCalls)
        assertEquals(1, widgetUpdates)
    }

    @Test
    fun `active session is exposed with the app name`() = runTest {
        access = true
        appLabels["com.spotify.music"] = "Spotify"
        val viewModel = createViewModel()
        collect(viewModel)

        mediaState.value =
            MediaState.Active("com.spotify.music", "Song", null, null, isPlaying = true)

        assertEquals("Spotify", viewModel.uiState.value.sessionLabel)
        assertEquals("com.spotify.music", viewModel.uiState.value.sessionPackage)
    }
}
