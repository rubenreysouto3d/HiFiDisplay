package com.rubenreysouto.hifidisplay.preferences

import android.content.Context
import com.rubenreysouto.hifidisplay.ui.ArtworkMotion
import com.rubenreysouto.hifidisplay.ui.ColorPalette
import com.rubenreysouto.hifidisplay.ui.DisplayAppearance
import com.rubenreysouto.hifidisplay.ui.DisplayDesign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayPreferencesRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _appearance = MutableStateFlow(
        DisplayAppearance(
            design = DisplayDesign.fromStorage(preferences.getString(DESIGN_KEY, null)),
            palette = ColorPalette.fromStorage(
                preferences.getString(PALETTE_KEY, null)
                    ?: preferences.getString(LEGACY_SKIN_KEY, null),
            ),
            artworkMotion = ArtworkMotion.fromStorage(preferences.getString(ARTWORK_MOTION_KEY, null)),
        ),
    )
    val appearance: StateFlow<DisplayAppearance> = _appearance.asStateFlow()

    fun selectPalette(palette: ColorPalette) {
        if (_appearance.value.palette == palette) return
        _appearance.value = _appearance.value.copy(palette = palette)
        preferences.edit()
            .putString(PALETTE_KEY, palette.storageKey)
            .remove(LEGACY_SKIN_KEY)
            .apply()
    }

    fun selectDesign(design: DisplayDesign) {
        if (_appearance.value.design == design) return
        _appearance.value = _appearance.value.copy(design = design)
        preferences.edit().putString(DESIGN_KEY, design.storageKey).apply()
    }

    fun selectArtworkMotion(motion: ArtworkMotion) {
        if (_appearance.value.artworkMotion == motion) return
        _appearance.value = _appearance.value.copy(artworkMotion = motion)
        preferences.edit().putString(ARTWORK_MOTION_KEY, motion.storageKey).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "display_preferences"
        const val DESIGN_KEY = "design"
        const val PALETTE_KEY = "palette"
        const val ARTWORK_MOTION_KEY = "artwork_motion"
        const val LEGACY_SKIN_KEY = "skin"
    }
}
