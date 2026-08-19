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
    ),
    MONOLITH_GLASS(
        storageKey = "monolith-glass",
        displayName = "Monolith Glass",
        descriptor = "CINEMA · DEPTH · PRESENCE",
    ),
    PRECISION_DECK(
        storageKey = "precision-deck",
        displayName = "Precision Deck",
        descriptor = "TIMEBASE · CONTROL · DETAIL",
    ),
    CRYSTAL_ATRIUM(
        storageKey = "crystal-atrium",
        displayName = "Crystal Atrium",
        descriptor = "LIGHT · CLARITY · SUSPENSION",
    );

    companion object {
        fun fromStorage(value: String?): DisplayDesign =
            entries.firstOrNull { it.storageKey == value } ?: MODERN_REFERENCE
    }
}

enum class ColorPalette(val storageKey: String, val displayName: String) {
    HIFI_GREEN("green", "Hi-Fi Green"),
    WARM_AMBER("amber", "Warm Amber"),
    ARCTIC_SILVER("arctic-silver", "Arctic Silver"),
    COBALT_NIGHT("cobalt-night", "Cobalt Night"),
    VELVET_VIOLET("velvet-violet", "Velvet Violet"),
    RUBY_SIGNAL("ruby-signal", "Ruby Signal");

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
    DECK(
        storageKey = "deck",
        displayName = "Deck",
        descriptor = "DEFINED SLIDE · PHYSICAL CHANGEOVER",
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

enum class PlaybackArtworkEffect(
    val storageKey: String,
    val displayName: String,
    val descriptor: String,
) {
    PULSE(
        storageKey = "pulse",
        displayName = "Pulse",
        descriptor = "GENTLE CADENCE · PLAYBACK ACTIVE",
    ),
    DRIFT(
        storageKey = "drift",
        displayName = "Drift",
        descriptor = "SLOW DEPTH · CINEMATIC MOTION",
    ),
    HALO(
        storageKey = "halo",
        displayName = "Halo",
        descriptor = "STABLE COVER · BREATHING LIGHT",
    ),
    STILL(
        storageKey = "still",
        displayName = "Still",
        descriptor = "STATIC · ZERO MOTION",
    );

    companion object {
        fun fromStorage(value: String?): PlaybackArtworkEffect =
            entries.firstOrNull { it.storageKey == value } ?: PULSE
    }
}

data class DisplayAppearance(
    val design: DisplayDesign = DisplayDesign.MODERN_REFERENCE,
    val palette: ColorPalette = ColorPalette.HIFI_GREEN,
    val artworkMotion: ArtworkMotion = ArtworkMotion.FOCUS,
    val playbackArtworkEffect: PlaybackArtworkEffect = PlaybackArtworkEffect.PULSE,
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
        ColorPalette.ARCTIC_SILVER -> DisplayColors(
            background = Color(0xFF090C10),
            surface = Color(0xFF11171D),
            surfaceRaised = Color(0xFF222D36),
            primaryText = Color(0xFFF4F8FA),
            secondaryText = Color(0xFF95A4AE),
            accent = Color(0xFFD8F1F5),
        )
        ColorPalette.COBALT_NIGHT -> DisplayColors(
            background = Color(0xFF070A12),
            surface = Color(0xFF0D1423),
            surfaceRaised = Color(0xFF172A46),
            primaryText = Color(0xFFF1F5FF),
            secondaryText = Color(0xFF8799B7),
            accent = Color(0xFF75B9FF),
        )
        ColorPalette.VELVET_VIOLET -> DisplayColors(
            background = Color(0xFF0B0810),
            surface = Color(0xFF17101E),
            surfaceRaised = Color(0xFF2C1E37),
            primaryText = Color(0xFFF9F2FC),
            secondaryText = Color(0xFFA593AB),
            accent = Color(0xFFD8A5F4),
        )
        ColorPalette.RUBY_SIGNAL -> DisplayColors(
            background = Color(0xFF0D0809),
            surface = Color(0xFF1A1012),
            surfaceRaised = Color(0xFF332025),
            primaryText = Color(0xFFFFF3F4),
            secondaryText = Color(0xFFAD9297),
            accent = Color(0xFFFF7F91),
        )
    }
