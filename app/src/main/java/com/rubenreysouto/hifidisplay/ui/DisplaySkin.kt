package com.rubenreysouto.hifidisplay.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class DisplaySkin(val storageKey: String, val displayName: String) {
    GREEN("green", "Hi-Fi Green"),
    AMBER("amber", "Warm Amber");

    companion object {
        fun fromStorage(value: String?): DisplaySkin = entries.firstOrNull { it.storageKey == value } ?: GREEN
    }
}

@Immutable
internal data class DisplayPalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
)

internal val DisplaySkin.palette: DisplayPalette
    get() = when (this) {
        DisplaySkin.GREEN -> DisplayPalette(
            background = Color(0xFF090B0D),
            surface = Color(0xFF111519),
            surfaceRaised = Color(0xFF20262B),
            primaryText = Color(0xFFF4F6F0),
            secondaryText = Color(0xFF929A92),
            accent = Color(0xFFD6FF7F),
        )
        DisplaySkin.AMBER -> DisplayPalette(
            background = Color(0xFF0C0A07),
            surface = Color(0xFF17120D),
            surfaceRaised = Color(0xFF2A2117),
            primaryText = Color(0xFFFFF4E2),
            secondaryText = Color(0xFFAA9780),
            accent = Color(0xFFFFBC57),
        )
    }
