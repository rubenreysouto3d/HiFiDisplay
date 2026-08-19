package com.rubenreysouto.hifidisplay.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientInteractionTest {
    @Test
    fun `timeout hides controls in ambient mode`() {
        val state = AmbientInteractionState().reduce(AmbientInteractionEvent.TIMEOUT)

        assertFalse(state.controlsVisible)
    }

    @Test
    fun `touch reveals controls and restarts timeout identity`() {
        val hidden = AmbientInteractionState(controlsVisible = false, interactionId = 4)
        val revealed = hidden.reduce(AmbientInteractionEvent.REVEAL_CONTROLS)

        assertTrue(revealed.controlsVisible)
        assertTrue(revealed.interactionId > hidden.interactionId)
    }

    @Test
    fun `surface touch toggles controls with an observable result`() {
        val visible = AmbientInteractionState(controlsVisible = true, interactionId = 4)
        val hidden = visible.reduce(AmbientInteractionEvent.TOGGLE_CONTROLS)
        val revealed = hidden.reduce(AmbientInteractionEvent.TOGGLE_CONTROLS)

        assertFalse(hidden.controlsVisible)
        assertTrue(revealed.controlsVisible)
        assertTrue(revealed.interactionId > visible.interactionId)
    }

    @Test
    fun `transport interaction keeps controls visible and restarts timeout`() {
        val visible = AmbientInteractionState(controlsVisible = true, interactionId = 4)
        val keptAlive = visible.reduce(AmbientInteractionEvent.KEEP_ALIVE)

        assertTrue(keptAlive.controlsVisible)
        assertTrue(keptAlive.interactionId > visible.interactionId)
    }

    @Test
    fun `overlay prevents accidental control toggle`() {
        val overlay = AmbientInteractionState(controlsVisible = true, overlayOpen = true, interactionId = 4)

        assertTrue(overlay.reduce(AmbientInteractionEvent.TOGGLE_CONTROLS).controlsVisible)
    }

    @Test
    fun `open overlay keeps controls visible through timeout`() {
        val open = AmbientInteractionState(controlsVisible = false)
            .reduce(AmbientInteractionEvent.OVERLAY_OPENED)
        val afterTimeout = open.reduce(AmbientInteractionEvent.TIMEOUT)

        assertTrue(afterTimeout.controlsVisible)
        assertTrue(afterTimeout.overlayOpen)
    }

    @Test
    fun `closing overlay restarts visible interval`() {
        val open = AmbientInteractionState(overlayOpen = true, interactionId = 2)
        val closed = open.reduce(AmbientInteractionEvent.OVERLAY_CLOSED)

        assertTrue(closed.controlsVisible)
        assertFalse(closed.overlayOpen)
        assertTrue(closed.interactionId > open.interactionId)
    }
}
