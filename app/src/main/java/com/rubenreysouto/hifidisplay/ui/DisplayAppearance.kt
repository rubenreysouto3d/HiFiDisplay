package com.rubenreysouto.hifidisplay.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class DisplayDesign(
    val storageKey: String,
    val displayName: String,
    val descriptor: String,
) {
    MODERN_REFERENCE(
        storageKey = "modern-reference",
        displayName = "Modern Reference",
        descriptor = "SPACE · BALANCE · SILENCE",
    ),
    STUDIO_LEDGER(
        storageKey = "studio-ledger",
        displayName = "Studio Ledger",
        descriptor = "SIGNAL · INDEX · PRECISION",
    );

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

enum class ArtworkMotion(
    val storageKey: String,
    val displayName: String,
    val descriptor: String,
) {
    FOCUS(
        storageKey = "focus",
        displayName = "Focus",
        descriptor = "SUBTLE DEPTH · CALM ARRIVAL",
    ),
    DISSOLVE(
        storageKey = "dissolve",
        displayName = "Dissolve",
        descriptor = "CLEAN CROSSFADE · NO SCALE",
    ),
    DIRECT(
        storageKey = "direct",
        displayName = "Direct",
        descriptor = "INSTANT · NO ANIMATION",
    );

    companion object {
        fun fromStorage(value: String?): ArtworkMotion =
            entries.firstOrNull { it.storageKey == value } ?: FOCUS
    }
}

data class DisplayAppearance(
    val design: DisplayDesign = DisplayDesign.MODERN_REFERENCE,
    val palette: ColorPalette = ColorPalette.HIFI_GREEN,
    val artworkMotion: ArtworkMotion = ArtworkMotion.FOCUS,
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
