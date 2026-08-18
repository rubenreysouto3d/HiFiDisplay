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
    private val preferences = appContext.getSharedPreferences("media_session", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var observedControllers = emptyList<MediaController>()
    private var pinnedSourcePackage: String? = preferences.getString(PINNED_SOURCE_KEY, null)
    private var sessionErrorMessage: String? = null
    private var tickerRunning = false
    private var sessionsListenerRegistered = false
    private val applicationLabels = mutableMapOf<String, String>()
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishState()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshSessions()
        override fun onSessionDestroyed() = refreshSessions()
    }
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }
    private val ticker = object : Runnable {
        override fun run() {
            if (controller.safePlaybackState()?.state == PlaybackState.STATE_PLAYING) publishState()
            if (tickerRunning) handler.postDelayed(this, 1_000L)
        }
    }

    init {
        instance = this
        refreshAccessAndListener()
    }

    fun onResume() {
        refreshAccessAndListener()
        if (!tickerRunning) {
            tickerRunning = true
            handler.post(ticker)
        }
    }

    fun onPause() {
        tickerRunning = false
        handler.removeCallbacks(ticker)
    }

    private fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)

    private fun refreshAccessAndListener() {
        val access = hasAccess()
        if (access && !sessionsListenerRegistered) {
            sessionsListenerRegistered = runCatching {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, notificationComponent)
            }.isSuccess
        } else if (!access && sessionsListenerRegistered) {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
            sessionsListenerRegistered = false
        }
        refreshSessions()
    }

    private fun refreshSessions() {
        val access = hasAccess()
        if (!access) {
            sessionErrorMessage = null
            updateObservedControllers(emptyList())
            controller = null
            publishState()
            return
        }

        val sessionsResult = runCatching { sessionManager.getActiveSessions(notificationComponent) }
        if (sessionsResult.isFailure) {
            sessionErrorMessage = sessionsResult.exceptionOrNull()?.javaClass?.simpleName ?: "MediaSession error"
            updateObservedControllers(emptyList())
            controller = null
            publishState()
            return
        }

        sessionErrorMessage = null
        val sessions = sessionsResult.getOrDefault(emptyList())
        updateObservedControllers(sessions)
        val selectedToken = MediaSessionArbitrator.select(
            candidates = sessions.map {
                SessionCandidate(
                    id = it.sessionToken,
                    packageName = it.packageName,
                    isPlaying = it.safePlaybackState()?.state == PlaybackState.STATE_PLAYING,
                )
            },
            pinnedPackageName = pinnedSourcePackage,
            currentId = controller?.sessionToken,
        )
        controller = sessions.firstOrNull { it.sessionToken == selectedToken }
        publishState()
    }

    private fun updateObservedControllers(sessions: List<MediaController>) {
        val observedTokens = observedControllers.map { it.sessionToken }
        val sessionTokens = sessions.map { it.sessionToken }
        if (observedTokens != sessionTokens) {
            observedControllers.forEach { runCatching { it.unregisterCallback(callback) } }
            observedControllers = sessions
            observedControllers.forEach { runCatching { it.registerCallback(callback, handler) } }
        }
    }

    private fun publishState() {
        val access = hasAccess()
        val current = controller
        val metadata = current?.let { runCatching { it.metadata }.getOrNull() }
        val playback = current.safePlaybackState()
        val actions = playback?.actions ?: 0L
        val sourceApp = current?.packageName?.let(::applicationLabel)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0L }
        val position = playback?.let {
            PlaybackPositionEstimator.estimate(
                basePositionMs = it.position,
                lastUpdateTimeMs = it.lastPositionUpdateTime,
                playbackSpeed = it.playbackSpeed,
                isAdvancing = it.state == PlaybackState.STATE_PLAYING,
                nowMs = SystemClock.elapsedRealtime(),
                durationMs = duration,
            )
        } ?: 0L
        val availability = when {
            !access -> SessionAvailability.PERMISSION_REQUIRED
            sessionErrorMessage != null -> SessionAvailability.ERROR
            current == null -> SessionAvailability.NO_SESSION
            else -> SessionAvailability.ACTIVE
        }
        val selectedToken = current?.sessionToken
        _state.value = MediaUiState(
            availability = availability,
            playbackStatus = playback?.state.toMediaPlaybackStatus(),
            capabilities = actions.toMediaCapabilities(),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).present()
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).present()
                ?: metadata?.description?.title?.toString().present(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).present()
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).present()
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).present()
                ?: metadata?.description?.subtitle?.toString().present()
                ?: sourceApp,
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            sourceApp = sourceApp,
            selectedSourcePackage = current?.packageName,
            pinnedSourcePackage = pinnedSourcePackage,
            availableSources = observedControllers
                .map {
                    MediaSourceUiState(
                        packageName = it.packageName,
                        label = applicationLabel(it.packageName),
                        isPlaying = it.safePlaybackState()?.state == PlaybackState.STATE_PLAYING,
                        isSelected = it.sessionToken == selectedToken,
                        isPinned = it.packageName == pinnedSourcePackage,
                    )
                }
                .distinctBy { it.packageName },
            positionMs = position,
            durationMs = duration,
            errorMessage = sessionErrorMessage,
        )
    }

    private fun String?.present(): String? = this?.takeUnless(String::isBlank)

    private fun applicationLabel(packageName: String): String = applicationLabels.getOrPut(packageName) {
        try {
            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    fun selectSource(packageName: String?) {
        pinnedSourcePackage = packageName
        preferences.edit().apply {
            if (packageName == null) remove(PINNED_SOURCE_KEY) else putString(PINNED_SOURCE_KEY, packageName)
        }.apply()
        refreshSessions()
    }

    fun play() = withSupportedController(Long::supportsPlay) { it.play() }

    fun pause() = withSupportedController(Long::supportsPause) { it.pause() }

    fun previous() = withSupportedController({ it supports PlaybackState.ACTION_SKIP_TO_PREVIOUS }) {
        it.skipToPrevious()
    }

    fun next() = withSupportedController({ it supports PlaybackState.ACTION_SKIP_TO_NEXT }) {
        it.skipToNext()
    }

    fun seekTo(positionMs: Long) = withSupportedController({ it supports PlaybackState.ACTION_SEEK_TO }) {
        val duration = controller?.metadata
            ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { value -> value > 0L }
        it.seekTo(SeekPositionSanitizer.sanitize(positionMs, duration))
    }

    private fun withSupportedController(
        supportsAction: (Long) -> Boolean,
        command: (MediaController.TransportControls) -> Unit,
    ) {
        val current = controller ?: return
        runCatching {
            val actions = current.playbackState?.actions ?: 0L
            if (supportsAction(actions)) command(current.transportControls)
        }
    }

    private infix fun Long.supports(action: Long) = this and action != 0L

    private fun MediaController?.safePlaybackState(): PlaybackState? =
        this?.let { runCatching { it.playbackState }.getOrNull() }

    companion object {
        private const val PINNED_SOURCE_KEY = "pinned_source_package"
        @Volatile private var instance: MediaSessionRepository? = null
        fun get(context: Context): MediaSessionRepository = instance
            ?: synchronized(this) { instance ?: MediaSessionRepository(context).also { instance = it } }
        fun notifySessionEnvironmentChanged() { instance?.handler?.post { instance?.refreshSessions() } }
    }
}
