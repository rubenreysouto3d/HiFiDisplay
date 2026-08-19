package com.rubenreysouto.hifidisplay.media

import android.media.session.PlaybackState

internal data class SessionCandidate<T>(
    val id: T,
    val packageName: String,
    val playbackStatus: MediaPlaybackStatus,
)

internal object MediaSessionArbitrator {
    fun <T> select(
        candidates: List<SessionCandidate<T>>,
        pinnedPackageName: String?,
        currentId: T?,
    ): T? {
        val pinnedCandidates = pinnedPackageName?.let { packageName ->
            candidates.filter { it.packageName == packageName }
        }.orEmpty()
        val eligible = pinnedCandidates.ifEmpty { candidates }
        val bestPriority = eligible.maxOfOrNull { it.playbackStatus.selectionPriority } ?: return null
        eligible.firstOrNull {
            it.id == currentId && it.playbackStatus.selectionPriority == bestPriority
        }?.let { return it.id }
        return eligible.firstOrNull { it.playbackStatus.selectionPriority == bestPriority }?.id
    }
}

private val MediaPlaybackStatus.selectionPriority: Int
    get() = when (this) {
        MediaPlaybackStatus.PLAYING -> 5
        MediaPlaybackStatus.BUFFERING -> 4
        MediaPlaybackStatus.CONNECTING -> 3
        MediaPlaybackStatus.PAUSED -> 2
        MediaPlaybackStatus.STOPPED -> 1
        MediaPlaybackStatus.IDLE, MediaPlaybackStatus.ERROR -> 0
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

internal interface MediaTransport {
    val actions: Long
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
}

internal object MediaTransportDispatcher {
    fun play(transport: MediaTransport?) = execute(transport, Long::supportsPlay, MediaTransport::play)

    fun pause(transport: MediaTransport?) = execute(transport, Long::supportsPause, MediaTransport::pause)

    fun previous(transport: MediaTransport?) = execute(
        transport,
        { it.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS) },
        MediaTransport::skipToPrevious,
    )

    fun next(transport: MediaTransport?) = execute(
        transport,
        { it.supports(PlaybackState.ACTION_SKIP_TO_NEXT) },
        MediaTransport::skipToNext,
    )

    fun seek(transport: MediaTransport?, positionMs: Long, durationMs: Long?): Boolean {
        if (transport == null || !transport.actions.supports(PlaybackState.ACTION_SEEK_TO)) return false
        transport.seekTo(SeekPositionSanitizer.sanitize(positionMs, durationMs))
        return true
    }

    private fun execute(
        transport: MediaTransport?,
        supportsAction: (Long) -> Boolean,
        command: (MediaTransport) -> Unit,
    ): Boolean {
        if (transport == null || !supportsAction(transport.actions)) return false
        command(transport)
        return true
    }
}

internal object SessionRetryPolicy {
    private val delaysMs = longArrayOf(500L, 1_000L, 2_000L, 5_000L)

    fun delayForAttempt(attempt: Int): Long = delaysMs[attempt.coerceIn(0, delaysMs.lastIndex)]
}

internal fun Long.toMediaCapabilities() = MediaCapabilities(
    canPlay = supports(PlaybackState.ACTION_PLAY) || supports(PlaybackState.ACTION_PLAY_PAUSE),
    canPause = supports(PlaybackState.ACTION_PAUSE) || supports(PlaybackState.ACTION_PLAY_PAUSE),
    canSkipPrevious = supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
    canSkipNext = supports(PlaybackState.ACTION_SKIP_TO_NEXT),
    canSeek = supports(PlaybackState.ACTION_SEEK_TO),
)

internal fun Long.supports(action: Long) = this and action != 0L

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

internal fun Long.toSupportedActionNames(): List<String> = buildList {
    if (supportsPlay()) add("PLAY")
    if (supportsPause()) add("PAUSE")
    if (supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) add("PREVIOUS")
    if (supports(PlaybackState.ACTION_SKIP_TO_NEXT)) add("NEXT")
    if (supports(PlaybackState.ACTION_SEEK_TO)) add("SEEK")
}
