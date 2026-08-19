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
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubenreysouto.hifidisplay.ui.HiFiDisplayApp

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<HiFiDisplayViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        enterImmersiveMode()
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val appearance = viewModel.appearance.collectAsStateWithLifecycle().value
            HiFiDisplayApp(
                state = state,
                appearance = appearance,
                onOpenAccessSettings = {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onPlay = viewModel::play,
                onPause = viewModel::pause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onSeek = viewModel::seekTo,
                onSelectSource = viewModel::selectSource,
                onSelectPalette = viewModel::selectPalette,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        enterImmersiveMode()
    }

    override fun onPause() {
        viewModel.onPause()
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
