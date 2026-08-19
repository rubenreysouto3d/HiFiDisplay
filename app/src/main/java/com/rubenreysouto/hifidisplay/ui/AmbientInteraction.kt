package com.rubenreysouto.hifidisplay.ui

internal data class AmbientInteractionState(
    val controlsVisible: Boolean = true,
    val overlayOpen: Boolean = false,
    val interactionId: Long = 0L,
)

internal enum class AmbientInteractionEvent {
    INTERACT,
    TIMEOUT,
    OVERLAY_OPENED,
    OVERLAY_CLOSED,
}

internal fun AmbientInteractionState.reduce(
    event: AmbientInteractionEvent,
): AmbientInteractionState = when (event) {
    AmbientInteractionEvent.INTERACT -> copy(
        controlsVisible = true,
        interactionId = interactionId + 1L,
    )
    AmbientInteractionEvent.TIMEOUT -> if (overlayOpen) this else copy(controlsVisible = false)
    AmbientInteractionEvent.OVERLAY_OPENED -> copy(
        controlsVisible = true,
        overlayOpen = true,
        interactionId = interactionId + 1L,
    )
    AmbientInteractionEvent.OVERLAY_CLOSED -> copy(
        controlsVisible = true,
        overlayOpen = false,
        interactionId = interactionId + 1L,
    )
}
