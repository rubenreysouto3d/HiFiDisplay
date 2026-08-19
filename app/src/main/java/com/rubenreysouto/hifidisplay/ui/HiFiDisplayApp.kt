package com.rubenreysouto.hifidisplay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
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
                AnimatedVisibility(
                    visible = !activeDisplay || interaction.controlsVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(260)),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    DisplayMenuButton(
                        modifier = Modifier.padding(top = 18.dp, end = 22.dp),
                        onClick = {
                            showSourcePicker = true
                            dispatch(AmbientInteractionEvent.OVERLAY_OPENED)
                        },
                    )
                }
                if (activeDisplay && !interaction.controlsVisible) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { dispatch(AmbientInteractionEvent.INTERACT) },
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
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Artwork(state, Modifier.fillMaxHeight().aspectRatio(1f))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SourceHeader(
                    state = state,
                    controlsVisible = controlsVisible,
                    onShowSources = onShowSources,
                    onShowDiagnostics = onShowDiagnostics,
                )
                Spacer(Modifier.weight(0.7f))
                Text(state.title ?: "Título no disponible", color = PrimaryText, fontSize = 38.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                state.artist?.let { Text(it, color = SecondaryText, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                state.album?.takeUnless(String::isBlank)?.let {
                    Text(it.uppercase(), color = SecondaryText.copy(alpha = .65f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                PlayerControls(
                    state = state,
                    visible = controlsVisible,
                    onInteraction = onInteraction,
                    onPlay = onPlay,
                    onPause = onPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
                Spacer(Modifier.height(20.dp))
                Progress(state, controlsVisible, onInteraction, onSeek)
            }
        }
    }
}

@Composable
private fun SourceHeader(
    state: MediaUiState,
    controlsVisible: Boolean,
    onShowSources: () -> Unit,
    onShowDiagnostics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(max = 260.dp)
            .then(
                if (controlsVisible) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onShowSources() },
                            onLongPress = { onShowDiagnostics() },
                        )
                    }
                } else Modifier,
            ),
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
        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            Row {
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.ExpandMore, "Cambiar fuente", tint = SecondaryText, modifier = Modifier.size(16.dp))
            }
        }
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
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(SurfaceRaised, Surface))),
        contentAlignment = Alignment.Center,
    ) {
        val artwork = state.artwork
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = state.title?.let { "Carátula de $it" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Album, null, tint = SecondaryText.copy(alpha = .35f), modifier = Modifier.size(92.dp))
                Spacer(Modifier.height(12.dp))
                Text("SIN CARÁTULA", color = SecondaryText.copy(alpha = .65f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.4.sp)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    state: MediaUiState,
    visible: Boolean,
    onInteraction: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val hasPrimaryControl = if (state.isPlaying) state.canPause else state.canPlay
    val hasAnyControl = state.canSkipPrevious || hasPrimaryControl || state.canSkipNext
    Box(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(260)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            ) {
                if (!hasAnyControl) {
                    Text("CONTROLES NO DISPONIBLES", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp)
                } else {
                    if (state.canSkipPrevious) ControlButton(Icons.Rounded.SkipPrevious, { onInteraction(); onPrevious() }, "Anterior")
                    if (state.isPlaying && state.canPause) ControlButton(Icons.Rounded.Pause, { onInteraction(); onPause() }, "Pausa", true)
                    else if (!state.isPlaying && state.canPlay) ControlButton(Icons.Rounded.PlayArrow, { onInteraction(); onPlay() }, "Reproducir", true)
                    if (state.canSkipNext) ControlButton(Icons.Rounded.SkipNext, { onInteraction(); onNext() }, "Siguiente")
                }
            }
        }
    }
}

@Composable
private fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, label: String, primary: Boolean = false) {
    IconButton(
        onClick = action,
        modifier = Modifier
            .size(if (primary) 68.dp else 52.dp)
            .then(if (primary) Modifier.background(Accent, CircleShape) else Modifier),
    ) {
        Icon(icon, label, tint = if (primary) Background else PrimaryText, modifier = Modifier.fillMaxSize(if (primary) .58f else .72f))
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
    Column(Modifier.fillMaxWidth().height(64.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            if (controlsVisible) {
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = if (state.canSeek && duration != null) ({ onInteraction(); dragValue = it }) else ({ }),
                    onValueChangeFinished = {
                        val value = dragValue
                        if (value != null && duration != null) onSeek((value * duration).roundToLong())
                        dragValue = null
                        onInteraction()
                    },
                    enabled = state.canSeek && duration != null,
                    colors = SliderDefaults.colors(
                        thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = SecondaryText.copy(alpha = .25f),
                        disabledThumbColor = SecondaryText, disabledActiveTrackColor = SecondaryText.copy(alpha = .45f),
                    ),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(SecondaryText.copy(alpha = .18f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Accent.copy(alpha = .82f)),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(duration?.let(::formatTime) ?: "--:--", color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private const val CONTROLS_TIMEOUT_MS = 6_000L

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
