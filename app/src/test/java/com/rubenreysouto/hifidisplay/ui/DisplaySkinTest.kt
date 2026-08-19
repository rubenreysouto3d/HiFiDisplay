package com.rubenreysouto.hifidisplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DisplaySkinTest {
    @Test
    fun `stored skin is restored`() {
        assertEquals(DisplaySkin.AMBER, DisplaySkin.fromStorage("amber"))
    }

    @Test
    fun `unknown or missing skin falls back to green`() {
        assertEquals(DisplaySkin.GREEN, DisplaySkin.fromStorage(null))
        assertEquals(DisplaySkin.GREEN, DisplaySkin.fromStorage("future-skin"))
    }

    @Test
    fun `each skin has a distinct accent`() {
        assertNotEquals(DisplaySkin.GREEN.palette.accent, DisplaySkin.AMBER.palette.accent)
    }
}
