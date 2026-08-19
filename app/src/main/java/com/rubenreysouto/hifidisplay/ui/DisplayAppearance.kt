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

enum class ColorPalette(
    val storageKey: String,
    val displayName: String,
    val descriptor: String,
    val isLight: Boolean = false,
    val isOled: Boolean = false,
) {
    HIFI_GREEN("green", "Hi-Fi Green", "DARK · SIGNATURE GREEN"),
    WARM_AMBER("amber", "Warm Amber", "DARK · VALVE WARMTH"),
    OLED_ABSOLUTE("oled-absolute", "OLED Absolute", "TRUE BLACK · PIXELS OFF", isOled = true),
    ARCTIC_SILVER("arctic-silver", "Arctic Silver", "LIGHT · COOL GLASS", isLight = true),
    CHAMPAGNE_FROST("champagne-frost", "Champagne Frost", "LIGHT · WARM GLASS", isLight = true),
    COBALT_NIGHT("cobalt-night", "Cobalt Night", "DARK · COBALT BLUE"),
    VELVET_VIOLET("velvet-violet", "Velvet Violet", "DARK · SOFT VIOLET"),
    RUBY_SIGNAL("ruby-signal", "Ruby Signal", "DARK · SIGNAL RED");

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
    val accentContent: Color,
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
            accentContent = Color(0xFF101607),
        )
        ColorPalette.WARM_AMBER -> DisplayColors(
            background = Color(0xFF0C0A07),
            surface = Color(0xFF17120D),
            surfaceRaised = Color(0xFF2A2117),
            primaryText = Color(0xFFFFF4E2),
            secondaryText = Color(0xFFAA9780),
            accent = Color(0xFFFFBC57),
            accentContent = Color(0xFF211305),
        )
        ColorPalette.OLED_ABSOLUTE -> DisplayColors(
            background = Color.Black,
            surface = Color(0xFF050605),
            surfaceRaised = Color(0xFF131613),
            primaryText = Color(0xFFE9EEE9),
            secondaryText = Color(0xFF747B75),
            accent = Color(0xFFC7FF77),
            accentContent = Color.Black,
        )
        ColorPalette.ARCTIC_SILVER -> DisplayColors(
            background = Color(0xFFE8EEF1),
            surface = Color(0xFFDCE5E9),
            surfaceRaised = Color(0xFFF6F9FA),
            primaryText = Color(0xFF17232A),
            secondaryText = Color(0xFF53656F),
            accent = Color(0xFF246984),
            accentContent = Color(0xFFF5FAFC),
        )
        ColorPalette.CHAMPAGNE_FROST -> DisplayColors(
            background = Color(0xFFEAE3D8),
            surface = Color(0xFFDDD2C2),
            surfaceRaised = Color(0xFFF8F2E9),
            primaryText = Color(0xFF2B241D),
            secondaryText = Color(0xFF6E6153),
            accent = Color(0xFF956326),
            accentContent = Color(0xFFFFF8ED),
        )
        ColorPalette.COBALT_NIGHT -> DisplayColors(
            background = Color(0xFF070A12),
            surface = Color(0xFF0D1423),
            surfaceRaised = Color(0xFF172A46),
            primaryText = Color(0xFFF1F5FF),
            secondaryText = Color(0xFF8799B7),
            accent = Color(0xFF75B9FF),
            accentContent = Color(0xFF07182B),
        )
        ColorPalette.VELVET_VIOLET -> DisplayColors(
            background = Color(0xFF0B0810),
            surface = Color(0xFF17101E),
            surfaceRaised = Color(0xFF2C1E37),
            primaryText = Color(0xFFF9F2FC),
            secondaryText = Color(0xFFA593AB),
            accent = Color(0xFFD8A5F4),
            accentContent = Color(0xFF211126),
        )
        ColorPalette.RUBY_SIGNAL -> DisplayColors(
            background = Color(0xFF0D0809),
            surface = Color(0xFF1A1012),
            surfaceRaised = Color(0xFF332025),
            primaryText = Color(0xFFFFF3F4),
            secondaryText = Color(0xFFAD9297),
            accent = Color(0xFFFF7F91),
            accentContent = Color(0xFF29090E),
        )
    }
