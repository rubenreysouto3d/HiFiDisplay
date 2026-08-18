package com.rubenreysouto.hifidisplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rubenreysouto.hifidisplay.media.MediaSessionRepository

class HiFiDisplayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaSessionRepository.get(application)

    val state = repository.state

    fun onResume() = repository.onResume()
    fun onPause() = repository.onPause()
    fun play() = repository.play()
    fun pause() = repository.pause()
    fun previous() = repository.previous()
    fun next() = repository.next()
    fun seekTo(positionMs: Long) = repository.seekTo(positionMs)
    fun selectSource(packageName: String?) = repository.selectSource(packageName)
}
