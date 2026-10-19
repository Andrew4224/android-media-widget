package de.andrew4224.mediawidget.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressTest {

    private fun progress(
        position: Long = 30_000,
        duration: Long = 120_000,
        elapsed: Long = 0,
        speed: Float = 1f,
        isPlaying: Boolean = false,
    ) = playbackProgress(position, duration, elapsed, speed, isPlaying)

    @Test
    fun `paused position is used as is`() {
        assertEquals(0.25f, progress(elapsed = 60_000)!!, DELTA)
    }

    @Test
    fun `playing position is extrapolated by elapsed time`() {
        assertEquals(0.5f, progress(elapsed = 30_000, isPlaying = true)!!, DELTA)
    }

    @Test
    fun `playback speed is applied while playing`() {
        assertEquals(0.75f, progress(elapsed = 30_000, speed = 2f, isPlaying = true)!!, DELTA)
    }

    @Test
    fun `progress is capped at the end of the track`() {
        assertEquals(1f, progress(elapsed = 600_000, isPlaying = true)!!, DELTA)
    }

    @Test
    fun `progress is not negative when rewinding`() {
        assertEquals(0f, progress(elapsed = 60_000, speed = -1f, isPlaying = true)!!, DELTA)
    }

    @Test
    fun `start of the track is zero`() {
        assertEquals(0f, progress(position = 0)!!, DELTA)
    }

    @Test
    fun `unknown position has no progress`() {
        assertNull(progress(position = -1))
    }

    @Test
    fun `missing duration has no progress`() {
        assertNull(progress(duration = 0))
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
