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
) {
    HIFI_GREEN("green", "Reference Green", "SIGNATURE · FRESH"),
    WARM_AMBER("amber", "Valve Amber", "WARM · ANALOG"),
    ARCTIC_SILVER("arctic-silver", "Arctic Cyan", "COOL · PRECISE"),
    COBALT_NIGHT("cobalt-night", "Cobalt", "DEEP · ELECTRIC"),
    VELVET_VIOLET("velvet-violet", "Velvet", "SOFT · VIOLET"),
    RUBY_SIGNAL("ruby-signal", "Ruby", "FOCUSED · SIGNAL");

    companion object {
        fun fromStorage(value: String?): ColorPalette =
            entries.firstOrNull { it.storageKey == value } ?: HIFI_GREEN
    }
}

enum class PaletteMode(
    val storageKey: String,
    val displayName: String,
    val descriptor: String,
) {
    DARK("dark", "Dark", "LOW LIGHT"),
    LIGHT("light", "Light", "CLEAR FIELD"),
    OLED("oled", "OLED", "TRUE BLACK");

    companion object {
        fun fromStorage(value: String?): PaletteMode =
            entries.firstOrNull { it.storageKey == value } ?: DARK
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
        displayName = "Breathe",
        descriptor = "GENTLE SCALE · VISUAL ONLY",
    ),
    DRIFT(
        storageKey = "drift",
        displayName = "Float",
        descriptor = "SLOW DEPTH · VISUAL ONLY",
    ),
    HALO(
        storageKey = "halo",
        displayName = "Glow",
        descriptor = "EDGE LIGHT · STABLE COVER",
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
    val paletteMode: PaletteMode = PaletteMode.DARK,
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

internal fun ColorPalette.colors(mode: PaletteMode): DisplayColors = when (mode) {
    PaletteMode.DARK -> when (this) {
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
        ColorPalette.ARCTIC_SILVER -> DisplayColors(
            background = Color(0xFF071014),
            surface = Color(0xFF0D1A20),
            surfaceRaised = Color(0xFF17303A),
            primaryText = Color(0xFFEFF9FB),
            secondaryText = Color(0xFF87A8B0),
            accent = Color(0xFF70D7E8),
            accentContent = Color(0xFF041B20),
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

    PaletteMode.LIGHT -> when (this) {
        ColorPalette.HIFI_GREEN -> DisplayColors(
            background = Color(0xFFE8EDE4), surface = Color(0xFFDCE4D6), surfaceRaised = Color(0xFFF5F8F2),
            primaryText = Color(0xFF182017), secondaryText = Color(0xFF596857), accent = Color(0xFF4D741B), accentContent = Color(0xFFF7FBEF),
        )
        ColorPalette.WARM_AMBER -> DisplayColors(
            background = Color(0xFFECE4D8), surface = Color(0xFFE0D3C1), surfaceRaised = Color(0xFFFAF4EA),
            primaryText = Color(0xFF2B241D), secondaryText = Color(0xFF706151), accent = Color(0xFF956326), accentContent = Color(0xFFFFF8ED),
        )
        ColorPalette.ARCTIC_SILVER -> DisplayColors(
            background = Color(0xFFE5EEF1), surface = Color(0xFFD6E4E8), surfaceRaised = Color(0xFFF4F9FA),
            primaryText = Color(0xFF15252B), secondaryText = Color(0xFF506A72), accent = Color(0xFF19748A), accentContent = Color(0xFFF1FBFD),
        )
        ColorPalette.COBALT_NIGHT -> DisplayColors(
            background = Color(0xFFE5EAF3), surface = Color(0xFFD7DFEC), surfaceRaised = Color(0xFFF5F7FC),
            primaryText = Color(0xFF182235), secondaryText = Color(0xFF586981), accent = Color(0xFF315F9E), accentContent = Color(0xFFF5F8FF),
        )
        ColorPalette.VELVET_VIOLET -> DisplayColors(
            background = Color(0xFFECE6EF), surface = Color(0xFFDFD5E4), surfaceRaised = Color(0xFFFAF6FB),
            primaryText = Color(0xFF2A2030), secondaryText = Color(0xFF71627A), accent = Color(0xFF805096), accentContent = Color(0xFFFFF8FF),
        )
        ColorPalette.RUBY_SIGNAL -> DisplayColors(
            background = Color(0xFFF0E5E7), surface = Color(0xFFE5D4D8), surfaceRaised = Color(0xFFFFF7F8),
            primaryText = Color(0xFF301E22), secondaryText = Color(0xFF7A5E65), accent = Color(0xFFA13D51), accentContent = Color(0xFFFFF7F8),
        )
    }

    PaletteMode.OLED -> when (this) {
        ColorPalette.HIFI_GREEN -> oledColors(Color(0xFFC7FF77), Color(0xFF101607))
        ColorPalette.WARM_AMBER -> oledColors(Color(0xFFFFBC57), Color(0xFF211305))
        ColorPalette.ARCTIC_SILVER -> oledColors(Color(0xFF70D7E8), Color(0xFF041B20))
        ColorPalette.COBALT_NIGHT -> oledColors(Color(0xFF75B9FF), Color(0xFF07182B))
        ColorPalette.VELVET_VIOLET -> oledColors(Color(0xFFD8A5F4), Color(0xFF211126))
        ColorPalette.RUBY_SIGNAL -> oledColors(Color(0xFFFF7F91), Color(0xFF29090E))
    }
}

private fun oledColors(accent: Color, accentContent: Color) = DisplayColors(
    background = Color.Black,
    surface = Color(0xFF040504),
    surfaceRaised = Color(0xFF111411),
    primaryText = Color(0xFFE9EEE9),
    secondaryText = Color(0xFF747B75),
    accent = accent,
    accentContent = accentContent,
)
