package com.rubenreysouto.hifidisplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rubenreysouto.hifidisplay.media.MediaSessionRepository
import com.rubenreysouto.hifidisplay.preferences.DisplayPreferencesRepository
import com.rubenreysouto.hifidisplay.ui.ArtworkMotion
import com.rubenreysouto.hifidisplay.ui.ColorPalette
import com.rubenreysouto.hifidisplay.ui.DisplayDesign
import com.rubenreysouto.hifidisplay.ui.PlaybackArtworkEffect
import com.rubenreysouto.hifidisplay.ui.PaletteMode

class HiFiDisplayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaSessionRepository.get(application)
    private val displayPreferences = DisplayPreferencesRepository(application)

    val state = repository.state
    val appearance = displayPreferences.appearance

    fun onResume() = repository.onResume()
    fun onPause() = repository.onPause()
    fun play() = repository.play()
    fun pause() = repository.pause()
    fun previous() = repository.previous()
    fun next() = repository.next()
    fun seekTo(positionMs: Long) = repository.seekTo(positionMs)
    fun selectSource(packageName: String?) = repository.selectSource(packageName)
    fun selectPalette(palette: ColorPalette) = displayPreferences.selectPalette(palette)
    fun selectPaletteMode(mode: PaletteMode) = displayPreferences.selectPaletteMode(mode)
    fun selectDesign(design: DisplayDesign) = displayPreferences.selectDesign(design)
    fun selectArtworkMotion(motion: ArtworkMotion) = displayPreferences.selectArtworkMotion(motion)
    fun selectPlaybackArtworkEffect(effect: PlaybackArtworkEffect) =
        displayPreferences.selectPlaybackArtworkEffect(effect)
}
