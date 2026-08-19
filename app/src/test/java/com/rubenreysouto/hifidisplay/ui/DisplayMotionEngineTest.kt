package com.rubenreysouto.hifidisplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayMotionEngineTest {
    @Test
    fun `paused playback always resolves to a still identity`() {
        PlaybackArtworkEffect.entries.forEach { effect ->
            assertEquals(PlaybackArtworkVisual(), resolvePlaybackArtworkVisual(effect, phase = .75f, isPlaying = false))
        }
    }

    @Test
    fun `playback effects produce distinct visual signatures`() {
        val visuals = PlaybackArtworkEffect.entries.map { resolvePlaybackArtworkVisual(it, phase = .8f, isPlaying = true) }

        assertEquals(visuals.size, visuals.distinct().size)
        assertTrue(visuals[PlaybackArtworkEffect.PULSE.ordinal].scale > 1f)
        assertNotEquals(0f, visuals[PlaybackArtworkEffect.DRIFT.ordinal].translationX)
        assertTrue(visuals[PlaybackArtworkEffect.HALO.ordinal].haloAlpha > .3f)
        assertEquals(PlaybackArtworkVisual(), visuals[PlaybackArtworkEffect.STILL.ordinal])
    }

    @Test
    fun `motion phase is clamped before visual values are calculated`() {
        assertEquals(
            resolvePlaybackArtworkVisual(PlaybackArtworkEffect.PULSE, phase = 0f, isPlaying = true),
            resolvePlaybackArtworkVisual(PlaybackArtworkEffect.PULSE, phase = -4f, isPlaying = true),
        )
        assertEquals(
            resolvePlaybackArtworkVisual(PlaybackArtworkEffect.HALO, phase = 1f, isPlaying = true),
            resolvePlaybackArtworkVisual(PlaybackArtworkEffect.HALO, phase = 8f, isPlaying = true),
        )
    }
}
