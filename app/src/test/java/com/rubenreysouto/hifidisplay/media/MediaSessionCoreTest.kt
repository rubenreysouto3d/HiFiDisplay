package com.rubenreysouto.hifidisplay.media

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionCoreTest {
    @Test
    fun `pinned source wins over another playing source`() {
        val candidates = listOf(
            SessionCandidate(id = "spotify", packageName = "spotify", isPlaying = true),
            SessionCandidate(id = "poweramp", packageName = "poweramp", isPlaying = false),
        )

        val selected = MediaSessionArbitrator.select(candidates, "poweramp", "spotify")

        assertEquals("poweramp", selected)
    }

    @Test
    fun `playing source wins when nothing is pinned`() {
        val candidates = listOf(
            SessionCandidate(id = "paused", packageName = "paused", isPlaying = false),
            SessionCandidate(id = "playing", packageName = "playing", isPlaying = true),
        )

        val selected = MediaSessionArbitrator.select(candidates, null, "paused")

        assertEquals("playing", selected)
    }

    @Test
    fun `current source is retained when all sessions are paused`() {
        val candidates = listOf(
            SessionCandidate(id = "first", packageName = "first", isPlaying = false),
            SessionCandidate(id = "current", packageName = "current", isPlaying = false),
        )

        val selected = MediaSessionArbitrator.select(candidates, null, "current")

        assertEquals("current", selected)
    }

    @Test
    fun `position advances using speed and elapsed realtime`() {
        val position = PlaybackPositionEstimator.estimate(
            basePositionMs = 10_000L,
            lastUpdateTimeMs = 5_000L,
            playbackSpeed = 1.5f,
            isAdvancing = true,
            nowMs = 7_000L,
            durationMs = 30_000L,
        )

        assertEquals(13_000L, position)
    }

    @Test
    fun `position does not advance with missing update time`() {
        val position = PlaybackPositionEstimator.estimate(
            basePositionMs = 10_000L,
            lastUpdateTimeMs = 0L,
            playbackSpeed = 1f,
            isAdvancing = true,
            nowMs = 20_000L,
            durationMs = 30_000L,
        )

        assertEquals(10_000L, position)
    }

    @Test
    fun `position is clamped to known duration`() {
        val position = PlaybackPositionEstimator.estimate(
            basePositionMs = 29_000L,
            lastUpdateTimeMs = 1_000L,
            playbackSpeed = 2f,
            isAdvancing = true,
            nowMs = 3_000L,
            durationMs = 30_000L,
        )

        assertEquals(30_000L, position)
    }

    @Test
    fun `toggle action enables play and pause but not seek`() {
        val capabilities = PlaybackState.ACTION_PLAY_PAUSE.toMediaCapabilities()

        assertTrue(capabilities.canPlay)
        assertTrue(capabilities.canPause)
        assertFalse(capabilities.canSeek)
    }

    @Test
    fun `capabilities exactly reflect transport actions`() {
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SEEK_TO

        val capabilities = actions.toMediaCapabilities()

        assertTrue(capabilities.canPlay)
        assertFalse(capabilities.canPause)
        assertFalse(capabilities.canSkipPrevious)
        assertTrue(capabilities.canSkipNext)
        assertTrue(capabilities.canSeek)
    }
}
