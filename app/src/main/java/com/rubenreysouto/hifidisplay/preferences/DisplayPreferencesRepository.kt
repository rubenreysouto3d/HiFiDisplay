package com.rubenreysouto.hifidisplay.preferences

import android.content.Context
import com.rubenreysouto.hifidisplay.ui.ArtworkMotion
import com.rubenreysouto.hifidisplay.ui.ColorPalette
import com.rubenreysouto.hifidisplay.ui.DisplayAppearance
import com.rubenreysouto.hifidisplay.ui.DisplayDesign
import com.rubenreysouto.hifidisplay.ui.PlaybackArtworkEffect
import com.rubenreysouto.hifidisplay.ui.PaletteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayPreferencesRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val storedPalette = preferences.getString(PALETTE_KEY, null)
        ?: preferences.getString(LEGACY_SKIN_KEY, null)
    private val _appearance = MutableStateFlow(
        DisplayAppearance(
            design = DisplayDesign.fromStorage(preferences.getString(DESIGN_KEY, null)),
            palette = ColorPalette.fromStorage(migratePaletteFamily(storedPalette)),
            paletteMode = preferences.getString(PALETTE_MODE_KEY, null)?.let(PaletteMode::fromStorage)
                ?: migratePaletteMode(storedPalette),
            artworkMotion = ArtworkMotion.fromStorage(preferences.getString(ARTWORK_MOTION_KEY, null)),
            playbackArtworkEffect = PlaybackArtworkEffect.fromStorage(
                preferences.getString(PLAYBACK_ARTWORK_EFFECT_KEY, null),
            ),
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

    fun selectPaletteMode(mode: PaletteMode) {
        if (_appearance.value.paletteMode == mode) return
        _appearance.value = _appearance.value.copy(paletteMode = mode)
        preferences.edit().putString(PALETTE_MODE_KEY, mode.storageKey).apply()
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

    fun selectPlaybackArtworkEffect(effect: PlaybackArtworkEffect) {
        if (_appearance.value.playbackArtworkEffect == effect) return
        _appearance.value = _appearance.value.copy(playbackArtworkEffect = effect)
        preferences.edit().putString(PLAYBACK_ARTWORK_EFFECT_KEY, effect.storageKey).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "display_preferences"
        const val DESIGN_KEY = "design"
        const val PALETTE_KEY = "palette"
        const val PALETTE_MODE_KEY = "palette_mode"
        const val ARTWORK_MOTION_KEY = "artwork_motion"
        const val PLAYBACK_ARTWORK_EFFECT_KEY = "playback_artwork_effect"
        const val LEGACY_SKIN_KEY = "skin"

    }
}

internal fun migratePaletteFamily(value: String?): String? = when (value) {
    "champagne-frost" -> ColorPalette.WARM_AMBER.storageKey
    "oled-absolute" -> ColorPalette.HIFI_GREEN.storageKey
    else -> value
}

internal fun migratePaletteMode(value: String?): PaletteMode = when (value) {
    "arctic-silver", "champagne-frost" -> PaletteMode.LIGHT
    "oled-absolute" -> PaletteMode.OLED
    else -> PaletteMode.DARK
}
