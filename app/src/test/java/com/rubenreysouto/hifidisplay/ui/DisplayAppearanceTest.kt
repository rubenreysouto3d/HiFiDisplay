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
    fun `unknown appearance values use safe defaults`() {
        assertEquals(ColorPalette.HIFI_GREEN, ColorPalette.fromStorage("future-palette"))
        assertEquals(DisplayDesign.MODERN_REFERENCE, DisplayDesign.fromStorage("future-design"))
    }

    @Test
    fun `palette changes do not alter display design`() {
        val original = DisplayAppearance()
        val recolored = original.copy(palette = ColorPalette.WARM_AMBER)

        assertEquals(original.design, recolored.design)
        assertNotEquals(original.palette, recolored.palette)
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
}
