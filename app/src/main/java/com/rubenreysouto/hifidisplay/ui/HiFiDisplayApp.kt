package com.rubenreysouto.hifidisplay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubenreysouto.hifidisplay.media.MediaUiState
import com.rubenreysouto.hifidisplay.media.SessionAvailability
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

private val Background: Color @Composable get() = MaterialTheme.colorScheme.background
private val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
private val SurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val PrimaryText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val SecondaryText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary

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
) {
    val colors = appearance.palette.colors
    var showSourcePicker by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
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
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceRaised,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            primary = colors.accent,
            onPrimary = colors.background,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Box(Modifier.fillMaxSize()) {
                when (state.availability) {
                    SessionAvailability.PERMISSION_REQUIRED -> AccessRequired(onOpenAccessSettings)
                    SessionAvailability.NO_SESSION -> EmptySession()
                    SessionAvailability.ERROR -> SessionError()
                    SessionAvailability.ACTIVE -> NowPlaying(
                        state = state,
                        design = appearance.design,
                        onPlay = onPlay,
                        onPause = onPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSeek = onSeek,
                        controlsVisible = interaction.controlsVisible,
                        onInteraction = { dispatch(AmbientInteractionEvent.INTERACT) },
                        onShowSources = {
                            showSourcePicker = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                        onShowDiagnostics = {
                            showDiagnostics = true
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
            }
        }
    }
}

@Composable
private fun DisplayMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceRaised.copy(alpha = .94f))
            .clickable(onClick = onClick)
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
private fun NowPlaying(
    state: MediaUiState,
    design: DisplayDesign,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
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
            burnInOffset = burnInOffset,
            controlsVisible = controlsVisible,
            onInteraction = onInteraction,
            onShowSources = onShowSources,
            onShowDiagnostics = onShowDiagnostics,
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
    burnInOffset: Pair<Int, Int>,
    controlsVisible: Boolean,
    onInteraction: () -> Unit,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val tokens = design.tokens
    val artworkInteraction = remember { MutableInteractionSource() }
    val metadataInteraction = remember { MutableInteractionSource() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
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
        Row(
            Modifier
                .fillMaxSize()
                .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            if (tokens.artworkPlacement == ArtworkPlacement.LEADING) {
                Artwork(
                    state = state,
                    design = design,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = onInteraction,
                        ),
                )
                Spacer(Modifier.width(contentGap))
            }
            PlaybackInformation(
                state = state,
                design = design,
                compact = compact,
                controlsVisible = controlsVisible,
                onInteraction = onInteraction,
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
                    .clickable(
                        interactionSource = metadataInteraction,
                        indication = null,
                        onClick = onInteraction,
                    ),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            if (tokens.artworkPlacement == ArtworkPlacement.TRAILING) {
                Spacer(Modifier.width(contentGap))
                Artwork(
                    state = state,
                    design = design,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = onInteraction,
                        ),
                )
            }
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
            SourceHeader(
                state = state,
                design = design,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = if (compact) 170.dp else 220.dp),
                onShowSources = onShowSources,
                onShowDiagnostics = onShowDiagnostics,
            )
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
        Crossfade(
            targetState = copy,
            animationSpec = tween(420),
            label = "track metadata",
        ) { track ->
            when (design) {
                DisplayDesign.MODERN_REFERENCE -> ModernMetadata(track, compact)
                DisplayDesign.STUDIO_LEDGER -> StudioMetadata(track, compact)
            }
        }
    }
}

@Composable
private fun ModernMetadata(track: TrackCopy, compact: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = track.title,
            color = PrimaryText,
            fontSize = if (compact) 29.sp else 40.sp,
            lineHeight = if (compact) 33.sp else 44.sp,
            fontWeight = FontWeight.Medium,
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
                .background(Accent.copy(alpha = .82f)),
        )
        Spacer(Modifier.width(if (compact) 14.dp else 20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = "PROGRAM / NOW PLAYING",
                color = Accent.copy(alpha = .82f),
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
    val shape = RoundedCornerShape(design.tokens.sourceCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container by animateColorAsState(
        targetValue = when {
            pressed -> SurfaceRaised.copy(alpha = .8f)
            studio -> SurfaceRaised.copy(alpha = .18f)
            else -> SurfaceRaised.copy(alpha = .34f)
        },
        animationSpec = tween(120),
        label = "source press",
    )
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, SecondaryText.copy(alpha = if (pressed) .3f else .12f), shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onShowSources,
                onLongClick = onShowDiagnostics,
                onLongClickLabel = "Abrir diagnóstico",
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .then(if (studio) Modifier.width(3.dp).height(14.dp) else Modifier.size(7.dp))
                .clip(if (studio) RoundedCornerShape(1.dp) else CircleShape)
                .background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .4f))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            (state.sourceApp ?: "Sesión multimedia").uppercase(),
            color = if (state.isPlaying) Accent else SecondaryText,
            fontSize = if (studio) 10.sp else 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = if (studio) 2.sp else 1.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.ExpandMore, "Abrir opciones de fuente y display", tint = SecondaryText, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SourcePickerOverlay(
    state: MediaUiState,
    appearance: DisplayAppearance,
    onSelectSource: (String?) -> Unit,
    onSelectDesign: (DisplayDesign) -> Unit,
    onSelectPalette: (ColorPalette) -> Unit,
    onShowDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 20.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SOURCE / DISPLAY",
                    color = Accent,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, "Cerrar", tint = SecondaryText)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("FUENTE", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
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
                Spacer(Modifier.height(16.dp))
                Text("DESIGN", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
                DesignPreviewGrid(
                    selected = appearance.design,
                    onSelect = onSelectDesign,
                )
                Spacer(Modifier.height(16.dp))
                Text("PALETTE", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
                ColorPalette.entries.forEach { option ->
                    PalettePickerRow(
                        palette = option,
                        selected = appearance.palette == option,
                        onClick = { onSelectPalette(option) },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("TOOLS", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
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
    }
}

@Composable
private fun DesignPreviewGrid(
    selected: DisplayDesign,
    onSelect: (DisplayDesign) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DisplayDesign.entries.forEach { design ->
                    DesignPreviewCard(design, selected == design, { onSelect(design) }, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DisplayDesign.entries.forEach { design ->
                    DesignPreviewCard(design, selected == design, { onSelect(design) }, Modifier.weight(1f))
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
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .height(86.dp)
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
                onClick = onClick,
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
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        if (design == DisplayDesign.MODERN_REFERENCE) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(3.dp)).background(SecondaryText.copy(alpha = .2f)))
            Spacer(Modifier.width(9.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth(.78f).height(3.dp).background(PrimaryText.copy(alpha = .7f)))
            Box(Modifier.fillMaxWidth(.52f).height(2.dp).background(SecondaryText.copy(alpha = .45f)))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Accent.copy(alpha = .7f)))
        }
        if (design == DisplayDesign.STUDIO_LEDGER) {
            Spacer(Modifier.width(9.dp))
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

@Composable
private fun PalettePickerRow(
    palette: ColorPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = palette.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Accent.copy(alpha = .1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(colors.background, colors.primaryText, colors.accent).forEach { color ->
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, SecondaryText.copy(alpha = .2f), CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(palette.displayName, color = PrimaryText, fontSize = 14.sp)
            Text("COLOR ONLY", color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }
        if (selected) Text("ACTIVE", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Accent.copy(alpha = .12f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
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
        if (selected) Text("SELECTED", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
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
    modifier: Modifier,
) {
    when (design.tokens.artworkTreatment) {
        ArtworkTreatment.REFERENCE -> ReferenceArtwork(state, design, modifier)
        ArtworkTreatment.STUDIO_DECK -> StudioArtworkDeck(state, design, modifier)
    }
}

@Composable
private fun ReferenceArtwork(state: MediaUiState, design: DisplayDesign, modifier: Modifier) {
    val frameShape = RoundedCornerShape(design.tokens.artworkCornerRadius)
    Box(
        modifier
            .clip(frameShape)
            .background(Brush.linearGradient(listOf(SurfaceRaised, Surface)))
            .border(1.dp, SecondaryText.copy(alpha = .12f), frameShape),
        contentAlignment = Alignment.Center,
    ) {
        ArtworkVisual(state)
    }
}

@Composable
private fun StudioArtworkDeck(state: MediaUiState, design: DisplayDesign, modifier: Modifier) {
    val shape = RoundedCornerShape(design.tokens.artworkCornerRadius)
    Column(
        modifier
            .clip(shape)
            .background(Surface.copy(alpha = .94f))
            .border(1.dp, SecondaryText.copy(alpha = .2f), shape)
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
            ArtworkVisual(state)
        }
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .35f)))
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
private fun ArtworkVisual(state: MediaUiState) {
    Crossfade(
        targetState = state.artwork,
        animationSpec = tween(480),
        label = "album artwork",
    ) { artwork ->
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = state.title?.let { "Carátula de $it" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            ArtworkFallback(state.sourceApp)
        }
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
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(260)),
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
    val console = design.tokens.controlTreatment == ControlTreatment.CONSOLE
    val shape = if (console) RoundedCornerShape(4.dp) else CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .93f else 1f,
        animationSpec = tween(100),
        label = "$label press",
    )
    val container by animateColorAsState(
        targetValue = when {
            primary && console && pressed -> Accent.copy(alpha = .22f)
            primary && console -> Accent.copy(alpha = .11f)
            primary && pressed -> Accent.copy(alpha = .82f)
            primary -> Accent
            pressed -> SurfaceRaised.copy(alpha = .86f)
            console -> SurfaceRaised.copy(alpha = .18f)
            else -> SurfaceRaised.copy(alpha = .34f)
        },
        animationSpec = tween(100),
        label = "$label color",
    )
    val size = when {
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
                color = if (primary) Accent.copy(alpha = if (console) .5f else .55f) else SecondaryText.copy(alpha = .12f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = action,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (primary && console) Accent else if (primary) Background else PrimaryText,
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
) {
    val duration = state.durationMs
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val progress = dragValue ?: if (duration != null && duration > 0) state.positionMs.toFloat() / duration else 0f
    val revealModifier = if (controlsVisible) Modifier else Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onInteraction,
    )
    Column(Modifier.fillMaxWidth().height(64.dp).then(revealModifier)) {
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
                onInteraction()
            },
            onSetProgress = { value ->
                if (duration != null) onSeek((value.coerceIn(0f, 1f) * duration).roundToLong())
                onInteraction()
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val studio = design == DisplayDesign.STUDIO_LEDGER
            Text(
                if (studio) "ELAPSED  ${formatTime(state.positionMs)}" else formatTime(state.positionMs),
                color = SecondaryText,
                fontSize = if (studio) 9.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = if (studio) .8.sp else 0.sp,
            )
            Text(
                if (studio) "TOTAL  ${duration?.let(::formatTime) ?: "--:--"}" else duration?.let(::formatTime) ?: "--:--",
                color = SecondaryText,
                fontSize = if (studio) 9.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = if (studio) .8.sp else 0.sp,
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
        if (treatment == ProgressTreatment.TICKED) {
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                val centerY = size.height / 2f
                repeat(25) { index ->
                    val x = size.width * index / 24f
                    val halfHeight = if (index % 6 == 0) 5.dp.toPx() else 2.dp.toPx()
                    drawLine(
                        color = tickColor.copy(alpha = if (index % 6 == 0) .28f else .16f),
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
                .height(activeHeight)
                .clip(CircleShape)
                .background(Accent.copy(alpha = if (enabled) .86f else .46f)),
        )
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
private const val BURN_IN_SHIFT_INTERVAL_MS = 90_000L
private val BURN_IN_OFFSETS = listOf(0 to 0, 1 to -1, -1 to 1, 1 to 1)

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
