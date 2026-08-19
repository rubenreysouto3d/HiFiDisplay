package com.rubenreysouto.hifidisplay.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.rubenreysouto.hifidisplay.preferences.migratePaletteFamily
import com.rubenreysouto.hifidisplay.preferences.migratePaletteMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayAppearanceTest {
    @Test
    fun `stored palette is restored independently`() {
        assertEquals(ColorPalette.WARM_AMBER, ColorPalette.fromStorage("amber"))
    }

    @Test
    fun `stored display design is restored independently`() {
        assertEquals(DisplayDesign.STUDIO_LEDGER, DisplayDesign.fromStorage("studio-ledger"))
        assertEquals(DisplayDesign.MONOLITH_GLASS, DisplayDesign.fromStorage("monolith-glass"))
        assertEquals(DisplayDesign.PRECISION_DECK, DisplayDesign.fromStorage("precision-deck"))
        assertEquals(DisplayDesign.CRYSTAL_ATRIUM, DisplayDesign.fromStorage("crystal-atrium"))
    }

    @Test
    fun `stored artwork motion is restored independently`() {
        assertEquals(ArtworkMotion.DISSOLVE, ArtworkMotion.fromStorage("dissolve"))
        assertEquals(ArtworkMotion.DECK, ArtworkMotion.fromStorage("deck"))
        assertEquals(ArtworkMotion.DIRECT, ArtworkMotion.fromStorage("direct"))
    }

    @Test
    fun `unknown appearance values use safe defaults`() {
        assertEquals(ColorPalette.HIFI_GREEN, ColorPalette.fromStorage("future-palette"))
        assertEquals(DisplayDesign.MODERN_REFERENCE, DisplayDesign.fromStorage("future-design"))
        assertEquals(ArtworkMotion.FOCUS, ArtworkMotion.fromStorage("future-motion"))
        assertEquals(PlaybackArtworkEffect.PULSE, PlaybackArtworkEffect.fromStorage("future-effect"))
        assertEquals(PaletteMode.DARK, PaletteMode.fromStorage("future-mode"))
    }

    @Test
    fun `stored playback effect is restored independently`() {
        assertEquals(PlaybackArtworkEffect.DRIFT, PlaybackArtworkEffect.fromStorage("drift"))
        assertEquals(PlaybackArtworkEffect.HALO, PlaybackArtworkEffect.fromStorage("halo"))
        assertEquals(PlaybackArtworkEffect.STILL, PlaybackArtworkEffect.fromStorage("still"))
    }

    @Test
    fun `palette changes do not alter display design`() {
        val original = DisplayAppearance()
        val recolored = original.copy(palette = ColorPalette.WARM_AMBER)

        assertEquals(original.design, recolored.design)
        assertNotEquals(original.palette, recolored.palette)
    }

    @Test
    fun `artwork motion changes independently from design and palette`() {
        val original = DisplayAppearance(
            design = DisplayDesign.STUDIO_LEDGER,
            palette = ColorPalette.WARM_AMBER,
        )
        val direct = original.copy(artworkMotion = ArtworkMotion.DIRECT)

        assertEquals(original.design, direct.design)
        assertEquals(original.palette, direct.palette)
        assertNotEquals(original.artworkMotion, direct.artworkMotion)
    }

    @Test
    fun `each palette has a distinct accent`() {
        val accents = ColorPalette.entries.map { it.colors(PaletteMode.DARK).accent }
        assertEquals(ColorPalette.entries.size, accents.distinct().size)
        assertNotEquals(
            ColorPalette.HIFI_GREEN.colors(PaletteMode.DARK).accent,
            ColorPalette.WARM_AMBER.colors(PaletteMode.DARK).accent,
        )
    }

    @Test
    fun `new palettes restore from stable storage keys`() {
        assertEquals(ColorPalette.ARCTIC_SILVER, ColorPalette.fromStorage("arctic-silver"))
        assertEquals(ColorPalette.COBALT_NIGHT, ColorPalette.fromStorage("cobalt-night"))
        assertEquals(ColorPalette.VELVET_VIOLET, ColorPalette.fromStorage("velvet-violet"))
        assertEquals(ColorPalette.RUBY_SIGNAL, ColorPalette.fromStorage("ruby-signal"))
    }

    @Test
    fun `legacy combined palettes migrate to family and light level`() {
        assertEquals(ColorPalette.WARM_AMBER.storageKey, migratePaletteFamily("champagne-frost"))
        assertEquals(PaletteMode.LIGHT, migratePaletteMode("champagne-frost"))
        assertEquals(ColorPalette.HIFI_GREEN.storageKey, migratePaletteFamily("oled-absolute"))
        assertEquals(PaletteMode.OLED, migratePaletteMode("oled-absolute"))
        assertEquals(ColorPalette.ARCTIC_SILVER.storageKey, migratePaletteFamily("arctic-silver"))
        assertEquals(PaletteMode.LIGHT, migratePaletteMode("arctic-silver"))
    }

    @Test
    fun `oled mode gives every family a fully opaque absolute black background`() {
        ColorPalette.entries.forEach { palette ->
            val oled = palette.colors(PaletteMode.OLED)

            assertEquals(Color.Black, oled.background)
            assertEquals(1f, oled.background.alpha)
        }
    }

    @Test
    fun `light mode gives every family dark text over genuinely light surfaces`() {
        ColorPalette.entries.forEach { palette ->
            val colors = palette.colors(PaletteMode.LIGHT)
            assertTrue(colors.background.luminance() > colors.primaryText.luminance())
            assertTrue(colors.surfaceRaised.luminance() > colors.secondaryText.luminance())
        }
    }

    @Test
    fun `light level changes independently from color family and design`() {
        val original = DisplayAppearance(
            design = DisplayDesign.CRYSTAL_ATRIUM,
            palette = ColorPalette.VELVET_VIOLET,
            paletteMode = PaletteMode.DARK,
        )
        val light = original.copy(paletteMode = PaletteMode.LIGHT)

        assertEquals(original.design, light.design)
        assertEquals(original.palette, light.palette)
        assertNotEquals(original.paletteMode, light.paletteMode)
    }

    @Test
    fun `designs provide genuinely different structural tokens`() {
        val reference = DisplayDesign.MODERN_REFERENCE.tokens
        val studio = DisplayDesign.STUDIO_LEDGER.tokens

        assertNotEquals(reference.artworkPlacement, studio.artworkPlacement)
        assertNotEquals(reference.artworkTreatment, studio.artworkTreatment)
        assertNotEquals(reference.controlTreatment, studio.controlTreatment)
        assertNotEquals(reference.progressTreatment, studio.progressTreatment)

        val allTokens = DisplayDesign.entries.map(DisplayDesign::tokens)
        assertEquals(DisplayDesign.entries.size, allTokens.map { it.artworkTreatment }.distinct().size)
        assertEquals(DisplayDesign.entries.size, allTokens.map { it.controlTreatment }.distinct().size)
        assertEquals(DisplayDesign.entries.size, allTokens.map { it.progressTreatment }.distinct().size)
    }

    @Test
    fun `layout resolver protects compact landscape displays`() {
        assertEquals(DisplayLayoutMode.COMPACT, resolveDisplayLayoutMode(widthDp = 620f, heightDp = 360f))
        assertEquals(DisplayLayoutMode.COMPACT, resolveDisplayLayoutMode(widthDp = 900f, heightDp = 280f))
    }

    @Test
    fun `layout resolver distinguishes standard and wide displays`() {
        assertEquals(DisplayLayoutMode.STANDARD, resolveDisplayLayoutMode(widthDp = 900f, heightDp = 400f))
        assertEquals(DisplayLayoutMode.WIDE, resolveDisplayLayoutMode(widthDp = 1_100f, heightDp = 500f))
    }


    @Test
    fun `artwork transition identity is stable across bitmap refreshes`() {
        val first = buildArtworkTransitionKey("music.app", "Track", "Artist", "Album", 180_000L, true)
        val refreshed = buildArtworkTransitionKey("music.app", "Track", "Artist", "Album", 180_000L, true)

        assertEquals(first, refreshed)
    }

    @Test
    fun `artwork transition identity changes for meaningful display changes`() {
        val base = buildArtworkTransitionKey("music.app", "Track A", "Artist", "Album", 180_000L, true)

        assertNotEquals(base, buildArtworkTransitionKey("music.app", "Track B", "Artist", "Album", 180_000L, true))
        assertNotEquals(base, buildArtworkTransitionKey("other.app", "Track A", "Artist", "Album", 180_000L, true))
        assertNotEquals(base, buildArtworkTransitionKey("music.app", "Track A", "Artist", "Album", 180_000L, false))
    }
}
