package com.rubenreysouto.hifidisplay.ui

import androidx.compose.runtime.Immutable

@Immutable
internal data class PlaybackArtworkVisual(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val haloAlpha: Float = 0f,
    val sheenAlpha: Float = 0f,
)

internal fun resolvePlaybackArtworkVisual(
    effect: PlaybackArtworkEffect,
    phase: Float,
    isPlaying: Boolean,
): PlaybackArtworkVisual {
    if (!isPlaying) return PlaybackArtworkVisual()
    val value = phase.coerceIn(0f, 1f)
    return when (effect) {
        PlaybackArtworkEffect.PULSE -> PlaybackArtworkVisual(
            scale = 1f + value * .018f,
            haloAlpha = .12f + value * .26f,
            sheenAlpha = .04f + value * .09f,
        )
        PlaybackArtworkEffect.DRIFT -> PlaybackArtworkVisual(
            scale = 1.018f + value * .02f,
            translationX = -5f + value * 10f,
            translationY = 3f - value * 6f,
            haloAlpha = .1f + value * .06f,
            sheenAlpha = .05f,
        )
        PlaybackArtworkEffect.HALO -> PlaybackArtworkVisual(
            haloAlpha = .12f + value * .36f,
            sheenAlpha = .06f + value * .1f,
        )
        PlaybackArtworkEffect.STILL -> PlaybackArtworkVisual()
    }
}
