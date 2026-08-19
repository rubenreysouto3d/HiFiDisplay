package com.rubenreysouto.hifidisplay.preferences

import android.content.Context
import com.rubenreysouto.hifidisplay.ui.DisplaySkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayPreferencesRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _skin = MutableStateFlow(DisplaySkin.fromStorage(preferences.getString(SKIN_KEY, null)))
    val skin: StateFlow<DisplaySkin> = _skin.asStateFlow()

    fun selectSkin(skin: DisplaySkin) {
        if (_skin.value == skin) return
        _skin.value = skin
        preferences.edit().putString(SKIN_KEY, skin.storageKey).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "display_preferences"
        const val SKIN_KEY = "skin"
    }
}
