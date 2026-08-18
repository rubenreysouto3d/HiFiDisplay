package com.rubenreysouto.hifidisplay.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaSessionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val notificationComponent = ComponentName(appContext, HiFiNotificationListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishState()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishState()
        override fun onSessionDestroyed() = refreshSessions()
    }
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }
    private val ticker = object : Runnable {
        override fun run() {
            if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) publishState()
            handler.postDelayed(this, 1_000L)
        }
    }

    init {
        instance = this
        refreshAccessAndListener()
        handler.post(ticker)
    }

    fun onResume() = refreshAccessAndListener()

    private fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)

    private fun refreshAccessAndListener() {
        val access = hasAccess()
        try { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) } catch (_: Exception) { }
        if (access) {
            try { sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, notificationComponent) } catch (_: SecurityException) { }
        }
        refreshSessions()
    }

    private fun refreshSessions() {
        val access = hasAccess()
        val sessions = if (access) {
            try { sessionManager.getActiveSessions(notificationComponent) } catch (_: SecurityException) { emptyList() }
        } else emptyList()
        val selected = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
        if (selected?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = selected
            controller?.registerCallback(callback, handler)
        }
        publishState()
    }

    private fun publishState() {
        val access = hasAccess()
        val current = controller
        val metadata = current?.metadata
        val playback = current?.playbackState
        val actions = playback?.actions ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0L }
        val calculatedPosition = playback?.let {
            val elapsed = if (it.state == PlaybackState.STATE_PLAYING) {
                (SystemClock.elapsedRealtime() - it.lastPositionUpdateTime).coerceAtLeast(0L)
            } else 0L
            (it.position + elapsed * it.playbackSpeed).toLong().coerceAtLeast(0L)
        } ?: 0L
        val position = duration?.let { calculatedPosition.coerceAtMost(it) } ?: calculatedPosition
        _state.value = MediaUiState(
            hasNotificationAccess = access,
            hasActiveSession = current != null,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            sourceApp = current?.packageName?.let(::applicationLabel),
            positionMs = position,
            durationMs = duration,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            canPlay = actions supports PlaybackState.ACTION_PLAY,
            canPause = actions supports PlaybackState.ACTION_PAUSE,
            canSkipPrevious = actions supports PlaybackState.ACTION_SKIP_TO_PREVIOUS,
            canSkipNext = actions supports PlaybackState.ACTION_SKIP_TO_NEXT,
            canSeek = actions supports PlaybackState.ACTION_SEEK_TO,
        )
    }

    private infix fun Long.supports(action: Long) = this and action != 0L

    private fun applicationLabel(packageName: String): String = try {
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) { packageName }

    fun play() = controller?.transportControls?.play()
    fun pause() = controller?.transportControls?.pause()
    fun previous() = controller?.transportControls?.skipToPrevious()
    fun next() = controller?.transportControls?.skipToNext()
    fun seekTo(positionMs: Long) = controller?.transportControls?.seekTo(positionMs)

    companion object {
        @Volatile private var instance: MediaSessionRepository? = null
        fun get(context: Context): MediaSessionRepository = instance
            ?: synchronized(this) { instance ?: MediaSessionRepository(context).also { instance = it } }
        fun notifySessionEnvironmentChanged() { instance?.handler?.post { instance?.refreshSessions() } }
    }
}
