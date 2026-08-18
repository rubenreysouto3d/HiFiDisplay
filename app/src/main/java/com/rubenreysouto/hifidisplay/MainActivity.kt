package com.rubenreysouto.hifidisplay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubenreysouto.hifidisplay.media.MediaSessionRepository
import com.rubenreysouto.hifidisplay.ui.HiFiDisplayApp

class MainActivity : ComponentActivity() {
    private val repository by lazy { MediaSessionRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        enterImmersiveMode()
        setContent {
            val state = repository.state.collectAsStateWithLifecycle().value
            HiFiDisplayApp(
                state = state,
                onOpenAccessSettings = {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onPlay = repository::play,
                onPause = repository::pause,
                onPrevious = repository::previous,
                onNext = repository::next,
                onSeek = repository::seekTo,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        repository.onResume()
        enterImmersiveMode()
    }

    override fun onPause() {
        repository.onPause()
        super.onPause()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
