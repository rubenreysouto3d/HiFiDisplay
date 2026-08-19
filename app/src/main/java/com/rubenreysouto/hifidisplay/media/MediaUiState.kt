package com.rubenreysouto.hifidisplay.media

import android.graphics.Bitmap

enum class SessionAvailability {
    PERMISSION_REQUIRED,
    NO_SESSION,
    ACTIVE,
    ERROR,
}

enum class MediaPlaybackStatus {
    IDLE,
    CONNECTING,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR,
}

data class MediaCapabilities(
    val canPlay: Boolean = false,
    val canPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
)

data class MediaSourceUiState(
    val packageName: String,
    val label: String,
    val isPlaying: Boolean,
    val isSelected: Boolean,
    val isPinned: Boolean,
)

data class MediaDiagnosticsUiState(
    val packageName: String? = null,
    val playbackStatus: MediaPlaybackStatus = MediaPlaybackStatus.IDLE,
    val supportedActions: List<String> = emptyList(),
    val hasTitle: Boolean = false,
    val hasArtist: Boolean = false,
    val hasAlbum: Boolean = false,
    val hasArtwork: Boolean = false,
    val hasDuration: Boolean = false,
    val retryAttempt: Int = 0,
    val errorType: String? = null,
)

data class MediaUiState(
    val availability: SessionAvailability = SessionAvailability.PERMISSION_REQUIRED,
    val playbackStatus: MediaPlaybackStatus = MediaPlaybackStatus.IDLE,
    val capabilities: MediaCapabilities = MediaCapabilities(),
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artwork: Bitmap? = null,
    val sourceApp: String? = null,
    val selectedSourcePackage: String? = null,
    val pinnedSourcePackage: String? = null,
    val availableSources: List<MediaSourceUiState> = emptyList(),
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val errorMessage: String? = null,
    val diagnostics: MediaDiagnosticsUiState = MediaDiagnosticsUiState(),
) {
    val hasNotificationAccess: Boolean
        get() = availability != SessionAvailability.PERMISSION_REQUIRED

    val hasActiveSession: Boolean
        get() = availability == SessionAvailability.ACTIVE

    val isPlaying: Boolean
        get() = playbackStatus == MediaPlaybackStatus.PLAYING

    val canPlay: Boolean
        get() = capabilities.canPlay

    val canPause: Boolean
        get() = capabilities.canPause

    val canSkipPrevious: Boolean
        get() = capabilities.canSkipPrevious

    val canSkipNext: Boolean
        get() = capabilities.canSkipNext

    val canSeek: Boolean
        get() = capabilities.canSeek
}
