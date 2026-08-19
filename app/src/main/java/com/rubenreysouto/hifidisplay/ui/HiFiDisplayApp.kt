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
import androidx.compose.ui.semantics.Role
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
    val artworkInteraction = remember { MutableInteractionSource() }
    val metadataInteraction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compactHeight = maxHeight < 300.dp
            val horizontalPadding = if (maxWidth < 720.dp) 24.dp else 36.dp
            val verticalPadding = if (compactHeight) 18.dp else 28.dp
            val contentGap = if (maxWidth < 720.dp) 28.dp else 40.dp
            Row(
                Modifier
                    .fillMaxSize()
                    .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(contentGap),
            ) {
                Artwork(
                    state,
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = artworkInteraction,
                            indication = null,
                            onClick = onInteraction,
                        ),
                )
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    val compactWidth = maxWidth < 400.dp
                    val compact = compactHeight || compactWidth
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().height(if (compact) 54.dp else 64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SourceHeader(
                                state = state,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .widthIn(max = if (compact) 170.dp else 220.dp),
                                onShowSources = onShowSources,
                                onShowDiagnostics = onShowDiagnostics,
                            )
                            Spacer(Modifier.width(if (compact) 8.dp else 16.dp))
                            PlayerControls(
                                state = state,
                                visible = controlsVisible,
                                compact = compact,
                                onInteraction = onInteraction,
                                onPlay = onPlay,
                                onPause = onPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                            )
                        }
                        TrackMetadata(
                            state = state,
                            compact = compact,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = metadataInteraction,
                                    indication = null,
                                    onClick = onInteraction,
                                ),
                        )
                        Progress(state, controlsVisible, onInteraction, onSeek)
                    }
                }
            }
        }
    }
}

private data class TrackCopy(
    val title: String,
    val artist: String?,
    val album: String?,
)

@Composable
private fun TrackMetadata(state: MediaUiState, compact: Boolean, modifier: Modifier = Modifier) {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceHeader(
    state: MediaUiState,
    modifier: Modifier = Modifier,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container by animateColorAsState(
        targetValue = if (pressed) SurfaceRaised.copy(alpha = .8f) else SurfaceRaised.copy(alpha = .34f),
        animationSpec = tween(120),
        label = "source press",
    )
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(container)
            .border(1.dp, SecondaryText.copy(alpha = if (pressed) .26f else .12f), RoundedCornerShape(5.dp))
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
                .size(7.dp)
                .clip(CircleShape)
                .background(if (state.isPlaying) Accent else SecondaryText.copy(alpha = .4f))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            (state.sourceApp ?: "Sesión multimedia").uppercase(),
            color = if (state.isPlaying) Accent else SecondaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.7.sp,
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
    onSelectPalette: (ColorPalette) -> Unit,
    onShowDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = .98f))
            .padding(horizontal = 48.dp, vertical = 30.dp),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
            Spacer(Modifier.height(18.dp))
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
            Spacer(Modifier.height(22.dp))
            Text("DESIGN", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(7.dp))
            AppearanceInfoRow(
                title = appearance.design.displayName,
                subtitle = "STRUCTURE · TYPOGRAPHY · INTERACTION",
            )
            Spacer(Modifier.height(22.dp))
            Text("PALETTE", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(7.dp))
            ColorPalette.entries.forEach { option ->
                PickerRow(
                    title = option.displayName,
                    subtitle = option.storageKey.uppercase(),
                    selected = appearance.palette == option,
                    onClick = { onSelectPalette(option) },
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("TOOLS", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(7.dp))
            PickerRow(
                title = "Session diagnostics",
                subtitle = if (state.hasActiveSession) "VIEW SESSION CAPABILITIES" else "REQUIRES AN ACTIVE SESSION",
                selected = false,
                enabled = state.hasActiveSession,
                onClick = onShowDiagnostics,
            )
        }
    }
}

@Composable
private fun AppearanceInfoRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceRaised.copy(alpha = .45f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Accent))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("ACTIVE", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
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
private fun Artwork(state: MediaUiState, modifier: Modifier) {
    val frameShape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(frameShape)
            .background(Brush.linearGradient(listOf(SurfaceRaised, Surface)))
            .border(1.dp, SecondaryText.copy(alpha = .12f), frameShape),
        contentAlignment = Alignment.Center,
    ) {
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
            if (state.canSkipPrevious) ControlButton(Icons.Rounded.SkipPrevious, { onInteraction(); onPrevious() }, "Anterior", compact = compact)
            if (state.isPlaying && state.canPause) ControlButton(Icons.Rounded.Pause, { onInteraction(); onPause() }, "Pausa", primary = true, compact = compact)
            else if (!state.isPlaying && state.canPlay) ControlButton(Icons.Rounded.PlayArrow, { onInteraction(); onPlay() }, "Reproducir", primary = true, compact = compact)
            if (state.canSkipNext) ControlButton(Icons.Rounded.SkipNext, { onInteraction(); onNext() }, "Siguiente", compact = compact)
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit,
    label: String,
    primary: Boolean = false,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .93f else 1f,
        animationSpec = tween(100),
        label = "$label press",
    )
    val container by animateColorAsState(
        targetValue = when {
            primary && pressed -> Accent.copy(alpha = .82f)
            primary -> Accent
            pressed -> SurfaceRaised.copy(alpha = .86f)
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
            .clip(CircleShape)
            .background(container)
            .border(
                width = 1.dp,
                color = if (primary) Accent.copy(alpha = .55f) else SecondaryText.copy(alpha = .12f),
                shape = CircleShape,
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
            tint = if (primary) Background else PrimaryText,
            modifier = Modifier.size(if (primary) size * .48f else size * .5f),
        )
    }
}

@Composable
private fun Progress(
    state: MediaUiState,
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
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(duration?.let(::formatTime) ?: "--:--", color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PremiumSeekBar(
    progress: Float,
    interactive: Boolean,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
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
            .then(gestureModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
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
