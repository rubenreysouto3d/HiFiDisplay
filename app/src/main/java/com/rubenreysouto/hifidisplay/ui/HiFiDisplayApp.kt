package com.rubenreysouto.hifidisplay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubenreysouto.hifidisplay.media.MediaUiState
import kotlin.math.roundToLong

private val Background = Color(0xFF090B0D)
private val Surface = Color(0xFF111519)
private val SurfaceRaised = Color(0xFF20262B)
private val PrimaryText = Color(0xFFF4F6F0)
private val SecondaryText = Color(0xFF929A92)
private val Accent = Color(0xFFD6FF7F)

@Composable
fun HiFiDisplayApp(
    state: MediaUiState,
    onOpenAccessSettings: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Surface, primary = Accent)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            when {
                !state.hasNotificationAccess -> AccessRequired(onOpenAccessSettings)
                !state.hasActiveSession -> EmptySession()
                else -> NowPlaying(state, onPlay, onPause, onPrevious, onNext, onSeek)
            }
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
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Artwork(state, Modifier.fillMaxHeight().aspectRatio(1f))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            SourceHeader(state)
            Spacer(Modifier.weight(0.7f))
            Text(state.title ?: "Título no disponible", color = PrimaryText, fontSize = 38.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            state.artist?.let { Text(it, color = SecondaryText, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            state.album?.takeUnless(String::isBlank)?.let {
                Text(it.uppercase(), color = SecondaryText.copy(alpha = .65f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            PlayerControls(state, onPlay, onPause, onPrevious, onNext)
            Spacer(Modifier.height(20.dp))
            Progress(state, onSeek)
        }
    }
}

@Composable
private fun SourceHeader(state: MediaUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
        )
    }
}

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
            Image(artwork.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
private fun PlayerControls(state: MediaUiState, onPlay: () -> Unit, onPause: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        if (state.canSkipPrevious) ControlButton(Icons.Rounded.SkipPrevious, onPrevious, "Anterior")
        if (state.isPlaying && state.canPause) ControlButton(Icons.Rounded.Pause, onPause, "Pausa", true)
        else if (!state.isPlaying && state.canPlay) ControlButton(Icons.Rounded.PlayArrow, onPlay, "Reproducir", true)
        if (state.canSkipNext) ControlButton(Icons.Rounded.SkipNext, onNext, "Siguiente")
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
private fun Progress(state: MediaUiState, onSeek: (Long) -> Unit) {
    val duration = state.durationMs
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val progress = dragValue ?: if (duration != null && duration > 0) state.positionMs.toFloat() / duration else 0f
    Slider(
        value = progress.coerceIn(0f, 1f),
        onValueChange = if (state.canSeek && duration != null) ({ dragValue = it }) else ({ }),
        onValueChangeFinished = {
            val value = dragValue
            if (value != null && duration != null) onSeek((value * duration).roundToLong())
            dragValue = null
        },
        enabled = state.canSeek && duration != null,
        colors = SliderDefaults.colors(
            thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = SecondaryText.copy(alpha = .25f),
            disabledThumbColor = SecondaryText, disabledActiveTrackColor = SecondaryText.copy(alpha = .45f),
        ),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(state.positionMs), color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(duration?.let(::formatTime) ?: "--:--", color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
