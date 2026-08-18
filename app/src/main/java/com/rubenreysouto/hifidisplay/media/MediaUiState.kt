package com.rubenreysouto.hifidisplay.media

import android.graphics.Bitmap

data class MediaUiState(
    val hasNotificationAccess: Boolean = false,
    val hasActiveSession: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artwork: Bitmap? = null,
    val sourceApp: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val isPlaying: Boolean = false,
    val canPlay: Boolean = false,
    val canPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
)
