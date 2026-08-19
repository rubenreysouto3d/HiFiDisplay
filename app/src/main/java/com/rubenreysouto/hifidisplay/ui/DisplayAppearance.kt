package com.rubenreysouto.hifidisplay.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class DisplayDesign(val storageKey: String, val displayName: String) {
    MODERN_REFERENCE("modern-reference", "Modern Reference");

    companion object {
        fun fromStorage(value: String?): DisplayDesign =
            entries.firstOrNull { it.storageKey == value } ?: MODERN_REFERENCE
    }
}

enum class ColorPalette(val storageKey: String, val displayName: String) {
    HIFI_GREEN("green", "Hi-Fi Green"),
    WARM_AMBER("amber", "Warm Amber");

    companion object {
        fun fromStorage(value: String?): ColorPalette =
            entries.firstOrNull { it.storageKey == value } ?: HIFI_GREEN
    }
}

data class DisplayAppearance(
    val design: DisplayDesign = DisplayDesign.MODERN_REFERENCE,
    val palette: ColorPalette = ColorPalette.HIFI_GREEN,
)

@Immutable
internal data class DisplayColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
)

internal val ColorPalette.colors: DisplayColors
    get() = when (this) {
        ColorPalette.HIFI_GREEN -> DisplayColors(
            background = Color(0xFF090B0D),
            surface = Color(0xFF111519),
            surfaceRaised = Color(0xFF20262B),
            primaryText = Color(0xFFF4F6F0),
            secondaryText = Color(0xFF929A92),
            accent = Color(0xFFD6FF7F),
        )
        ColorPalette.WARM_AMBER -> DisplayColors(
            background = Color(0xFF0C0A07),
            surface = Color(0xFF17120D),
            surfaceRaised = Color(0xFF2A2117),
            primaryText = Color(0xFFFFF4E2),
            secondaryText = Color(0xFFAA9780),
            accent = Color(0xFFFFBC57),
        )
    }
