package com.rubenreysouto.hifidisplay.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class ArtworkPlacement {
    LEADING,
    TRAILING,
}

internal enum class ArtworkTreatment {
    REFERENCE,
    STUDIO_DECK,
}

internal enum class ProgressTreatment {
    CONTINUOUS,
    TICKED,
}

internal enum class ControlTreatment {
    CIRCULAR,
    CONSOLE,
}

internal enum class DisplayLayoutMode {
    COMPACT,
    STANDARD,
    WIDE,
}

@Immutable
internal data class DisplayDesignTokens(
    val artworkPlacement: ArtworkPlacement,
    val artworkTreatment: ArtworkTreatment,
    val progressTreatment: ProgressTreatment,
    val controlTreatment: ControlTreatment,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentGap: Dp,
    val artworkCornerRadius: Dp,
    val sourceCornerRadius: Dp,
)

internal val DisplayDesign.tokens: DisplayDesignTokens
    get() = when (this) {
        DisplayDesign.MODERN_REFERENCE -> DisplayDesignTokens(
            artworkPlacement = ArtworkPlacement.LEADING,
            artworkTreatment = ArtworkTreatment.REFERENCE,
            progressTreatment = ProgressTreatment.CONTINUOUS,
            controlTreatment = ControlTreatment.CIRCULAR,
            horizontalPadding = 36.dp,
            verticalPadding = 28.dp,
            contentGap = 40.dp,
            artworkCornerRadius = 10.dp,
            sourceCornerRadius = 5.dp,
        )

        DisplayDesign.STUDIO_LEDGER -> DisplayDesignTokens(
            artworkPlacement = ArtworkPlacement.TRAILING,
            artworkTreatment = ArtworkTreatment.STUDIO_DECK,
            progressTreatment = ProgressTreatment.TICKED,
            controlTreatment = ControlTreatment.CONSOLE,
            horizontalPadding = 42.dp,
            verticalPadding = 24.dp,
            contentGap = 52.dp,
            artworkCornerRadius = 2.dp,
            sourceCornerRadius = 2.dp,
        )
    }

internal fun resolveDisplayLayoutMode(widthDp: Float, heightDp: Float): DisplayLayoutMode = when {
    heightDp < 300f || widthDp < 640f -> DisplayLayoutMode.COMPACT
    widthDp >= 1_000f && heightDp >= 420f -> DisplayLayoutMode.WIDE
    else -> DisplayLayoutMode.STANDARD
}
