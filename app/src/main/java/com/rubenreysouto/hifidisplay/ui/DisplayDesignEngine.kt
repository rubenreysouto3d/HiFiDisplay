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
    MONOLITH_GLASS,
    PRECISION_DIAL,
    CRYSTAL_FLOAT,
}

internal enum class ProgressTreatment {
    CONTINUOUS,
    TICKED,
    LUMINOUS,
    SEGMENTED,
    PRISMATIC,
}

internal enum class ControlTreatment {
    CIRCULAR,
    CONSOLE,
    GLASS,
    MACHINED,
    FLOATING_GLASS,
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

        DisplayDesign.MONOLITH_GLASS -> DisplayDesignTokens(
            artworkPlacement = ArtworkPlacement.LEADING,
            artworkTreatment = ArtworkTreatment.MONOLITH_GLASS,
            progressTreatment = ProgressTreatment.LUMINOUS,
            controlTreatment = ControlTreatment.GLASS,
            horizontalPadding = 24.dp,
            verticalPadding = 20.dp,
            contentGap = 28.dp,
            artworkCornerRadius = 18.dp,
            sourceCornerRadius = 12.dp,
        )

        DisplayDesign.PRECISION_DECK -> DisplayDesignTokens(
            artworkPlacement = ArtworkPlacement.TRAILING,
            artworkTreatment = ArtworkTreatment.PRECISION_DIAL,
            progressTreatment = ProgressTreatment.SEGMENTED,
            controlTreatment = ControlTreatment.MACHINED,
            horizontalPadding = 38.dp,
            verticalPadding = 24.dp,
            contentGap = 42.dp,
            artworkCornerRadius = 4.dp,
            sourceCornerRadius = 3.dp,
        )

        DisplayDesign.CRYSTAL_ATRIUM -> DisplayDesignTokens(
            artworkPlacement = ArtworkPlacement.LEADING,
            artworkTreatment = ArtworkTreatment.CRYSTAL_FLOAT,
            progressTreatment = ProgressTreatment.PRISMATIC,
            controlTreatment = ControlTreatment.FLOATING_GLASS,
            horizontalPadding = 28.dp,
            verticalPadding = 22.dp,
            contentGap = 30.dp,
            artworkCornerRadius = 22.dp,
            sourceCornerRadius = 18.dp,
        )
    }

internal fun resolveDisplayLayoutMode(widthDp: Float, heightDp: Float): DisplayLayoutMode = when {
    heightDp < 300f || widthDp < 640f -> DisplayLayoutMode.COMPACT
    widthDp >= 1_000f && heightDp >= 420f -> DisplayLayoutMode.WIDE
    else -> DisplayLayoutMode.STANDARD
}

internal fun buildArtworkTransitionKey(
    sourcePackage: String?,
    title: String?,
    artist: String?,
    album: String?,
    durationMs: Long?,
    hasArtwork: Boolean,
): String = listOf(
    sourcePackage,
    title,
    artist,
    album,
    durationMs?.toString(),
    if (hasArtwork) "art" else "fallback",
).joinToString(separator = "|") { it.orEmpty() }
