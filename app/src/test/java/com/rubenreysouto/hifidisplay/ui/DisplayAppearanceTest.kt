package com.rubenreysouto.hifidisplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DisplayAppearanceTest {
    @Test
    fun `stored palette is restored independently`() {
        assertEquals(ColorPalette.WARM_AMBER, ColorPalette.fromStorage("amber"))
    }

    @Test
    fun `stored display design is restored independently`() {
        assertEquals(DisplayDesign.STUDIO_LEDGER, DisplayDesign.fromStorage("studio-ledger"))
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
        assertNotEquals(ColorPalette.HIFI_GREEN.colors.accent, ColorPalette.WARM_AMBER.colors.accent)
    }

    @Test
    fun `designs provide genuinely different structural tokens`() {
        val reference = DisplayDesign.MODERN_REFERENCE.tokens
        val studio = DisplayDesign.STUDIO_LEDGER.tokens

        assertNotEquals(reference.artworkPlacement, studio.artworkPlacement)
        assertNotEquals(reference.artworkTreatment, studio.artworkTreatment)
        assertNotEquals(reference.controlTreatment, studio.controlTreatment)
        assertNotEquals(reference.progressTreatment, studio.progressTreatment)
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
