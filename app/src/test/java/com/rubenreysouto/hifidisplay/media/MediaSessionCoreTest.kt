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
            SessionCandidate(id = "spotify", packageName = "spotify", playbackStatus = MediaPlaybackStatus.PLAYING),
            SessionCandidate(id = "poweramp", packageName = "poweramp", playbackStatus = MediaPlaybackStatus.PAUSED),
        )

        val selected = MediaSessionArbitrator.select(candidates, "poweramp", "spotify")

        assertEquals("poweramp", selected)
    }

    @Test
    fun `playing source wins when nothing is pinned`() {
        val candidates = listOf(
            SessionCandidate(id = "paused", packageName = "paused", playbackStatus = MediaPlaybackStatus.PAUSED),
            SessionCandidate(id = "playing", packageName = "playing", playbackStatus = MediaPlaybackStatus.PLAYING),
        )

        val selected = MediaSessionArbitrator.select(candidates, null, "paused")

        assertEquals("playing", selected)
    }

    @Test
    fun `current source is retained when all sessions are paused`() {
        val candidates = listOf(
            SessionCandidate(id = "first", packageName = "first", playbackStatus = MediaPlaybackStatus.PAUSED),
            SessionCandidate(id = "current", packageName = "current", playbackStatus = MediaPlaybackStatus.PAUSED),
        )

        val selected = MediaSessionArbitrator.select(candidates, null, "current")

        assertEquals("current", selected)
    }

    @Test
    fun `missing pinned source falls back to playing source`() {
        val candidates = listOf(
            SessionCandidate(id = "paused", packageName = "paused", playbackStatus = MediaPlaybackStatus.PAUSED),
            SessionCandidate(id = "playing", packageName = "playing", playbackStatus = MediaPlaybackStatus.PLAYING),
        )

        val selected = MediaSessionArbitrator.select(candidates, "missing", "paused")

        assertEquals("playing", selected)
    }

    @Test
    fun `buffering source wins over paused source`() {
        val candidates = listOf(
            SessionCandidate(id = "paused", packageName = "paused", playbackStatus = MediaPlaybackStatus.PAUSED),
            SessionCandidate(id = "buffering", packageName = "buffering", playbackStatus = MediaPlaybackStatus.BUFFERING),
        )

        assertEquals("buffering", MediaSessionArbitrator.select(candidates, null, "paused"))
    }

    @Test
    fun `current source is stable when candidates have equal priority`() {
        val candidates = listOf(
            SessionCandidate(id = "first", packageName = "first", playbackStatus = MediaPlaybackStatus.PLAYING),
            SessionCandidate(id = "current", packageName = "current", playbackStatus = MediaPlaybackStatus.PLAYING),
        )

        assertEquals("current", MediaSessionArbitrator.select(candidates, null, "current"))
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
    fun `invalid playback speed does not corrupt position`() {
        val position = PlaybackPositionEstimator.estimate(
            basePositionMs = 10_000L,
            lastUpdateTimeMs = 1_000L,
            playbackSpeed = Float.NaN,
            isAdvancing = true,
            nowMs = 3_000L,
            durationMs = 30_000L,
        )

        assertEquals(10_000L, position)
    }

    @Test
    fun `rewind speed cannot produce a negative position`() {
        val position = PlaybackPositionEstimator.estimate(
            basePositionMs = 1_000L,
            lastUpdateTimeMs = 1_000L,
            playbackSpeed = -2f,
            isAdvancing = true,
            nowMs = 3_000L,
            durationMs = 30_000L,
        )

        assertEquals(0L, position)
    }

    @Test
    fun `seek position is clamped to valid media range`() {
        assertEquals(0L, SeekPositionSanitizer.sanitize(-1_000L, 30_000L))
        assertEquals(30_000L, SeekPositionSanitizer.sanitize(40_000L, 30_000L))
        assertEquals(40_000L, SeekPositionSanitizer.sanitize(40_000L, null))
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

    @Test
    fun `transport dispatcher blocks unsupported commands`() {
        val transport = RecordingTransport(actions = PlaybackState.ACTION_PLAY)

        assertFalse(MediaTransportDispatcher.pause(transport))
        assertFalse(MediaTransportDispatcher.next(transport))
        assertTrue(transport.commands.isEmpty())
        assertTrue(MediaTransportDispatcher.play(transport))
        assertEquals(listOf("play"), transport.commands)
    }

    @Test
    fun `transport dispatcher clamps supported seek`() {
        val transport = RecordingTransport(actions = PlaybackState.ACTION_SEEK_TO)

        assertTrue(MediaTransportDispatcher.seek(transport, 45_000L, 30_000L))
        assertEquals(30_000L, transport.seekPositionMs)
    }

    @Test
    fun `retry delay backs off and remains bounded`() {
        assertEquals(500L, SessionRetryPolicy.delayForAttempt(0))
        assertEquals(1_000L, SessionRetryPolicy.delayForAttempt(1))
        assertEquals(2_000L, SessionRetryPolicy.delayForAttempt(2))
        assertEquals(5_000L, SessionRetryPolicy.delayForAttempt(3))
        assertEquals(5_000L, SessionRetryPolicy.delayForAttempt(100))
    }

    @Test
    fun `diagnostic action names only expose supported controls`() {
        val actions = PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT

        assertEquals(listOf("PLAY", "PAUSE", "NEXT"), actions.toSupportedActionNames())
    }

    private class RecordingTransport(override val actions: Long) : MediaTransport {
        val commands = mutableListOf<String>()
        var seekPositionMs: Long? = null

        override fun play() { commands += "play" }
        override fun pause() { commands += "pause" }
        override fun skipToPrevious() { commands += "previous" }
        override fun skipToNext() { commands += "next" }
        override fun seekTo(positionMs: Long) {
            commands += "seek"
            seekPositionMs = positionMs
        }
    }
}
