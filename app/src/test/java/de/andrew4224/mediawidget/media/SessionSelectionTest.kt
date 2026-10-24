package de.andrew4224.mediawidget.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSelectionTest {

    private fun session(packageName: String, isPlaying: Boolean = false) =
        Session(packageName, isPlaying, packageName)

    private fun remembered(packageName: String) =
        MediaState.Active(
            packageName,
            "Song",
            "Artist",
            cover = null,
            isPlaying = false,
            isRemembered = true
        )

    private val spotify = session(SPOTIFY)
    private val podcasts = session(PODCASTS)
    private val youtube = session(YOUTUBE)

    @Test
    fun `nothing is selected without sessions`() {
        assertNull(selectSession(emptyList<Session<String>>(), SelectionSettings()))
    }

    @Test
    fun `most recently active session is the last resort`() {
        assertEquals(podcasts, selectSession(listOf(podcasts, youtube), SelectionSettings()))
    }

    @Test
    fun `playing session wins over the most recently active one`() {
        val playing = session(YOUTUBE, isPlaying = true)

        assertEquals(playing, selectSession(listOf(podcasts, playing), SelectionSettings()))
    }

    @Test
    fun `favorite wins over a playing session`() {
        val playing = session(YOUTUBE, isPlaying = true)

        assertEquals(
            spotify,
            selectSession(listOf(playing, spotify), SelectionSettings(favorite = SPOTIFY))
        )
    }

    @Test
    fun `player picked in the widget wins over the favorite`() {
        val settings = SelectionSettings(favorite = SPOTIFY, selected = PODCASTS)

        assertEquals(podcasts, selectSession(listOf(spotify, podcasts), settings))
    }

    @Test
    fun `missing favorite falls back to the normal order`() {
        val playing = session(YOUTUBE, isPlaying = true)

        assertEquals(
            playing,
            selectSession(listOf(podcasts, playing), SelectionSettings(favorite = SPOTIFY))
        )
    }

    @Test
    fun `ignored players are never shown, not even as favorite or pick`() {
        val settings =
            SelectionSettings(ignored = setOf(SPOTIFY), favorite = SPOTIFY, selected = SPOTIFY)

        assertEquals(podcasts, chooseSession(listOf(spotify, podcasts), settings).session)
    }

    @Test
    fun `only ignored players leave nothing to show`() {
        val selection = chooseSession(listOf(youtube), SelectionSettings(ignored = setOf(YOUTUBE)))

        assertNull(selection.session)
        assertTrue(selection.isIgnoredPlayerConnected)
    }

    @Test
    fun `no ignored player is connected without ignored sessions`() {
        val selection = chooseSession(listOf(spotify), SelectionSettings(ignored = setOf(YOUTUBE)))

        assertFalse(selection.isIgnoredPlayerConnected)
    }

    @Test
    fun `switching needs two different allowed players`() {
        assertFalse(chooseSession(listOf(spotify), SelectionSettings()).canSwitch)
        assertFalse(chooseSession(listOf(spotify, session(SPOTIFY)), SelectionSettings()).canSwitch)
        assertFalse(
            chooseSession(
                listOf(spotify, youtube),
                SelectionSettings(ignored = setOf(YOUTUBE))
            ).canSwitch
        )
        assertTrue(chooseSession(listOf(spotify, podcasts), SelectionSettings()).canSwitch)
    }

    @Test
    fun `pick is kept while its player has a session`() {
        assertTrue(
            chooseSession(
                listOf(spotify, podcasts),
                SelectionSettings(selected = PODCASTS)
            ).keepSelected
        )
        assertTrue(chooseSession(listOf(spotify), SelectionSettings()).keepSelected)
    }

    @Test
    fun `pick is dropped when its session is gone or its player turned off`() {
        assertFalse(
            chooseSession(
                listOf(spotify),
                SelectionSettings(selected = PODCASTS)
            ).keepSelected
        )
        val ignored = SelectionSettings(ignored = setOf(PODCASTS), selected = PODCASTS)
        assertFalse(chooseSession(listOf(spotify, podcasts), ignored).keepSelected)
    }

    @Test
    fun `remembered track is shown without a session`() {
        val selection = chooseSession(emptyList<Session<String>>(), SelectionSettings())

        assertEquals(remembered(SPOTIFY), fallbackState(remembered(SPOTIFY), selection, emptySet()))
    }

    @Test
    fun `remembered track is flagged while an ignored player is connected`() {
        val settings = SelectionSettings(ignored = setOf(YOUTUBE))
        val selection = chooseSession(listOf(youtube), settings)

        val state = fallbackState(remembered(SPOTIFY), selection, settings.ignored)

        assertEquals(remembered(SPOTIFY).copy(isIgnoredPlayerConnected = true), state)
    }

    @Test
    fun `remembered track of a turned-off player is not shown`() {
        val settings = SelectionSettings(ignored = setOf(SPOTIFY))
        val selection = chooseSession(emptyList<Session<String>>(), settings)

        assertEquals(
            MediaState.NoSession,
            fallbackState(remembered(SPOTIFY), selection, settings.ignored)
        )
    }

    @Test
    fun `nothing remembered means no session`() {
        val selection = chooseSession(emptyList<Session<String>>(), SelectionSettings())

        assertEquals(MediaState.NoSession, fallbackState(null, selection, emptySet()))
    }

    @Test
    fun `next player follows a stable order and wraps around`() {
        val players = listOf(YOUTUBE, SPOTIFY, PODCASTS)

        assertEquals(PODCASTS, nextPlayer(players, SPOTIFY))
        assertEquals(YOUTUBE, nextPlayer(players, PODCASTS))
        assertEquals(SPOTIFY, nextPlayer(players, YOUTUBE))
    }

    @Test
    fun `next player starts at the beginning for an unknown current player`() {
        assertEquals(PODCASTS, nextPlayer(listOf(SPOTIFY, PODCASTS), null))
        assertEquals(PODCASTS, nextPlayer(listOf(SPOTIFY, PODCASTS), YOUTUBE))
    }

    @Test
    fun `there is no next player with fewer than two players`() {
        assertNull(nextPlayer(emptyList(), null))
        assertNull(nextPlayer(listOf(SPOTIFY, SPOTIFY), SPOTIFY))
    }

    private companion object {
        const val PODCASTS = "com.example.podcasts"
        const val SPOTIFY = "com.spotify.music"
        const val YOUTUBE = "com.google.android.youtube"
    }
}
