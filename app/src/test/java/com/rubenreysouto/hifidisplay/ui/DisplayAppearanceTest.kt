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
}
