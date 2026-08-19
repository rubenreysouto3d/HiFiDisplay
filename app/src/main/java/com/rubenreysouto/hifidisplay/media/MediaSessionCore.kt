package com.rubenreysouto.hifidisplay.media

import android.media.session.PlaybackState

internal data class SessionCandidate<T>(
    val id: T,
    val packageName: String,
    val isPlaying: Boolean,
)

internal object MediaSessionArbitrator {
    fun <T> select(
        candidates: List<SessionCandidate<T>>,
        pinnedPackageName: String?,
        currentId: T?,
    ): T? {
        if (pinnedPackageName != null) {
            candidates.firstOrNull { it.packageName == pinnedPackageName && it.isPlaying }?.let { return it.id }
            candidates.firstOrNull { it.packageName == pinnedPackageName }?.let { return it.id }
        }
        candidates.firstOrNull { it.isPlaying }?.let { return it.id }
        candidates.firstOrNull { it.id == currentId }?.let { return it.id }
        return candidates.firstOrNull()?.id
    }
}

internal object PlaybackPositionEstimator {
    fun estimate(
        basePositionMs: Long,
        lastUpdateTimeMs: Long,
        playbackSpeed: Float,
        isAdvancing: Boolean,
        nowMs: Long,
        durationMs: Long?,
    ): Long {
        val elapsedMs = if (isAdvancing && lastUpdateTimeMs > 0L && nowMs >= lastUpdateTimeMs) {
            nowMs - lastUpdateTimeMs
        } else {
            0L
        }
        val safeSpeed = playbackSpeed.takeIf(Float::isFinite) ?: 0f
        val estimated = basePositionMs + (elapsedMs * safeSpeed).toLong()
        val nonNegative = estimated.coerceAtLeast(0L)
        return durationMs?.takeIf { it > 0L }?.let { nonNegative.coerceAtMost(it) } ?: nonNegative
    }
}

internal object SeekPositionSanitizer {
    fun sanitize(positionMs: Long, durationMs: Long?): Long {
        val nonNegative = positionMs.coerceAtLeast(0L)
        return durationMs?.takeIf { it > 0L }?.let(nonNegative::coerceAtMost) ?: nonNegative
    }
}

internal fun Long.toMediaCapabilities() = MediaCapabilities(
    canPlay = supports(PlaybackState.ACTION_PLAY) || supports(PlaybackState.ACTION_PLAY_PAUSE),
    canPause = supports(PlaybackState.ACTION_PAUSE) || supports(PlaybackState.ACTION_PLAY_PAUSE),
    canSkipPrevious = supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
    canSkipNext = supports(PlaybackState.ACTION_SKIP_TO_NEXT),
    canSeek = supports(PlaybackState.ACTION_SEEK_TO),
)

private fun Long.supports(action: Long) = this and action != 0L

internal fun Long.supportsPlay() = supports(PlaybackState.ACTION_PLAY) ||
    supports(PlaybackState.ACTION_PLAY_PAUSE)

internal fun Long.supportsPause() = supports(PlaybackState.ACTION_PAUSE) ||
    supports(PlaybackState.ACTION_PLAY_PAUSE)

internal fun Int?.toMediaPlaybackStatus(): MediaPlaybackStatus = when (this) {
    PlaybackState.STATE_CONNECTING -> MediaPlaybackStatus.CONNECTING
    PlaybackState.STATE_BUFFERING -> MediaPlaybackStatus.BUFFERING
    PlaybackState.STATE_PLAYING -> MediaPlaybackStatus.PLAYING
    PlaybackState.STATE_PAUSED -> MediaPlaybackStatus.PAUSED
    PlaybackState.STATE_STOPPED -> MediaPlaybackStatus.STOPPED
    PlaybackState.STATE_ERROR -> MediaPlaybackStatus.ERROR
    else -> MediaPlaybackStatus.IDLE
}
