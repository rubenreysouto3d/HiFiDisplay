package com.rubenreysouto.hifidisplay.ui

import android.graphics.Bitmap
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubenreysouto.hifidisplay.media.MediaUiState
import com.rubenreysouto.hifidisplay.media.SessionAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private val Background: Color @Composable get() = MaterialTheme.colorScheme.background
private val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
private val SurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val PrimaryText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val SecondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
private val LocalOledPalette = staticCompositionLocalOf { false }
private val OledPalette: Boolean @Composable get() = LocalOledPalette.current

@Composable
fun HiFiDisplayApp(
    state: MediaUiState,
    appearance: DisplayAppearance,
    onOpenAccessSettings: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectSource: (String?) -> Unit,
    onSelectDesign: (DisplayDesign) -> Unit,
    onSelectPalette: (ColorPalette) -> Unit,
    onSelectPaletteMode: (PaletteMode) -> Unit,
    onSelectArtworkMotion: (ArtworkMotion) -> Unit,
    onSelectPlaybackArtworkEffect: (PlaybackArtworkEffect) -> Unit,
) {
    val colors = appearance.palette.colors(appearance.paletteMode)
    var showSourcePicker by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showArtworkFocus by remember { mutableStateOf(false) }
    var interaction by remember { mutableStateOf(AmbientInteractionState()) }
    val activeDisplay = state.availability == SessionAvailability.ACTIVE
    fun dispatch(event: AmbientInteractionEvent) {
        interaction = interaction.reduce(event)
    }
    LaunchedEffect(
        activeDisplay,
        interaction.controlsVisible,
        interaction.overlayOpen,
        interaction.interactionId,
    ) {
        if (activeDisplay && interaction.controlsVisible && !interaction.overlayOpen) {
            delay(CONTROLS_TIMEOUT_MS)
            dispatch(AmbientInteractionEvent.TIMEOUT)
        }
    }
    val colorScheme = if (appearance.paletteMode == PaletteMode.LIGHT) {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceRaised,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            primary = colors.accent,
            onPrimary = colors.accentContent,
            outline = colors.secondaryText,
            scrim = colors.primaryText,
        )
    } else {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceRaised,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            primary = colors.accent,
            onPrimary = colors.accentContent,
            outline = colors.secondaryText,
            scrim = colors.background,
        )
    }
    CompositionLocalProvider(LocalOledPalette provides (appearance.paletteMode == PaletteMode.OLED)) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout),
                ) {
                when (state.availability) {
                    SessionAvailability.PERMISSION_REQUIRED -> AccessRequired(onOpenAccessSettings)
                    SessionAvailability.NO_SESSION -> EmptySession()
                    SessionAvailability.ERROR -> SessionError()
                    SessionAvailability.ACTIVE -> NowPlaying(
                        state = state,
                        design = appearance.design,
                        artworkMotion = appearance.artworkMotion,
                        playbackArtworkEffect = appearance.playbackArtworkEffect,
                        onPlay = onPlay,
                        onPause = onPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSeek = onSeek,
                        controlsVisible = interaction.controlsVisible,
                        onToggleControls = { dispatch(AmbientInteractionEvent.TOGGLE_CONTROLS) },
                        onKeepAlive = { dispatch(AmbientInteractionEvent.KEEP_ALIVE) },
                        onShowSources = {
                            showSourcePicker = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                        onShowDiagnostics = {
                            showDiagnostics = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                        onShowArtworkFocus = {
                            showArtworkFocus = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                    )
                }
                if (!activeDisplay) {
                    DisplayMenuButton(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 22.dp),
                        onClick = {
                            showSourcePicker = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                    )
                }
                if (showSourcePicker) {
                    SourcePickerOverlay(
                        state = state,
                        appearance = appearance,
                        onSelectSource = {
                            onSelectSource(it)
                            showSourcePicker = false
                            dispatch(AmbientInteractionEvent.OVERLAY_CLOSED)
                        },
                        onSelectDesign = onSelectDesign,
                        onSelectPalette = onSelectPalette,
                        onSelectPaletteMode = onSelectPaletteMode,
                        onSelectArtworkMotion = onSelectArtworkMotion,
                        onSelectPlaybackArtworkEffect = onSelectPlaybackArtworkEffect,
                        onShowDiagnostics = {
                            showSourcePicker = false
                            showDiagnostics = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                        onDismiss = {
                            showSourcePicker = false
                            dispatch(AmbientInteractionEvent.OVERLAY_CLOSED)
                        },
                    )
                }
                if (showDiagnostics) {
                    DiagnosticsOverlay(
                        state,
                        onDismiss = {
                            showDiagnostics = false
                            dispatch(AmbientInteractionEvent.OVERLAY_CLOSED)
                        },
                    )
                }
                if (showArtworkFocus) {
                    ArtworkFocusOverlay(
                        state = state,
                        onDismiss = {
                            showArtworkFocus = false
                            dispatch(AmbientInteractionEvent.OVERLAY_CLOSED)
                        },
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun DisplayMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (pressed) Accent.copy(alpha = .14f) else SurfaceRaised.copy(alpha = .94f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performPremiumHaptic()
                    onClick()
                },
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Tune, null, tint = Accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "SOURCE / DISPLAY",
            color = PrimaryText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun SessionError() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HI-FI DISPLAY", color = Accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(20.dp))
            Text("No se pudo acceder a las sesiones", color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("La conexión se reintentará automáticamente", color = SecondaryText)
        }
    }
}

@Composable
private fun AccessRequired(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("HI-FI DISPLAY", color = Accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
        Spacer(Modifier.height(24.dp))
        Text("Acceso multimedia necesario", color = PrimaryText, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text(
            "Android necesita que HiFiDisplay tenga acceso a notificaciones para localizar y controlar la sesión multimedia activa.",
            color = SecondaryText,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "HiFiDisplay no lee, muestra, guarda ni comparte el contenido de tus notificaciones.",
            color = SecondaryText.copy(alpha = .72f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
        ) {
            Text("ABRIR AJUSTES", letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun EmptySession() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HI-FI DISPLAY", color = Accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(20.dp))
            Text("Esperando una sesión multimedia", color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Inicia la reproducción en una app compatible", color = SecondaryText)
        }
    }
}

@Composable
private fun ArtworkFocusOverlay(
    state: MediaUiState,
    onDismiss: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Cerrar vista de portada",
                onClick = {
                    view.performPremiumHaptic()
                    onDismiss()
                },
            )
            .padding(horizontal = 42.dp, vertical = 28.dp),
    ) {
        val compact = maxHeight < 360.dp
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .fillMaxHeight(if (compact) .94f else .9f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(if (compact) 8.dp else 14.dp))
                    .background(Surface)
                    .border(1.dp, PrimaryText.copy(alpha = .16f), RoundedCornerShape(if (compact) 8.dp else 14.dp)),
            ) {
                ArtworkVisual(state, ArtworkMotion.DISSOLVE)
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(.62f)
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, PrimaryText.copy(alpha = .62f), Color.Transparent))),
                )
            }
            Spacer(Modifier.width(if (compact) 28.dp else 52.dp))
            Column(
                Modifier.widthIn(min = 220.dp, max = 470.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "ALBUM FOCUS  /  TAP TO RETURN",
                    color = Accent,
                    fontSize = if (compact) 7.sp else 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.7.sp,
                )
                Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
                Text(
                    state.title?.takeUnless(String::isBlank) ?: "Título no disponible",
                    color = PrimaryText,
                    fontSize = if (compact) 30.sp else 46.sp,
                    lineHeight = if (compact) 33.sp else 49.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                state.artist?.takeUnless(String::isBlank)?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = PrimaryText.copy(alpha = .7f), fontSize = if (compact) 15.sp else 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                state.album?.takeUnless(String::isBlank)?.let {
                    Spacer(Modifier.height(7.dp))
                    Text(it.uppercase(), color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(if (compact) 20.dp else 34.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .42f)))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        (state.sourceApp ?: "MEDIA SESSION").uppercase(),
                        color = SecondaryText,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlaying(
    state: MediaUiState,
    design: DisplayDesign,
    artworkMotion: ArtworkMotion,
    playbackArtworkEffect: PlaybackArtworkEffect,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onKeepAlive: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onShowArtworkFocus: () -> Unit,
) {
    var burnInStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BURN_IN_SHIFT_INTERVAL_MS)
            burnInStep = (burnInStep + 1) % BURN_IN_OFFSETS.size
        }
    }
    val burnInOffset = BURN_IN_OFFSETS[burnInStep]
    Crossfade(
        targetState = design,
        animationSpec = tween(360),
        label = "display design",
    ) { activeDesign ->
        PlaybackSkinLayout(
            state = state,
            design = activeDesign,
            artworkMotion = artworkMotion,
            playbackArtworkEffect = playbackArtworkEffect,
            burnInOffset = burnInOffset,
            controlsVisible = controlsVisible,
            onToggleControls = onToggleControls,
            onKeepAlive = onKeepAlive,
            onShowSources = onShowSources,
            onShowDiagnostics = onShowDiagnostics,
            onShowArtworkFocus = onShowArtworkFocus,
            onPlay = onPlay,
            onPause = onPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onSeek = onSeek,
        )
    }
}

@Composable
private fun PlaybackSkinLayout(
    state: MediaUiState,
    design: DisplayDesign,
    artworkMotion: ArtworkMotion,
    playbackArtworkEffect: PlaybackArtworkEffect,
    burnInOffset: Pair<Int, Int>,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onKeepAlive: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onShowArtworkFocus: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val tokens = design.tokens
    val artworkInteraction = remember { MutableInteractionSource() }
    val screenInteraction = remember { MutableInteractionSource() }
    val artworkPressed by artworkInteraction.collectIsPressedAsState()
    val screenPressed by screenInteraction.collectIsPressedAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (artworkPressed) .95f else 1f,
        animationSpec = tween(110),
        label = "artwork touch",
    )
    val view = LocalView.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = screenInteraction,
                indication = null,
                role = Role.Button,
                onClickLabel = if (controlsVisible) "Ocultar controles" else "Mostrar controles",
                onClick = {
                    view.performPremiumHaptic()
                    onToggleControls()
                },
            ),
    ) {
        val layoutMode = resolveDisplayLayoutMode(maxWidth.value, maxHeight.value)
        val compact = layoutMode == DisplayLayoutMode.COMPACT
        val horizontalPadding = when (layoutMode) {
            DisplayLayoutMode.COMPACT -> 24.dp
            DisplayLayoutMode.STANDARD -> tokens.horizontalPadding
            DisplayLayoutMode.WIDE -> tokens.horizontalPadding + 8.dp
        }
        val verticalPadding = if (compact) 18.dp else tokens.verticalPadding
        val contentGap = when (layoutMode) {
            DisplayLayoutMode.COMPACT -> 28.dp
            DisplayLayoutMode.STANDARD -> tokens.contentGap
            DisplayLayoutMode.WIDE -> tokens.contentGap + 8.dp
        }
        when (design) {
            DisplayDesign.MONOLITH_GLASS -> MonolithGlassLayout(
                state = state,
                motion = artworkMotion,
                playbackEffect = playbackArtworkEffect,
                burnInOffset = burnInOffset,
                compact = compact,
                controlsVisible = controlsVisible,
                artworkPressed = artworkPressed,
                artworkScale = artworkScale,
                controlCue = null,
                artworkInteraction = artworkInteraction,
                onToggleControls = {
                    view.performPremiumHaptic(strong = true)
                    onShowArtworkFocus()
                },
                onInteraction = onKeepAlive,
                onShowSources = onShowSources,
                onShowDiagnostics = onShowDiagnostics,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
            )

            DisplayDesign.PRECISION_DECK -> PrecisionDeckLayout(
                state = state,
                motion = artworkMotion,
                playbackEffect = playbackArtworkEffect,
                burnInOffset = burnInOffset,
                compact = compact,
                controlsVisible = controlsVisible,
                artworkPressed = artworkPressed,
                artworkScale = artworkScale,
                controlCue = null,
                artworkInteraction = artworkInteraction,
                onToggleControls = {
                    view.performPremiumHaptic(strong = true)
                    onShowArtworkFocus()
                },
                onInteraction = onKeepAlive,
                onShowSources = onShowSources,
                onShowDiagnostics = onShowDiagnostics,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
            )

            DisplayDesign.CRYSTAL_ATRIUM -> CrystalAtriumLayout(
                state = state,
                motion = artworkMotion,
                playbackEffect = playbackArtworkEffect,
                burnInOffset = burnInOffset,
                compact = compact,
                controlsVisible = controlsVisible,
                artworkPressed = artworkPressed,
                artworkScale = artworkScale,
                controlCue = null,
                artworkInteraction = artworkInteraction,
                onToggleControls = {
                    view.performPremiumHaptic(strong = true)
                    onShowArtworkFocus()
                },
                onInteraction = onKeepAlive,
                onShowSources = onShowSources,
                onShowDiagnostics = onShowDiagnostics,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
            )

            else -> Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp),
            ) {
            LegacySkinAtmosphere(design)
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            ) {
            if (tokens.artworkPlacement == ArtworkPlacement.LEADING) {
                Artwork(
                    state = state,
                    design = design,
                    motion = artworkMotion,
                    playbackEffect = playbackArtworkEffect,
                    pressed = artworkPressed,
                    controlCue = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = {
                                view.performPremiumHaptic(strong = true)
                                onShowArtworkFocus()
                            },
                        ),
                )
                Spacer(Modifier.width(contentGap))
            }
            PlaybackInformation(
                state = state,
                design = design,
                compact = compact,
                controlsVisible = controlsVisible,
                onInteraction = onKeepAlive,
                onShowSources = onShowSources,
                onShowDiagnostics = onShowDiagnostics,
                onPlay = onPlay,
                onPause = onPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                metadataModifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            if (tokens.artworkPlacement == ArtworkPlacement.TRAILING) {
                Spacer(Modifier.width(contentGap))
                Artwork(
                    state = state,
                    design = design,
                    motion = artworkMotion,
                    playbackEffect = playbackArtworkEffect,
                    pressed = artworkPressed,
                    controlCue = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = {
                                view.performPremiumHaptic(strong = true)
                                onShowArtworkFocus()
                            },
                        ),
                )
            }
            }
            }
        }
        if (screenPressed) {
            Box(Modifier.fillMaxSize().background(PrimaryText.copy(alpha = .022f)))
        }
    }
}

@Composable
private fun LegacySkinAtmosphere(design: DisplayDesign) {
    if (OledPalette) return
    val atmospherePrimary = PrimaryText
    val atmosphereSecondary = SecondaryText
    val atmosphereAccent = Accent
    when (design) {
        DisplayDesign.MODERN_REFERENCE -> Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(atmosphereAccent.copy(alpha = .045f), Color.Transparent, Background),
                        radius = 1_350f,
                    ),
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    atmospherePrimary.copy(alpha = .055f),
                    androidx.compose.ui.geometry.Offset(size.width * .48f, size.height * .08f),
                    androidx.compose.ui.geometry.Offset(size.width * .96f, size.height * .08f),
                    1.dp.toPx(),
                )
                drawLine(
                    atmosphereSecondary.copy(alpha = .04f),
                    androidx.compose.ui.geometry.Offset(size.width * .48f, size.height * .92f),
                    androidx.compose.ui.geometry.Offset(size.width * .96f, size.height * .92f),
                    1.dp.toPx(),
                )
            }
        }
        DisplayDesign.STUDIO_LEDGER -> Canvas(Modifier.fillMaxSize()) {
            val inset = 22.dp.toPx()
            drawRect(
                atmosphereSecondary.copy(alpha = .055f),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(1.dp.toPx()),
            )
            repeat(12) { index ->
                val x = inset + (size.width - inset * 2) * index / 11f
                drawLine(
                    atmosphereSecondary.copy(alpha = if (index % 3 == 0) .035f else .018f),
                    androidx.compose.ui.geometry.Offset(x, inset),
                    androidx.compose.ui.geometry.Offset(x, size.height - inset),
                    1.dp.toPx(),
                )
            }
            drawLine(
                atmosphereAccent.copy(alpha = .22f),
                androidx.compose.ui.geometry.Offset(inset, inset),
                androidx.compose.ui.geometry.Offset(size.width * .22f, inset),
                1.dp.toPx(),
            )
        }
        else -> Unit
    }
}

@Composable
private fun MonolithGlassLayout(
    state: MediaUiState,
    motion: ArtworkMotion,
    playbackEffect: PlaybackArtworkEffect,
    burnInOffset: Pair<Int, Int>,
    compact: Boolean,
    controlsVisible: Boolean,
    artworkPressed: Boolean,
    artworkScale: Float,
    controlCue: ControlCue?,
    artworkInteraction: MutableInteractionSource,
    onToggleControls: () -> Unit,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val design = DisplayDesign.MONOLITH_GLASS
    Box(
        Modifier
            .fillMaxSize()
            .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
            .padding(horizontal = if (compact) 16.dp else 22.dp, vertical = if (compact) 14.dp else 18.dp),
    ) {
        Artwork(
            state = state,
            design = design,
            motion = motion,
            playbackEffect = playbackEffect,
            pressed = artworkPressed,
            controlCue = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                .clickable(
                    interactionSource = artworkInteraction,
                    indication = null,
                    onClick = onToggleControls,
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            .3f to Color.Transparent,
                            .46f to Background.copy(alpha = .7f),
                            .6f to Background.copy(alpha = .98f),
                            1f to Background,
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(if (compact) .55f else .52f)
                .padding(
                    top = if (compact) 72.dp else 84.dp,
                    end = if (compact) 16.dp else 34.dp,
                    bottom = 72.dp,
                )
                .graphicsLayer { alpha = 1f },
            verticalArrangement = Arrangement.Center,
        ) {
            MonolithEditorialMetadata(state, compact)
        }
        Progress(
            state = state,
            design = design,
            controlsVisible = controlsVisible,
            onInteraction = onInteraction,
            onSeek = onSeek,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(if (compact) .55f else .52f)
                .padding(end = if (compact) 16.dp else 34.dp),
        )
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = if (compact) 16.dp else 34.dp),
            enter = fadeIn(tween(180)) + scaleIn(tween(240), initialScale = .96f),
            exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = .98f),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryText.copy(alpha = .075f), SurfaceRaised.copy(alpha = .54f)),
                        ),
                    )
                    .border(1.dp, PrimaryText.copy(alpha = .12f), RoundedCornerShape(24.dp))
                    .padding(start = 7.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceHeader(
                    state = state,
                    design = design,
                    modifier = Modifier.widthIn(max = if (compact) 138.dp else 164.dp),
                    onShowSources = onShowSources,
                    onShowDiagnostics = onShowDiagnostics,
                )
                Spacer(Modifier.width(12.dp))
                PlayerControls(
                    state = state,
                    design = design,
                    visible = true,
                    compact = compact,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
        InlineControlStateCue(
            state = controlCue,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun MonolithEditorialMetadata(state: MediaUiState, compact: Boolean) {
    val track = TrackCopy(
        title = state.title?.takeUnless(String::isBlank) ?: "Título no disponible",
        artist = state.artist?.takeUnless(String::isBlank),
        album = state.album?.takeUnless(String::isBlank),
    )
    TrackCopyTransition(track, DisplayDesign.MONOLITH_GLASS, "monolith editorial metadata") { copy ->
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(1.dp).background(Accent.copy(alpha = .8f)))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (state.isPlaying) "CURRENT SELECTION" else "PLAYBACK PAUSED",
                    color = Accent.copy(alpha = .78f),
                    fontSize = if (compact) 7.sp else 8.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.8.sp,
                )
            }
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            Text(
                copy.title,
                color = PrimaryText,
                fontSize = if (compact) 33.sp else 52.sp,
                lineHeight = if (compact) 36.sp else 55.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-.5).sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            copy.artist?.let {
                Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
                Text(
                    it,
                    color = PrimaryText.copy(alpha = .7f),
                    fontSize = if (compact) 16.sp else 21.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            copy.album?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it.uppercase(),
                    color = SecondaryText.copy(alpha = .66f),
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CrystalAtriumLayout(
    state: MediaUiState,
    motion: ArtworkMotion,
    playbackEffect: PlaybackArtworkEffect,
    burnInOffset: Pair<Int, Int>,
    compact: Boolean,
    controlsVisible: Boolean,
    artworkPressed: Boolean,
    artworkScale: Float,
    controlCue: ControlCue?,
    artworkInteraction: MutableInteractionSource,
    onToggleControls: () -> Unit,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val design = DisplayDesign.CRYSTAL_ATRIUM
    val oled = OledPalette
    val sheetShape = RoundedCornerShape(if (compact) 16.dp else 22.dp)
    val crystalPrimary = PrimaryText
    val crystalAccent = Accent
    Box(
        Modifier
            .fillMaxSize()
            .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
            .background(
                if (oled) {
                    Brush.linearGradient(listOf(Background, Background))
                } else {
                    Brush.radialGradient(
                        colors = listOf(PrimaryText.copy(alpha = .1f), Accent.copy(alpha = .09f), Surface.copy(alpha = .42f), Background),
                        radius = if (compact) 760f else 1_320f,
                    )
                },
            )
            .padding(horizontal = if (compact) 18.dp else 30.dp, vertical = if (compact) 14.dp else 24.dp),
    ) {
        if (!oled) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(5) { index ->
                    val offset = size.width * (.03f + index * .18f)
                    drawLine(
                        color = crystalPrimary.copy(alpha = .018f + index * .004f),
                        start = androidx.compose.ui.geometry.Offset(offset, 0f),
                        end = androidx.compose.ui.geometry.Offset(offset + size.height * .42f, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawLine(
                    color = crystalAccent.copy(alpha = .08f),
                    start = androidx.compose.ui.geometry.Offset(size.width * .64f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .58f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .clip(sheetShape)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to PrimaryText.copy(alpha = .14f),
                            .18f to PrimaryText.copy(alpha = .045f),
                            .52f to Surface.copy(alpha = .34f),
                            .82f to Accent.copy(alpha = .035f),
                            1f to SurfaceRaised.copy(alpha = .2f),
                        ),
                    ),
                )
                .border(1.dp, PrimaryText.copy(alpha = .3f), sheetShape),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(6.dp)
                    .border(1.dp, PrimaryText.copy(alpha = .08f), RoundedCornerShape(if (compact) 12.dp else 17.dp)),
            )
            Box(
                Modifier
                    .fillMaxWidth(.56f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, PrimaryText.copy(alpha = .72f), Color.Transparent),
                        ),
                    ),
            )
            Row(
                Modifier.fillMaxSize().padding(if (compact) 14.dp else 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    state = state,
                    design = design,
                    motion = motion,
                    playbackEffect = playbackEffect,
                    pressed = artworkPressed,
                    controlCue = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = onToggleControls,
                        ),
                )
                Spacer(Modifier.width(if (compact) 18.dp else 30.dp))
                Box(
                    Modifier
                        .fillMaxHeight(.82f)
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, PrimaryText.copy(alpha = .28f), Accent.copy(alpha = .34f), Color.Transparent),
                            ),
                        ),
                )
                Spacer(Modifier.width(if (compact) 18.dp else 30.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = 1f },
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(if (compact) 68.dp else 76.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedVisibility(
                            visible = !controlsVisible,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(120)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .42f)))
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    if (state.isPlaying) "OPTICAL PLAYBACK" else "SESSION PAUSED",
                                    color = PrimaryText.copy(alpha = .72f),
                                    fontSize = if (compact) 7.sp else 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.8.sp,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        AnimatedVisibility(
                            visible = !controlsVisible,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(120)),
                        ) {
                            Text(
                                "ATRIUM / 01",
                                color = SecondaryText.copy(alpha = .64f),
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.4.sp,
                            )
                        }
                    }
                    CrystalMetadata(state, compact, Modifier.weight(1f))
                    Progress(
                        state = state,
                        design = design,
                        controlsVisible = controlsVisible,
                        onInteraction = onInteraction,
                        onSeek = onSeek,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = if (compact) 24.dp else 36.dp, end = if (compact) 26.dp else 42.dp),
            enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { -it / 4 },
            exit = fadeOut(tween(170)) + slideOutVertically(tween(210)) { -it / 5 },
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(PrimaryText.copy(alpha = .16f), Surface.copy(alpha = .82f))))
                    .border(1.dp, PrimaryText.copy(alpha = .3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceHeader(
                    state = state,
                    design = design,
                    modifier = Modifier.widthIn(max = if (compact) 132.dp else 160.dp),
                    onShowSources = onShowSources,
                    onShowDiagnostics = onShowDiagnostics,
                )
                Spacer(Modifier.width(8.dp))
                PlayerControls(
                    state = state,
                    design = design,
                    visible = true,
                    compact = compact,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
        InlineControlStateCue(
            state = controlCue,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (compact) 22.dp else 32.dp),
        )
    }
}

@Composable
private fun CrystalMetadata(state: MediaUiState, compact: Boolean, modifier: Modifier = Modifier) {
    val copy = TrackCopy(
        title = state.title?.takeUnless(String::isBlank) ?: "Título no disponible",
        artist = state.artist?.takeUnless(String::isBlank),
        album = state.album?.takeUnless(String::isBlank),
    )
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        TrackCopyTransition(copy, DisplayDesign.CRYSTAL_ATRIUM, "crystal metadata") { track ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    track.title,
                    color = PrimaryText,
                    fontSize = if (compact) 31.sp else 46.sp,
                    lineHeight = if (compact) 34.sp else 49.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-.35).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Text(
                        it,
                        color = PrimaryText.copy(alpha = .72f),
                        fontSize = if (compact) 15.sp else 19.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                track.album?.let {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        it.uppercase(),
                        color = SecondaryText.copy(alpha = .72f),
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecisionDeckLayout(
    state: MediaUiState,
    motion: ArtworkMotion,
    playbackEffect: PlaybackArtworkEffect,
    burnInOffset: Pair<Int, Int>,
    compact: Boolean,
    controlsVisible: Boolean,
    artworkPressed: Boolean,
    artworkScale: Float,
    controlCue: ControlCue?,
    artworkInteraction: MutableInteractionSource,
    onToggleControls: () -> Unit,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val design = DisplayDesign.PRECISION_DECK
    val oled = OledPalette
    val etched = SecondaryText
    Box(
        Modifier
            .fillMaxSize()
            .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
            .background(
                if (oled) Brush.linearGradient(listOf(Background, Background))
                else Brush.linearGradient(listOf(Background, Surface.copy(alpha = .48f), Background)),
            ),
    ) {
        if (!oled) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = if (compact) 18.dp.toPx() else 28.dp.toPx()
                drawRect(
                    color = etched.copy(alpha = .06f),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(1.dp.toPx()),
                )
                repeat(9) { index ->
                    val x = inset + (size.width - inset * 2) * index / 8f
                    drawLine(
                        etched.copy(alpha = if (index == 4) .045f else .022f),
                        androidx.compose.ui.geometry.Offset(x, inset),
                        androidx.compose.ui.geometry.Offset(x, size.height - inset),
                        1.dp.toPx(),
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxSize().padding(
                start = if (compact) 24.dp else 40.dp,
                end = if (compact) 24.dp else 40.dp,
                top = if (compact) 18.dp else 26.dp,
                bottom = if (compact) 16.dp else 24.dp,
            ),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "HIFI / PRECISION TRANSPORT",
                    color = SecondaryText.copy(alpha = .74f),
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .36f)))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isPlaying) "RUN" else "STANDBY",
                    color = if (state.isPlaying) Accent else SecondaryText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.4.sp,
                )
            }
            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = 1f }
                        .padding(end = if (compact) 22.dp else 42.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val precisionTrack = TrackCopy(
                        title = state.title?.takeUnless(String::isBlank) ?: "Título no disponible",
                        artist = state.artist?.takeUnless(String::isBlank),
                        album = state.album?.takeUnless(String::isBlank),
                    )
                    TrackCopyTransition(precisionTrack, DisplayDesign.PRECISION_DECK, "precision metadata") {
                        PrecisionMetadata(it, compact)
                    }
                    Spacer(Modifier.height(if (compact) 16.dp else 26.dp))
                    PrecisionTimebase(state)
                }
                Artwork(
                    state = state,
                    design = design,
                    motion = motion,
                    playbackEffect = playbackEffect,
                    pressed = artworkPressed,
                    controlCue = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = onToggleControls,
                        ),
                )
            }
            AnimatedVisibility(
                visible = !controlsVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                Column {
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Progress(
                        state = state,
                        design = design,
                        controlsVisible = false,
                        onInteraction = onInteraction,
                        onSeek = onSeek,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(180)) + slideInVertically(tween(260)) { it / 3 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 3 },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(Brush.verticalGradient(listOf(SurfaceRaised.copy(alpha = .82f), Surface.copy(alpha = .96f))))
                    .border(1.dp, SecondaryText.copy(alpha = .11f))
                    .padding(horizontal = if (compact) 24.dp else 40.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceHeader(
                    state = state,
                    design = design,
                    modifier = Modifier.widthIn(max = if (compact) 138.dp else 164.dp),
                    onShowSources = onShowSources,
                    onShowDiagnostics = onShowDiagnostics,
                )
                Spacer(Modifier.width(if (compact) 10.dp else 18.dp))
                PlayerControls(
                    state = state,
                    design = design,
                    visible = true,
                    compact = compact,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
                Spacer(Modifier.width(if (compact) 16.dp else 28.dp))
                Progress(
                    state = state,
                    design = design,
                    controlsVisible = true,
                    onInteraction = onInteraction,
                    onSeek = onSeek,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        InlineControlStateCue(
            state = controlCue,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = if (compact) 18.dp else 26.dp),
        )
    }
}

@Composable
private fun PrecisionTimebase(state: MediaUiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("ELAPSED", color = SecondaryText.copy(alpha = .62f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.4.sp)
            Text(formatTime(state.positionMs), color = PrimaryText.copy(alpha = .78f), fontSize = 17.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }
        Box(Modifier.width(1.dp).height(28.dp).background(SecondaryText.copy(alpha = .12f)))
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text("DURATION", color = SecondaryText.copy(alpha = .62f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.4.sp)
            Text(state.durationMs?.let(::formatTime) ?: "--:--", color = PrimaryText.copy(alpha = .78f), fontSize = 17.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun InlineControlStateCue(state: ControlCue?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state?.visible == true,
        modifier = modifier,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(220)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(if (state?.showsControls == true) Accent else SecondaryText))
            Spacer(Modifier.width(7.dp))
            Text(
                if (state?.showsControls == true) "HUD REVEALED" else "AMBIENT MODE",
                color = SecondaryText.copy(alpha = .72f),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.4.sp,
            )
        }
    }
}

@Composable
private fun PlaybackInformation(
    state: MediaUiState,
    design: DisplayDesign,
    compact: Boolean,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    metadataModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val studio = design == DisplayDesign.STUDIO_LEDGER
    val studioBottomControls = studio && !compact
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(if (compact) 54.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = .96f),
                exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = .97f),
            ) {
                SourceHeader(
                    state = state,
                    design = design,
                    modifier = Modifier.widthIn(max = if (compact) 148.dp else 176.dp),
                    onShowSources = onShowSources,
                    onShowDiagnostics = onShowDiagnostics,
                )
            }
            if (!studioBottomControls) {
                Spacer(Modifier.width(if (compact) 8.dp else 16.dp))
                PlayerControls(
                    state = state,
                    design = design,
                    visible = controlsVisible,
                    compact = compact,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
        TrackMetadata(
            state = state,
            design = design,
            compact = compact,
            modifier = metadataModifier,
        )
        if (studioBottomControls) {
            Box(
                Modifier.fillMaxWidth().height(if (compact) 48.dp else 58.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                PlayerControls(
                    state = state,
                    design = design,
                    visible = controlsVisible,
                    compact = compact,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
        Progress(
            state = state,
            design = design,
            controlsVisible = controlsVisible,
            onInteraction = onInteraction,
            onSeek = onSeek,
        )
    }
}

private data class TrackCopy(
    val title: String,
    val artist: String?,
    val album: String?,
)

@Composable
private fun TrackMetadata(
    state: MediaUiState,
    design: DisplayDesign,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val copy = TrackCopy(
        title = state.title?.takeUnless(String::isBlank) ?: "Título no disponible",
        artist = state.artist?.takeUnless(String::isBlank),
        album = state.album?.takeUnless(String::isBlank),
    )
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        TrackCopyTransition(copy, design, "track metadata") { track ->
            when (design) {
                DisplayDesign.MODERN_REFERENCE -> ModernMetadata(track, compact)
                DisplayDesign.STUDIO_LEDGER -> StudioMetadata(track, compact)
                DisplayDesign.MONOLITH_GLASS -> MonolithMetadata(track, compact)
                DisplayDesign.PRECISION_DECK -> PrecisionMetadata(track, compact)
                DisplayDesign.CRYSTAL_ATRIUM -> ModernMetadata(track, compact)
            }
        }
    }
}

@Composable
private fun TrackCopyTransition(
    track: TrackCopy,
    design: DisplayDesign,
    label: String,
    content: @Composable (TrackCopy) -> Unit,
) {
    AnimatedContent(
        targetState = track,
        transitionSpec = {
            when (design) {
                DisplayDesign.MODERN_REFERENCE ->
                    (fadeIn(tween(320, delayMillis = 50)) + slideInVertically(tween(420)) { it / 10 })
                        .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(260)) { -it / 14 })
                DisplayDesign.STUDIO_LEDGER ->
                    (fadeIn(tween(250)) + slideInHorizontally(tween(380)) { it / 12 })
                        .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { -it / 16 })
                DisplayDesign.MONOLITH_GLASS ->
                    (fadeIn(tween(420, delayMillis = 70)) + slideInHorizontally(tween(560)) { it / 20 })
                        .togetherWith(fadeOut(tween(260)))
                DisplayDesign.PRECISION_DECK ->
                    (fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 12 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(220)) { -it / 18 })
                DisplayDesign.CRYSTAL_ATRIUM ->
                    (fadeIn(tween(440, delayMillis = 60)) + scaleIn(tween(520), initialScale = .975f))
                        .togetherWith(fadeOut(tween(260)))
            }
        },
        label = label,
    ) { content(it) }
}

@Composable
private fun ModernMetadata(track: TrackCopy, compact: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(Accent.copy(alpha = .84f)))
            Spacer(Modifier.width(8.dp))
            Text(
                "REFERENCE / ACTIVE SESSION",
                color = SecondaryText.copy(alpha = .68f),
                fontSize = if (compact) 7.sp else 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.7.sp,
            )
        }
        Spacer(Modifier.height(if (compact) 11.dp else 17.dp))
        Text(
            text = track.title,
            color = PrimaryText,
            fontSize = if (compact) 31.sp else 44.sp,
            lineHeight = if (compact) 34.sp else 47.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-.35).sp,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        track.artist?.let {
            Spacer(Modifier.height(if (compact) 7.dp else 12.dp))
            Text(
                text = it,
                color = SecondaryText,
                fontSize = if (compact) 17.sp else 21.sp,
                lineHeight = if (compact) 20.sp else 25.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        track.album?.let {
            Spacer(Modifier.height(5.dp))
            Text(
                text = it.uppercase(),
                color = SecondaryText.copy(alpha = .62f),
                fontSize = if (compact) 9.sp else 11.sp,
                lineHeight = if (compact) 11.sp else 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StudioMetadata(track: TrackCopy, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(2.dp)
                .height(if (compact) 112.dp else 154.dp)
                .background(Accent.copy(alpha = .52f)),
        )
        Spacer(Modifier.width(if (compact) 14.dp else 20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = "PROGRAM / NOW PLAYING",
                color = Accent.copy(alpha = .72f),
                fontSize = if (compact) 8.sp else 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
            Text(
                text = track.title.uppercase(),
                color = PrimaryText,
                fontSize = if (compact) 25.sp else 35.sp,
                lineHeight = if (compact) 29.sp else 39.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = .35.sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            track.artist?.let {
                Spacer(Modifier.height(if (compact) 7.dp else 11.dp))
                Text(
                    text = it,
                    color = SecondaryText,
                    fontSize = if (compact) 15.sp else 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            track.album?.let {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "ALBUM / ${it.uppercase()}",
                    color = SecondaryText.copy(alpha = .58f),
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MonolithMetadata(track: TrackCopy, compact: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 12.dp else 18.dp))
            .background(
                Brush.linearGradient(
                    listOf(SurfaceRaised.copy(alpha = .34f), Surface.copy(alpha = .12f)),
                ),
            )
            .border(1.dp, PrimaryText.copy(alpha = .07f), RoundedCornerShape(if (compact) 12.dp else 18.dp))
            .padding(horizontal = if (compact) 18.dp else 26.dp, vertical = if (compact) 14.dp else 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                "LISTENING ROOM  /  CURRENT SELECTION",
                color = Accent.copy(alpha = .84f),
                fontSize = if (compact) 7.sp else 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            Text(
                track.title,
                color = PrimaryText,
                fontSize = if (compact) 28.sp else 43.sp,
                lineHeight = if (compact) 31.sp else 46.sp,
                fontWeight = FontWeight.Light,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            track.artist?.let {
                Spacer(Modifier.height(if (compact) 7.dp else 12.dp))
                Text(it, color = PrimaryText.copy(alpha = .7f), fontSize = if (compact) 15.sp else 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            track.album?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it.uppercase(),
                    color = SecondaryText.copy(alpha = .68f),
                    fontSize = if (compact) 8.sp else 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrecisionMetadata(track: TrackCopy, compact: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        PrecisionMetadataLine("01 / TITLE", track.title.uppercase(), compact, primary = true)
        track.artist?.let {
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            PrecisionMetadataLine("02 / ARTIST", it, compact)
        }
        track.album?.let {
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            PrecisionMetadataLine("03 / RELEASE", it.uppercase(), compact)
        }
    }
}

@Composable
private fun PrecisionMetadataLine(label: String, value: String, compact: Boolean, primary: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            color = if (primary) Accent.copy(alpha = .82f) else SecondaryText.copy(alpha = .62f),
            fontSize = if (compact) 7.sp else 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.1.sp,
            modifier = Modifier.width(if (compact) 72.dp else 88.dp).padding(top = 4.dp),
        )
        Text(
            value,
            color = if (primary) PrimaryText else SecondaryText,
            fontSize = if (primary) (if (compact) 24.sp else 34.sp) else (if (compact) 13.sp else 16.sp),
            lineHeight = if (primary) (if (compact) 27.sp else 38.sp) else (if (compact) 16.sp else 20.sp),
            fontWeight = if (primary) FontWeight.Light else FontWeight.Normal,
            fontFamily = if (primary) FontFamily.Default else FontFamily.Monospace,
            maxLines = if (primary && !compact) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceHeader(
    state: MediaUiState,
    design: DisplayDesign,
    modifier: Modifier = Modifier,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
) {
    val studio = design == DisplayDesign.STUDIO_LEDGER
    val precision = design == DisplayDesign.PRECISION_DECK
    val monolith = design == DisplayDesign.MONOLITH_GLASS
    val crystal = design == DisplayDesign.CRYSTAL_ATRIUM
    val shape = RoundedCornerShape(design.tokens.sourceCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val container by animateColorAsState(
        targetValue = when {
            pressed -> Accent.copy(alpha = .09f)
            studio || precision -> SurfaceRaised.copy(alpha = .08f)
            monolith -> PrimaryText.copy(alpha = .045f)
            crystal -> PrimaryText.copy(alpha = .065f)
            else -> SurfaceRaised.copy(alpha = .14f)
        },
        animationSpec = tween(120),
        label = "source press",
    )
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, if (pressed) Accent.copy(alpha = .46f) else SecondaryText.copy(alpha = .08f), shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    view.performPremiumHaptic()
                    onShowSources()
                },
                onLongClick = {
                    view.performPremiumHaptic(strong = true)
                    onShowDiagnostics()
                },
                onLongClickLabel = "Abrir diagnóstico",
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .then(if (studio) Modifier.width(2.dp).height(12.dp) else Modifier.size(5.dp))
                .clip(if (studio) RoundedCornerShape(1.dp) else CircleShape)
                .background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .4f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            (state.sourceApp ?: "Sesión multimedia").uppercase(),
            color = if (state.isPlaying && !precision) Accent else if (state.isPlaying) PrimaryText.copy(alpha = .78f) else SecondaryText,
            fontSize = if (studio || precision || crystal) 9.sp else 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = if (studio || precision || crystal) 1.4.sp else 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.ExpandMore, "Abrir opciones de fuente y display", tint = SecondaryText.copy(alpha = .78f), modifier = Modifier.size(14.dp))
    }
}

private enum class DisplayPanelSection(val label: String) {
    SOURCE("FUENTE"),
    APPEARANCE("APARIENCIA"),
    MOTION("MOVIMIENTO"),
}

@Composable
private fun SourcePickerOverlay(
    state: MediaUiState,
    appearance: DisplayAppearance,
    onSelectSource: (String?) -> Unit,
    onSelectDesign: (DisplayDesign) -> Unit,
    onSelectPalette: (ColorPalette) -> Unit,
    onSelectPaletteMode: (PaletteMode) -> Unit,
    onSelectArtworkMotion: (ArtworkMotion) -> Unit,
    onSelectPlaybackArtworkEffect: (PlaybackArtworkEffect) -> Unit,
    onShowDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
) {
    var section by remember { mutableStateOf(DisplayPanelSection.SOURCE) }
    var previewMotion by remember(appearance.artworkMotion) { mutableStateOf(appearance.artworkMotion) }
    var motionPreviewId by remember { mutableLongStateOf(0L) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 42.dp, vertical = 18.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DISPLAY CONTROL", color = Accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    Text(
                        "${appearance.design.displayName.uppercase()}  ·  ${appearance.palette.displayName.uppercase()}  ·  ${appearance.paletteMode.displayName.uppercase()}",
                        color = SecondaryText,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, "Cerrar", tint = SecondaryText)
                }
            }
            Spacer(Modifier.height(8.dp))
            DisplayPanelTabs(section, onSelect = { section = it })
            Spacer(Modifier.height(12.dp))
            Crossfade(
                targetState = section,
                animationSpec = tween(180),
                label = "display panel section",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { activeSection ->
                when (activeSection) {
                    DisplayPanelSection.SOURCE -> SourceSelectionPanel(state, onSelectSource, onShowDiagnostics)
                    DisplayPanelSection.APPEARANCE -> AppearanceSelectionPanel(
                        appearance = appearance,
                        onSelectDesign = onSelectDesign,
                        onSelectPalette = onSelectPalette,
                        onSelectPaletteMode = onSelectPaletteMode,
                    )
                    DisplayPanelSection.MOTION -> MotionSelectionPanel(
                        appearance = appearance,
                        previewMotion = previewMotion,
                        motionPreviewId = motionPreviewId,
                        onSelectArtworkMotion = { option ->
                            previewMotion = option
                            motionPreviewId += 1L
                            onSelectArtworkMotion(option)
                        },
                        onSelectPlaybackArtworkEffect = onSelectPlaybackArtworkEffect,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSelectionPanel(
    appearance: DisplayAppearance,
    onSelectDesign: (DisplayDesign) -> Unit,
    onSelectPalette: (ColorPalette) -> Unit,
    onSelectPaletteMode: (PaletteMode) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 780.dp
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(Modifier.weight(1.08f)) {
                    PanelIntro("DISPLAY DESIGN", "LAYOUT · TYPOGRAPHY · CONTROL LANGUAGE")
                    Spacer(Modifier.height(10.dp))
                    DesignPreviewGrid(selected = appearance.design, onSelect = onSelectDesign)
                }
                Box(Modifier.fillMaxHeight().width(1.dp).background(SecondaryText.copy(alpha = .12f)))
                Column(Modifier.weight(.92f)) {
                    PanelIntro("COLOR SYSTEM", "CHOOSE LIGHT LEVEL, THEN COLOR FAMILY")
                    Spacer(Modifier.height(10.dp))
                    PaletteModeSelector(appearance.paletteMode, onSelectPaletteMode)
                    Spacer(Modifier.height(12.dp))
                    PalettePickerGrid(appearance.palette, appearance.paletteMode, onSelectPalette)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PanelIntro("DISPLAY DESIGN", "LAYOUT · TYPOGRAPHY · CONTROL LANGUAGE")
                Spacer(Modifier.height(10.dp))
                DesignPreviewGrid(selected = appearance.design, onSelect = onSelectDesign)
                Spacer(Modifier.height(18.dp))
                PanelIntro("COLOR SYSTEM", "CHOOSE LIGHT LEVEL, THEN COLOR FAMILY")
                Spacer(Modifier.height(10.dp))
                PaletteModeSelector(appearance.paletteMode, onSelectPaletteMode)
                Spacer(Modifier.height(10.dp))
                PalettePickerGrid(appearance.palette, appearance.paletteMode, onSelectPalette)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun PaletteModeSelector(selected: PaletteMode, onSelect: (PaletteMode) -> Unit) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceRaised.copy(alpha = .28f))
            .border(1.dp, SecondaryText.copy(alpha = .14f), RoundedCornerShape(5.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PaletteMode.entries.forEach { mode ->
            val active = mode == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(90), label = "${mode.displayName} mode touch")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) Accent.copy(alpha = .16f) else Color.Transparent)
                    .border(1.dp, if (active) Accent.copy(alpha = .55f) else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = {
                            view.performPremiumHaptic(strong = true)
                            onSelect(mode)
                        },
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(mode.displayName.uppercase(), color = if (active) Accent else PrimaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Text(mode.descriptor, color = SecondaryText, fontSize = 6.sp, fontFamily = FontFamily.Monospace, letterSpacing = .6.sp)
            }
        }
    }
}

@Composable
private fun DisplayPanelTabs(
    selected: DisplayPanelSection,
    onSelect: (DisplayPanelSection) -> Unit,
) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Surface.copy(alpha = .72f))
            .border(1.dp, SecondaryText.copy(alpha = .12f), RoundedCornerShape(5.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DisplayPanelSection.entries.forEach { section ->
            val active = section == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(90), label = "${section.label} tab touch")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) Accent.copy(alpha = .13f) else Color.Transparent)
                    .border(1.dp, if (active) Accent.copy(alpha = .42f) else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            view.performPremiumHaptic(strong = true)
                            onSelect(section)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    section.label,
                    color = if (active) Accent else SecondaryText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun PanelIntro(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(1.dp).background(Accent.copy(alpha = .72f)))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.3.sp)
            Text(subtitle, color = SecondaryText, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = .8.sp)
        }
    }
}

@Composable
private fun SourceSelectionPanel(
    state: MediaUiState,
    onSelectSource: (String?) -> Unit,
    onShowDiagnostics: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PanelIntro("ACTIVE SOURCE", "AUTO-FOLLOW OR PIN ONE MEDIA SESSION")
        Spacer(Modifier.height(10.dp))
                PickerRow(
                    title = "AUTO",
                    subtitle = "Sigue la sesión con mayor actividad",
                    selected = state.pinnedSourcePackage == null,
                    onClick = { onSelectSource(null) },
                )
                if (state.availableSources.isEmpty()) {
                    Text(
                        "NO ACTIVE MEDIA SESSIONS",
                        color = SecondaryText.copy(alpha = .7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                val pinnedUnavailable = state.pinnedSourcePackage?.takeIf { pinned ->
                    state.availableSources.none { it.packageName == pinned }
                }
                pinnedUnavailable?.let { packageName ->
                    PickerRow(
                        title = packageName,
                        subtitle = "PINNED · NOT ACTIVE · TAP TO CLEAR",
                        selected = true,
                        onClick = { onSelectSource(null) },
                    )
                }
                state.availableSources.forEach { source ->
                    val status = buildList {
                        if (source.isPlaying) add("PLAYING")
                        if (source.isSelected) add("ACTIVE")
                        if (source.isPinned) add("PINNED")
                    }.joinToString(" · ").ifBlank { source.packageName }
                    PickerRow(
                        title = source.label,
                        subtitle = status,
                        selected = source.isPinned,
                        onClick = { onSelectSource(source.packageName) },
                    )
                }
        Spacer(Modifier.height(14.dp))
        PanelIntro("TOOLS", "SESSION CAPABILITIES AND RAW MEDIA DATA")
        Spacer(Modifier.height(8.dp))
                PickerRow(
                    title = "Session diagnostics",
                    subtitle = if (state.hasActiveSession) "VIEW SESSION CAPABILITIES" else "REQUIRES AN ACTIVE SESSION",
                    selected = false,
                    enabled = state.hasActiveSession,
                    onClick = onShowDiagnostics,
                )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MotionSelectionPanel(
    appearance: DisplayAppearance,
    previewMotion: ArtworkMotion,
    motionPreviewId: Long,
    onSelectArtworkMotion: (ArtworkMotion) -> Unit,
    onSelectPlaybackArtworkEffect: (PlaybackArtworkEffect) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MotionOptionColumn(
                    modifier = Modifier.weight(1f),
                    title = "TRACK TRANSITION",
                    subtitle = "CHANGEOVER BETWEEN COVERS",
                    preview = { ArtworkMotionPreview(previewMotion, motionPreviewId) },
                ) {
                    MotionChoiceGrid(
                        options = ArtworkMotion.entries,
                        selected = appearance.artworkMotion,
                        title = ArtworkMotion::displayName,
                        subtitle = ArtworkMotion::descriptor,
                        onSelect = onSelectArtworkMotion,
                    )
                }
                MotionOptionColumn(
                    modifier = Modifier.weight(1f),
                    title = "AMBIENT PRESENCE",
                    subtitle = "VISUAL MOTION · NOT AUDIO REACTIVE",
                    preview = { PlaybackEffectPreview(appearance.playbackArtworkEffect) },
                ) {
                    MotionChoiceGrid(
                        options = PlaybackArtworkEffect.entries,
                        selected = appearance.playbackArtworkEffect,
                        title = PlaybackArtworkEffect::displayName,
                        subtitle = PlaybackArtworkEffect::descriptor,
                        onSelect = onSelectPlaybackArtworkEffect,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MotionOptionColumn(
                    modifier = Modifier.fillMaxWidth(),
                    title = "TRACK TRANSITION",
                    subtitle = "CHANGEOVER BETWEEN COVERS",
                    preview = { ArtworkMotionPreview(previewMotion, motionPreviewId) },
                ) {
                    MotionChoiceGrid(
                        options = ArtworkMotion.entries,
                        selected = appearance.artworkMotion,
                        title = ArtworkMotion::displayName,
                        subtitle = ArtworkMotion::descriptor,
                        onSelect = onSelectArtworkMotion,
                    )
                }
                Spacer(Modifier.height(16.dp))
                MotionOptionColumn(
                    modifier = Modifier.fillMaxWidth(),
                    title = "AMBIENT PRESENCE",
                    subtitle = "VISUAL MOTION · NOT AUDIO REACTIVE",
                    preview = { PlaybackEffectPreview(appearance.playbackArtworkEffect) },
                ) {
                    MotionChoiceGrid(
                        options = PlaybackArtworkEffect.entries,
                        selected = appearance.playbackArtworkEffect,
                        title = PlaybackArtworkEffect::displayName,
                        subtitle = PlaybackArtworkEffect::descriptor,
                        onSelect = onSelectPlaybackArtworkEffect,
                    )
                }
            }
        }
    }
}

@Composable
private fun MotionOptionColumn(
    modifier: Modifier,
    title: String,
    subtitle: String,
    preview: @Composable () -> Unit,
    options: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        PanelIntro(title, subtitle)
        Spacer(Modifier.height(8.dp))
        preview()
        Spacer(Modifier.height(6.dp))
        options()
    }
}

@Composable
private fun <T> MotionChoiceGrid(
    options: List<T>,
    selected: T,
    title: (T) -> String,
    subtitle: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowOptions.forEach { option ->
                    val active = option == selected
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(90), label = "motion option touch")
                    val view = LocalView.current
                    Column(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    pressed -> SurfaceRaised.copy(alpha = .68f)
                                    active -> Accent.copy(alpha = .11f)
                                    else -> SurfaceRaised.copy(alpha = .2f)
                                },
                            )
                            .border(1.dp, if (active) Accent.copy(alpha = .5f) else SecondaryText.copy(alpha = .1f), RoundedCornerShape(4.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.RadioButton,
                                onClick = {
                                    view.performPremiumHaptic(strong = true)
                                    onSelect(option)
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(title(option), color = PrimaryText, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (active) Accent else SecondaryText.copy(alpha = .24f)))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subtitle(option),
                            color = SecondaryText,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = .55.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlaybackEffectPreview(effect: PlaybackArtworkEffect) {
    val visual = rememberPlaybackArtworkVisual(effect, isPlaying = true)
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceRaised.copy(alpha = .18f))
            .border(1.dp, SecondaryText.copy(alpha = .1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(effect.displayName.uppercase(), color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text("VISUAL ONLY · ACTIVE DURING PLAYBACK", color = SecondaryText.copy(alpha = .72f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = .8.sp)
        }
        Box(Modifier.width(104.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = visual.scale
                        scaleY = visual.scale
                        translationX = visual.translationX
                        translationY = visual.translationY
                    }
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.linearGradient(listOf(Accent.copy(alpha = .52f), SurfaceRaised)))
                    .border(1.dp, Accent.copy(alpha = visual.haloAlpha), RoundedCornerShape(5.dp)),
            )
        }
    }
}

@Composable
private fun ArtworkMotionPreview(motion: ArtworkMotion, replayId: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceRaised.copy(alpha = .18f))
            .border(1.dp, SecondaryText.copy(alpha = .1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(motion.displayName.uppercase(), color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text("TAP AN EFFECT TO REPLAY", color = SecondaryText.copy(alpha = .72f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = .8.sp)
        }
        Box(
            Modifier
                .width(104.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Surface),
        ) {
            MotionPreviewWindow(motion, replayId)
        }
    }
}

@Composable
private fun MotionPreviewWindow(motion: ArtworkMotion, replayId: Long) {
    when (motion) {
        ArtworkMotion.FOCUS -> AnimatedContent(
            targetState = replayId,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 480, delayMillis = 30)) +
                    scaleIn(tween(durationMillis = 680), initialScale = .9f))
                    .togetherWith(fadeOut(tween(durationMillis = 240)))
            },
            label = "focus preview",
        ) { MotionPreviewFrame(it) }

        ArtworkMotion.DISSOLVE -> Crossfade(
            targetState = replayId,
            animationSpec = tween(700),
            label = "dissolve preview",
        ) { MotionPreviewFrame(it) }

        ArtworkMotion.DECK -> AnimatedContent(
            targetState = replayId,
            transitionSpec = {
                (fadeIn(tween(360)) + slideInHorizontally(tween(520)) { it / 4 })
                    .togetherWith(fadeOut(tween(260)) + slideOutHorizontally(tween(440)) { -it / 6 })
            },
            label = "deck preview",
        ) { MotionPreviewFrame(it) }

        ArtworkMotion.DIRECT -> MotionPreviewFrame(replayId)
    }
}

@Composable
private fun MotionPreviewFrame(replayId: Long) {
    val alternate = replayId % 2L != 0L
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (alternate) listOf(Accent.copy(alpha = .58f), SurfaceRaised)
                    else listOf(SurfaceRaised, Accent.copy(alpha = .3f)),
                ),
            )
            .padding(8.dp),
    ) {
        Box(Modifier.align(Alignment.TopStart).width(if (alternate) 42.dp else 54.dp).height(3.dp).background(PrimaryText.copy(alpha = .72f)))
        Box(Modifier.align(Alignment.CenterStart).width(if (alternate) 62.dp else 44.dp).height(2.dp).background(PrimaryText.copy(alpha = .34f)))
        Text(
            if (alternate) "B" else "A",
            color = Background.copy(alpha = .66f),
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun DesignPreviewGrid(
    selected: DisplayDesign,
    onSelect: (DisplayDesign) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 700.dp) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DisplayDesign.entries.toList().chunked(columns).forEach { rowDesigns ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowDesigns.forEach { design ->
                        DesignPreviewCard(design, selected == design, { onSelect(design) }, Modifier.weight(1f))
                    }
                    repeat(columns - rowDesigns.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignPreviewCard(
    design: DisplayDesign,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(90), label = "${design.displayName} touch")
    val view = LocalView.current
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .height(86.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                when {
                    pressed -> SurfaceRaised.copy(alpha = .72f)
                    selected -> Accent.copy(alpha = .08f)
                    else -> SurfaceRaised.copy(alpha = .25f)
                },
            )
            .border(1.dp, if (selected) Accent.copy(alpha = .55f) else SecondaryText.copy(alpha = .12f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    view.performPremiumHaptic(strong = true)
                    onClick()
                },
            )
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(design.displayName, color = PrimaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    design.descriptor,
                    color = SecondaryText,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = .7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) Accent else SecondaryText.copy(alpha = .22f)),
            )
        }
        Spacer(Modifier.height(6.dp))
        DesignMiniature(design)
    }
}

@Composable
private fun DesignMiniature(design: DisplayDesign) {
    val miniatureSecondary = SecondaryText
    val miniatureAccent = Accent
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        if (design == DisplayDesign.MODERN_REFERENCE || design == DisplayDesign.MONOLITH_GLASS || design == DisplayDesign.CRYSTAL_ATRIUM) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(if (design == DisplayDesign.MODERN_REFERENCE) 3.dp else 7.dp))
                    .background(
                        when (design) {
                            DisplayDesign.MONOLITH_GLASS -> Brush.linearGradient(listOf(Accent.copy(alpha = .34f), SurfaceRaised))
                            DisplayDesign.CRYSTAL_ATRIUM -> Brush.linearGradient(listOf(PrimaryText.copy(alpha = .34f), Accent.copy(alpha = .12f)))
                            else -> Brush.linearGradient(listOf(SecondaryText.copy(alpha = .2f), SecondaryText.copy(alpha = .2f)))
                        }
                    )
                    .then(
                        if (design == DisplayDesign.MONOLITH_GLASS || design == DisplayDesign.CRYSTAL_ATRIUM) Modifier.border(1.dp, PrimaryText.copy(alpha = if (design == DisplayDesign.CRYSTAL_ATRIUM) .38f else .18f), RoundedCornerShape(7.dp))
                        else Modifier,
                    ),
            )
            Spacer(Modifier.width(9.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth(.78f).height(3.dp).background(PrimaryText.copy(alpha = .7f)))
            Box(Modifier.fillMaxWidth(.52f).height(2.dp).background(SecondaryText.copy(alpha = .45f)))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Accent.copy(alpha = .7f)))
        }
        if (design == DisplayDesign.STUDIO_LEDGER || design == DisplayDesign.PRECISION_DECK) {
            Spacer(Modifier.width(9.dp))
            if (design == DisplayDesign.PRECISION_DECK) {
                Box(
                    Modifier
                        .size(28.dp)
                        .border(1.dp, miniatureSecondary.copy(alpha = .3f), RoundedCornerShape(1.dp))
                        .padding(3.dp),
                ) {
                    Box(Modifier.fillMaxSize().background(SecondaryText.copy(alpha = .2f)))
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(miniatureAccent.copy(alpha = .75f)))
                }
            } else {
                Column(
                    Modifier
                        .size(28.dp)
                        .border(1.dp, SecondaryText.copy(alpha = .24f), RoundedCornerShape(1.dp))
                        .padding(3.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Accent.copy(alpha = .7f)))
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.fillMaxSize().background(SecondaryText.copy(alpha = .18f)))
                }
            }
        }
    }
}

@Composable
private fun PalettePickerGrid(
    selected: ColorPalette,
    mode: PaletteMode,
    onSelect: (ColorPalette) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 700.dp) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorPalette.entries.toList().chunked(columns).forEach { palettes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palettes.forEach { palette ->
                        PalettePickerRow(palette, mode, selected == palette, { onSelect(palette) }, Modifier.weight(1f))
                    }
                    repeat(columns - palettes.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePickerRow(
    palette: ColorPalette,
    mode: PaletteMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = palette.colors(mode)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(90), label = "${palette.displayName} touch")
    val view = LocalView.current
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    pressed -> SurfaceRaised.copy(alpha = .62f)
                    selected -> Accent.copy(alpha = .1f)
                    else -> Color.Transparent
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performPremiumHaptic(strong = true)
                    onClick()
                },
            )
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(colors.background, colors.primaryText, colors.accent).forEach { color ->
                Box(
                    Modifier
                            .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, SecondaryText.copy(alpha = .2f), CircleShape),
                )
            }
            }
            if (selected) {
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(20.dp).height(2.dp).background(Accent))
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(palette.displayName, color = PrimaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(palette.descriptor, color = SecondaryText, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = .65.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    selectedLabel: String = "SELECTED",
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .96f else 1f,
        animationSpec = tween(90),
        label = "$title touch",
    )
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    pressed && enabled -> SurfaceRaised.copy(alpha = .62f)
                    selected -> Accent.copy(alpha = .12f)
                    else -> Color.Transparent
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    view.performPremiumHaptic(strong = true)
                    onClick()
                },
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (selected) Accent else SecondaryText.copy(alpha = .25f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText.copy(alpha = if (enabled) 1f else .45f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = SecondaryText.copy(alpha = if (enabled) 1f else .45f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Text(selectedLabel, color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun DiagnosticsOverlay(state: MediaUiState, onDismiss: () -> Unit) {
    val diagnostics = state.diagnostics
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = .97f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 52.dp, vertical = 36.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SESSION DIAGNOSTICS",
                    color = Accent,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, "Cerrar diagnóstico", tint = SecondaryText)
                }
            }
            Spacer(Modifier.height(28.dp))
            DiagnosticLine("PACKAGE", diagnostics.packageName ?: "—")
            DiagnosticLine("STATE", diagnostics.playbackStatus.name)
            DiagnosticLine("ACTIONS", diagnostics.supportedActions.joinToString().ifBlank { "NONE" })
            DiagnosticLine("TITLE", diagnostics.hasTitle.availableLabel())
            DiagnosticLine("ARTIST", diagnostics.hasArtist.availableLabel())
            DiagnosticLine("ALBUM", diagnostics.hasAlbum.availableLabel())
            DiagnosticLine("ARTWORK", diagnostics.hasArtwork.availableLabel())
            DiagnosticLine("DURATION", diagnostics.hasDuration.availableLabel())
            DiagnosticLine("RETRY", diagnostics.retryAttempt.toString())
            diagnostics.errorType?.let { DiagnosticLine("ERROR", it) }
            Spacer(Modifier.weight(1f))
            Text(
                "No se muestra ni almacena contenido de notificaciones.",
                color = SecondaryText.copy(alpha = .7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(110.dp))
        Text(
            value,
            color = PrimaryText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Boolean.availableLabel() = if (this) "AVAILABLE" else "MISSING"

@Composable
private fun Artwork(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    playbackEffect: PlaybackArtworkEffect,
    pressed: Boolean,
    controlCue: ControlCue?,
    modifier: Modifier,
) {
    val playbackVisual = rememberPlaybackArtworkVisual(playbackEffect, state.isPlaying, state.positionMs)
    val animatedModifier = if (design == DisplayDesign.MONOLITH_GLASS) {
        modifier
    } else {
        modifier.graphicsLayer {
            scaleX *= playbackVisual.scale
            scaleY *= playbackVisual.scale
            translationX = playbackVisual.translationX
            translationY = playbackVisual.translationY
        }
    }
    when (design.tokens.artworkTreatment) {
        ArtworkTreatment.REFERENCE -> ReferenceArtwork(state, design, motion, pressed, controlCue, playbackVisual, animatedModifier)
        ArtworkTreatment.STUDIO_DECK -> StudioArtworkDeck(state, design, motion, pressed, controlCue, playbackVisual, animatedModifier)
        ArtworkTreatment.MONOLITH_GLASS -> MonolithGlassArtwork(state, design, motion, pressed, controlCue, playbackVisual, animatedModifier)
        ArtworkTreatment.PRECISION_FRAME -> PrecisionFrameArtwork(state, design, motion, pressed, controlCue, playbackVisual, animatedModifier)
        ArtworkTreatment.CRYSTAL_FLOAT -> CrystalFloatArtwork(state, design, motion, pressed, playbackVisual, animatedModifier)
    }
}

@Composable
private fun rememberPlaybackArtworkVisual(
    effect: PlaybackArtworkEffect,
    isPlaying: Boolean,
    positionMs: Long = 0L,
): PlaybackArtworkVisual {
    val latestPosition by rememberUpdatedState(positionMs)
    var phase by remember(effect) { mutableFloatStateOf(0f) }
    LaunchedEffect(effect, isPlaying) {
        phase = 0f
        if (isPlaying && effect != PlaybackArtworkEffect.STILL) {
            var observedPosition = Long.MIN_VALUE
            var anchorPosition = 0L
            var anchorFrameMs = 0L
            while (true) {
                withFrameMillis { frameMs ->
                    val reportedPosition = latestPosition
                    if (reportedPosition != observedPosition) {
                        observedPosition = reportedPosition
                        anchorPosition = reportedPosition
                        anchorFrameMs = frameMs
                    }
                    val estimatedPosition = anchorPosition + (frameMs - anchorFrameMs).coerceAtLeast(0L)
                    phase = resolvePlaybackEffectPhase(effect, estimatedPosition)
                }
            }
        }
    }
    return resolvePlaybackArtworkVisual(effect, phase, isPlaying)
}

@Composable
private fun ReferenceArtwork(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    pressed: Boolean,
    controlCue: ControlCue?,
    playbackVisual: PlaybackArtworkVisual,
    modifier: Modifier,
) {
    val frameShape = RoundedCornerShape(design.tokens.artworkCornerRadius)
    val frameColor by animateColorAsState(
        targetValue = if (pressed) Accent.copy(alpha = .52f) else SecondaryText.copy(alpha = .12f),
        animationSpec = tween(110),
        label = "artwork frame touch",
    )
    Box(
        modifier
            .clip(frameShape)
            .background(Brush.linearGradient(listOf(SurfaceRaised, Surface)))
            .border(1.dp, if (playbackVisual.haloAlpha > 0f) Accent.copy(alpha = playbackVisual.haloAlpha) else frameColor, frameShape),
        contentAlignment = Alignment.Center,
    ) {
        ArtworkVisual(state, motion)
        if (playbackVisual.sheenAlpha > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(PrimaryText.copy(alpha = playbackVisual.sheenAlpha), Color.Transparent, Color.Transparent),
                    ),
                ),
            )
        }
        if (pressed) Box(Modifier.fillMaxSize().background(Accent.copy(alpha = .025f)))
        ControlStateCue(controlCue, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
private fun StudioArtworkDeck(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    pressed: Boolean,
    controlCue: ControlCue?,
    playbackVisual: PlaybackArtworkVisual,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(design.tokens.artworkCornerRadius)
    val frameColor by animateColorAsState(
        targetValue = if (pressed) Accent.copy(alpha = .58f) else SecondaryText.copy(alpha = .2f),
        animationSpec = tween(110),
        label = "studio artwork touch",
    )
    Column(
        modifier
            .clip(shape)
            .background(Surface.copy(alpha = .94f))
            .border(1.dp, if (playbackVisual.haloAlpha > 0f) Accent.copy(alpha = playbackVisual.haloAlpha) else frameColor, shape)
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.Top) {
            Text(
                "MASTER SOURCE",
                color = SecondaryText.copy(alpha = .72f),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f),
            )
            Text("A / 01", color = Accent, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(1.dp))
                .background(SurfaceRaised),
        ) {
            ArtworkVisual(state, motion)
            if (playbackVisual.sheenAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrimaryText.copy(alpha = playbackVisual.sheenAlpha), Color.Transparent))))
            }
            if (pressed) Box(Modifier.fillMaxSize().background(Accent.copy(alpha = .025f)))
            ControlStateCue(controlCue, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
        }
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier
                    .size(if (state.isPlaying) 5.dp + (2.dp * playbackVisual.haloAlpha) else 5.dp)
                    .clip(CircleShape)
                    .background(if (state.isPlaying) Accent.copy(alpha = .72f + playbackVisual.haloAlpha * .28f) else SecondaryText.copy(alpha = .35f)),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                (state.sourceApp ?: "MEDIA SESSION").uppercase(),
                color = SecondaryText,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(if (state.isPlaying) "RUN" else "STBY", color = if (state.isPlaying) Accent else SecondaryText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun MonolithGlassArtwork(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    pressed: Boolean,
    controlCue: ControlCue?,
    playbackVisual: PlaybackArtworkVisual,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(if (design == DisplayDesign.MONOLITH_GLASS) 24.dp else design.tokens.artworkCornerRadius)
    val moving = playbackVisual.translationX != 0f || playbackVisual.translationY != 0f
    val protectedScale = if (moving) maxOf(1.025f, playbackVisual.scale) else playbackVisual.scale
    Box(
        modifier
            .clip(shape)
            .background(Surface)
            .border(
                1.dp,
                if (playbackVisual.haloAlpha > 0f) Accent.copy(alpha = playbackVisual.haloAlpha * .72f) else PrimaryText.copy(alpha = .08f),
                shape,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = protectedScale
                    scaleY = protectedScale
                    translationX = playbackVisual.translationX
                    translationY = playbackVisual.translationY
                },
        ) {
            if (state.artwork != null) ArtworkVisual(state, motion) else MonolithArtworkFallback(state.sourceApp)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PrimaryText.copy(alpha = .035f + playbackVisual.sheenAlpha * .35f),
                            Color.Transparent,
                            Accent.copy(alpha = playbackVisual.haloAlpha * .08f),
                            Background.copy(alpha = .34f),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PrimaryText.copy(alpha = .18f)),
        )
        if (pressed) Box(Modifier.fillMaxSize().background(PrimaryText.copy(alpha = .055f)))
    }
}

@Composable
private fun PrecisionFrameArtwork(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    pressed: Boolean,
    controlCue: ControlCue?,
    playbackVisual: PlaybackArtworkVisual,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(3.dp)
    val frameSecondary = SecondaryText
    val frameAccent = Accent
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(SurfaceRaised.copy(alpha = .68f), Surface.copy(alpha = .92f))))
            .border(
                1.dp,
                if (playbackVisual.haloAlpha > 0f) Accent.copy(alpha = playbackVisual.haloAlpha) else frameSecondary.copy(alpha = .2f),
                shape,
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceRaised)
                .border(1.dp, if (pressed) Accent.copy(alpha = .6f) else PrimaryText.copy(alpha = .12f), RoundedCornerShape(2.dp)),
        ) {
            if (state.artwork != null) ArtworkVisual(state, motion) else PrecisionArtworkFallback(state.sourceApp)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                PrimaryText.copy(alpha = playbackVisual.sheenAlpha * .72f),
                                Color.Transparent,
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            if (pressed) Box(Modifier.fillMaxSize().background(Accent.copy(alpha = .035f)))
        }
        Canvas(Modifier.fillMaxSize()) {
            val length = 18.dp.toPx()
            val stroke = 1.dp.toPx()
            val color = if (pressed) frameAccent.copy(alpha = .82f) else frameSecondary.copy(alpha = .38f)
            drawLine(color, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(length, 0f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(0f, length), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - length, 0f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, length), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(length, size.height), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - length), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - length, size.height), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - length), stroke)
        }
    }
}

@Composable
private fun CrystalFloatArtwork(
    state: MediaUiState,
    design: DisplayDesign,
    motion: ArtworkMotion,
    pressed: Boolean,
    playbackVisual: PlaybackArtworkVisual,
    modifier: Modifier,
) {
    val outerShape = RoundedCornerShape(design.tokens.artworkCornerRadius)
    val innerShape = RoundedCornerShape(design.tokens.artworkCornerRadius - 5.dp)
    Box(
        modifier
            .clip(outerShape)
            .background(Brush.linearGradient(listOf(PrimaryText.copy(alpha = .18f), Accent.copy(alpha = .055f), Surface.copy(alpha = .3f))))
            .border(1.dp, PrimaryText.copy(alpha = maxOf(.22f, playbackVisual.haloAlpha)), outerShape)
            .padding(5.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(innerShape)
                .background(SurfaceRaised.copy(alpha = .62f))
                .border(1.dp, PrimaryText.copy(alpha = .1f), innerShape),
        ) {
            if (state.artwork != null) ArtworkVisual(state, motion) else CrystalArtworkFallback(state.sourceApp)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to PrimaryText.copy(alpha = .13f + playbackVisual.sheenAlpha),
                                .24f to Color.Transparent,
                                .78f to Color.Transparent,
                                1f to Background.copy(alpha = .18f),
                            ),
                        ),
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(.72f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, PrimaryText.copy(alpha = (.64f + playbackVisual.sheenAlpha).coerceAtMost(.9f)), Color.Transparent))),
            )
            if (pressed) Box(Modifier.fillMaxSize().background(PrimaryText.copy(alpha = .07f)))
        }
    }
}

@Composable
private fun CrystalArtworkFallback(sourceApp: String?) {
    val glassText = PrimaryText
    val glassAccent = Accent
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(PrimaryText.copy(alpha = .14f), Surface.copy(alpha = .64f), Background.copy(alpha = .82f)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(22.dp)) {
            val radius = size.minDimension * .31f
            drawCircle(glassText.copy(alpha = .075f), radius = radius)
            drawCircle(glassText.copy(alpha = .22f), radius = radius, style = Stroke(1.dp.toPx()))
            drawCircle(glassText.copy(alpha = .12f), radius = radius * .72f, style = Stroke(1.dp.toPx()))
            drawCircle(glassAccent.copy(alpha = .42f), radius = radius * .16f)
            drawCircle(glassText.copy(alpha = .7f), radius = radius * .035f)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CLEAR SOURCE", color = PrimaryText.copy(alpha = .74f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.7.sp)
            sourceApp?.takeUnless(String::isBlank)?.let {
                Spacer(Modifier.height(3.dp))
                Text(it.uppercase(), color = SecondaryText.copy(alpha = .66f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MonolithArtworkFallback(sourceApp: String?) {
    val discColor = PrimaryText.copy(alpha = .12f)
    val grooveColor = Background.copy(alpha = .24f)
    val accentColor = Accent.copy(alpha = .68f)
    val fallbackBackground = Background
    val fallbackPrimary = PrimaryText
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(SurfaceRaised, Surface, Background))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * .43f
            val center = androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .47f)
            drawCircle(discColor, radius, center)
            repeat(11) { index ->
                drawCircle(
                    grooveColor.copy(alpha = .12f + index * .014f),
                    radius * (.28f + index * .06f),
                    center,
                    style = Stroke(1f),
                )
            }
            drawCircle(accentColor, radius * .18f, center)
            drawCircle(fallbackBackground, radius * .035f, center)
            drawLine(
                fallbackPrimary.copy(alpha = .14f),
                androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .9f),
                androidx.compose.ui.geometry.Offset(size.width * .76f, size.height * .9f),
                1.dp.toPx(),
            )
        }
        Text(
            sourceApp?.uppercase() ?: "MEDIA SESSION",
            color = PrimaryText.copy(alpha = .58f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.7.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun PrecisionArtworkFallback(sourceApp: String?) {
    val secondary = SecondaryText
    val accent = Accent
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(SurfaceRaised, Surface))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            drawCircle(secondary.copy(alpha = .12f), radius = size.minDimension * .34f)
            drawCircle(secondary.copy(alpha = .18f), radius = size.minDimension * .22f, style = Stroke(1.dp.toPx()))
            drawCircle(accent.copy(alpha = .54f), radius = size.minDimension * .08f)
            drawLine(secondary.copy(alpha = .16f), androidx.compose.ui.geometry.Offset(center.x, 0f), androidx.compose.ui.geometry.Offset(center.x, size.height), 1.dp.toPx())
            drawLine(secondary.copy(alpha = .16f), androidx.compose.ui.geometry.Offset(0f, center.y), androidx.compose.ui.geometry.Offset(size.width, center.y), 1.dp.toPx())
        }
        Text(
            sourceApp?.uppercase() ?: "NO COVER",
            color = SecondaryText.copy(alpha = .64f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.3.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}

private data class ControlCue(
    val showsControls: Boolean,
    val visible: Boolean,
)

@Composable
private fun ControlStateCue(state: ControlCue?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state?.visible == true,
        modifier = modifier,
        enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = .94f),
        exit = fadeOut(tween(240)),
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Background.copy(alpha = .86f))
                .border(1.dp, Accent.copy(alpha = .34f), RoundedCornerShape(3.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (state?.showsControls == true) Accent else SecondaryText))
            Spacer(Modifier.width(7.dp))
            Text(
                if (state?.showsControls == true) "CONTROLS" else "AMBIENT",
                color = if (state?.showsControls == true) PrimaryText else SecondaryText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

private data class ArtworkFrame(
    val transitionKey: String,
    val artwork: Bitmap?,
    val title: String?,
    val sourceApp: String?,
)

@Composable
private fun ArtworkVisual(state: MediaUiState, motion: ArtworkMotion) {
    val transitionKey = remember(
        state.selectedSourcePackage,
        state.title,
        state.artist,
        state.album,
        state.durationMs,
        state.artwork != null,
    ) {
        buildArtworkTransitionKey(
            sourcePackage = state.selectedSourcePackage,
            title = state.title,
            artist = state.artist,
            album = state.album,
            durationMs = state.durationMs,
            hasArtwork = state.artwork != null,
        )
    }
    val frame = remember(transitionKey) {
        ArtworkFrame(transitionKey, state.artwork, state.title, state.sourceApp)
    }
    when (motion) {
        ArtworkMotion.FOCUS -> AnimatedContent(
            targetState = frame,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 480, delayMillis = 30)) +
                    scaleIn(tween(durationMillis = 680), initialScale = .9f))
                    .togetherWith(fadeOut(tween(durationMillis = 240)))
            },
            label = "artwork focus",
        ) { ArtworkFrameContent(it) }

        ArtworkMotion.DISSOLVE -> Crossfade(
            targetState = frame,
            animationSpec = tween(700),
            label = "artwork dissolve",
        ) { ArtworkFrameContent(it) }

        ArtworkMotion.DECK -> AnimatedContent(
            targetState = frame,
            transitionSpec = {
                (fadeIn(tween(360)) + slideInHorizontally(tween(520)) { it / 4 })
                    .togetherWith(fadeOut(tween(260)) + slideOutHorizontally(tween(440)) { -it / 6 })
            },
            label = "artwork deck",
        ) { ArtworkFrameContent(it) }

        ArtworkMotion.DIRECT -> ArtworkFrameContent(frame)
    }
}

@Composable
private fun ArtworkFrameContent(frame: ArtworkFrame) {
    val artwork = frame.artwork
    if (artwork != null) {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = frame.title?.let { "Carátula de $it" },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        ArtworkFallback(frame.sourceApp)
    }
}

@Composable
private fun ArtworkFallback(sourceApp: String?) {
    val disc = PrimaryText.copy(alpha = .11f)
    val groove = SecondaryText.copy(alpha = .14f)
    val label = Accent.copy(alpha = .32f)
    val hole = Background
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SurfaceRaised.copy(alpha = .9f), Surface),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            val radius = size.minDimension * .36f
            drawCircle(color = disc, radius = radius)
            listOf(.24f, .42f, .60f, .78f, .94f).forEach { fraction ->
                drawCircle(
                    color = groove,
                    radius = radius * fraction,
                    style = Stroke(width = 1f),
                )
            }
            drawCircle(color = label, radius = radius * .22f)
            drawCircle(color = hole, radius = radius * .045f)
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(18.dp),
        ) {
            Text(
                text = "NO COVER",
                color = PrimaryText.copy(alpha = .72f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
            )
            sourceApp?.takeUnless(String::isBlank)?.let {
                Text(
                    text = it.uppercase(),
                    color = SecondaryText.copy(alpha = .6f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    state: MediaUiState,
    design: DisplayDesign,
    visible: Boolean,
    compact: Boolean,
    onInteraction: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val hasPrimaryControl = if (state.isPlaying) state.canPause else state.canPlay
    val hasAnyControl = state.canSkipPrevious || hasPrimaryControl || state.canSkipNext
    AnimatedVisibility(
        visible = visible && hasAnyControl,
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = .9f),
        exit = fadeOut(tween(220)) + scaleOut(tween(240), targetScale = .96f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            if (state.canSkipPrevious) ControlButton(design, Icons.Rounded.SkipPrevious, { onInteraction(); onPrevious() }, "Anterior", compact = compact)
            if (state.isPlaying && state.canPause) ControlButton(design, Icons.Rounded.Pause, { onInteraction(); onPause() }, "Pausa", primary = true, compact = compact)
            else if (!state.isPlaying && state.canPlay) ControlButton(design, Icons.Rounded.PlayArrow, { onInteraction(); onPlay() }, "Reproducir", primary = true, compact = compact)
            if (state.canSkipNext) ControlButton(design, Icons.Rounded.SkipNext, { onInteraction(); onNext() }, "Siguiente", compact = compact)
        }
    }
}

@Composable
private fun ControlButton(
    design: DisplayDesign,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit,
    label: String,
    primary: Boolean = false,
    compact: Boolean = false,
) {
    val treatment = design.tokens.controlTreatment
    val console = treatment == ControlTreatment.CONSOLE
    val glass = treatment == ControlTreatment.GLASS
    val machined = treatment == ControlTreatment.MACHINED
    val floatingGlass = treatment == ControlTreatment.FLOATING_GLASS
    val shape = when {
        machined -> RoundedCornerShape(2.dp)
        console -> RoundedCornerShape(4.dp)
        glass -> RoundedCornerShape(18.dp)
        floatingGlass -> RoundedCornerShape(22.dp)
        else -> CircleShape
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val feedback = remember { Animatable(1f) }
    val feedbackScope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .93f else 1f,
        animationSpec = tween(100),
        label = "$label press",
    )
    val container by animateColorAsState(
        targetValue = when {
            primary && console && pressed -> Accent.copy(alpha = .22f)
            primary && console -> Accent.copy(alpha = .11f)
            primary && machined && pressed -> Accent.copy(alpha = .24f)
            primary && machined -> Accent.copy(alpha = .13f)
            primary && glass && pressed -> PrimaryText.copy(alpha = .19f)
            primary && glass -> PrimaryText.copy(alpha = .1f)
            primary && floatingGlass && pressed -> PrimaryText.copy(alpha = .22f)
            primary && floatingGlass -> PrimaryText.copy(alpha = .13f)
            primary && pressed -> Accent.copy(alpha = .82f)
            primary -> Accent
            pressed -> SurfaceRaised.copy(alpha = .86f)
            console || machined -> SurfaceRaised.copy(alpha = .18f)
            glass -> PrimaryText.copy(alpha = .055f)
            floatingGlass -> PrimaryText.copy(alpha = .075f)
            else -> SurfaceRaised.copy(alpha = .34f)
        },
        animationSpec = tween(100),
        label = "$label color",
    )
    val size = when {
        (glass || floatingGlass) && primary -> 48.dp
        (glass || floatingGlass) -> 42.dp
        primary && compact -> 52.dp
        primary -> 56.dp
        else -> 48.dp
    }
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(container)
            .border(
                width = 1.dp,
                color = if (primary) Accent.copy(alpha = if (console || machined || glass || floatingGlass) .5f else .55f) else PrimaryText.copy(alpha = if (glass || floatingGlass) .18f else .12f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    view.performPremiumHaptic(strong = primary)
                    feedbackScope.launch {
                        feedback.snapTo(0f)
                        feedback.animateTo(1f, tween(320))
                    }
                    action()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pulseScale = .68f + feedback.value * .32f
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = 1f - feedback.value
                }
                .border(2.dp, if (primary) Accent else PrimaryText.copy(alpha = .78f), shape),
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (primary && (console || machined || glass || floatingGlass)) Accent else if (primary) Background else PrimaryText,
            modifier = Modifier.size(if (primary) size * .48f else size * .5f),
        )
    }
}

@Composable
private fun Progress(
    state: MediaUiState,
    design: DisplayDesign,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val duration = state.durationMs
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val progress = dragValue ?: if (duration != null && duration > 0) state.positionMs.toFloat() / duration else 0f
    val revealModifier = if (controlsVisible) Modifier else Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onInteraction,
    )
    Column(modifier.fillMaxWidth().height(64.dp).then(revealModifier)) {
        PremiumSeekBar(
            progress = progress,
            treatment = design.tokens.progressTreatment,
            interactive = controlsVisible,
            enabled = state.canSeek && duration != null,
            onValueChange = {
                onInteraction()
                dragValue = it
            },
            onValueChangeFinished = {
                val value = dragValue
                if (value != null && duration != null) onSeek((value * duration).roundToLong())
                dragValue = null
                view.performPremiumHaptic()
                onInteraction()
            },
            onSetProgress = { value ->
                if (duration != null) onSeek((value.coerceIn(0f, 1f) * duration).roundToLong())
                onInteraction()
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val studio = design == DisplayDesign.STUDIO_LEDGER
            val precision = design == DisplayDesign.PRECISION_DECK
            Text(
                when {
                    studio -> "ELAPSED  ${formatTime(state.positionMs)}"
                    precision -> "TIMEBASE  ${formatTime(state.positionMs)}"
                    else -> formatTime(state.positionMs)
                },
                color = SecondaryText,
                fontSize = if (studio || precision) 9.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = if (studio || precision) .8.sp else 0.sp,
            )
            Text(
                when {
                    studio -> "TOTAL  ${duration?.let(::formatTime) ?: "--:--"}"
                    precision -> "DURATION  ${duration?.let(::formatTime) ?: "--:--"}"
                    else -> duration?.let(::formatTime) ?: "--:--"
                },
                color = SecondaryText,
                fontSize = if (studio || precision) 9.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = if (studio || precision) .8.sp else 0.sp,
            )
        }
    }
}

@Composable
private fun PremiumSeekBar(
    progress: Float,
    treatment: ProgressTreatment,
    interactive: Boolean,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onSetProgress: (Float) -> Unit,
) {
    val fraction = progress.coerceIn(0f, 1f)
    val activeHeight by animateDpAsState(
        targetValue = if (interactive) 3.dp else 2.dp,
        animationSpec = tween(180),
        label = "seek track height",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (interactive && enabled) 1f else 0f,
        animationSpec = tween(160),
        label = "seek thumb",
    )
    val tickColor = SecondaryText
    val segmentedGapColor = Background
    val accessibilityModifier = Modifier.semantics {
        contentDescription = "Posición de reproducción"
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        if (interactive && enabled) {
            setProgress { value ->
                onSetProgress(value.coerceIn(0f, 1f))
                true
            }
        }
    }
    val gestureModifier = if (interactive && enabled) {
        Modifier.pointerInput(onValueChange, onValueChangeFinished) {
            awaitEachGesture {
                val down = awaitFirstDown()
                onValueChange((down.position.x / size.width).coerceIn(0f, 1f))
                drag(down.id) { change ->
                    change.consume()
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
                onValueChangeFinished()
            }
        }
    } else Modifier
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(accessibilityModifier)
            .then(gestureModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (treatment == ProgressTreatment.TICKED || treatment == ProgressTreatment.SEGMENTED) {
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                val centerY = size.height / 2f
                val marks = if (treatment == ProgressTreatment.SEGMENTED) 33 else 25
                repeat(marks) { index ->
                    val x = size.width * index / (marks - 1).toFloat()
                    val majorEvery = if (treatment == ProgressTreatment.SEGMENTED) 8 else 6
                    val halfHeight = if (index % majorEvery == 0) 5.dp.toPx() else 2.dp.toPx()
                    drawLine(
                        color = tickColor.copy(alpha = if (index % majorEvery == 0) .32f else .14f),
                        start = androidx.compose.ui.geometry.Offset(x, centerY - halfHeight),
                        end = androidx.compose.ui.geometry.Offset(x, centerY + halfHeight),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(SecondaryText.copy(alpha = .18f)),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(if (treatment == ProgressTreatment.LUMINOUS || treatment == ProgressTreatment.PRISMATIC) activeHeight + 2.dp else activeHeight)
                .clip(CircleShape)
                .background(Accent.copy(alpha = if (enabled) if (treatment == ProgressTreatment.LUMINOUS || treatment == ProgressTreatment.PRISMATIC) .74f else .86f else .46f)),
        )
        if (treatment == ProgressTreatment.LUMINOUS || treatment == ProgressTreatment.PRISMATIC) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(1.dp)
                    .background(PrimaryText.copy(alpha = if (treatment == ProgressTreatment.PRISMATIC) .94f else .82f)),
            )
        }
        if (treatment == ProgressTreatment.SEGMENTED) {
            Canvas(Modifier.fillMaxWidth().height(5.dp)) {
                repeat(32) { index ->
                    val x = size.width * (index + 1) / 33f
                    drawLine(
                        color = segmentedGapColor.copy(alpha = .9f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
        val thumbSize = 12.dp
        Box(
            Modifier
                .offset(x = (maxWidth - thumbSize) * fraction)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(Accent)
                .border(2.dp, Background, CircleShape),
        )
    }
}

private const val CONTROLS_TIMEOUT_MS = 6_000L
private const val CONTROL_CUE_DURATION_MS = 900L
private const val CONTROL_CUE_EXIT_MS = 260L
private const val BURN_IN_SHIFT_INTERVAL_MS = 90_000L
private val BURN_IN_OFFSETS = listOf(0 to 0, 1 to -1, -1 to 1, 1 to 1)

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

private fun View.performPremiumHaptic(strong: Boolean = false) {
    val feedback = when {
        strong && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.GESTURE_END
        strong && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
        strong -> HapticFeedbackConstants.LONG_PRESS
        else -> HapticFeedbackConstants.CLOCK_TICK
    }
    performHapticFeedback(feedback)
}
