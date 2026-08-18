package com.rubenreysouto.hifidisplay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubenreysouto.hifidisplay.media.MediaUiState
import kotlin.math.roundToLong

private val Background = Color(0xFF08090A)
private val Surface = Color(0xFF151719)
private val PrimaryText = Color(0xFFF1EEE7)
private val SecondaryText = Color(0xFF999C9D)
private val Accent = Color(0xFFD8B36A)

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
        Text("HIFI DISPLAY", color = Accent, fontSize = 14.sp, letterSpacing = 5.sp)
        Spacer(Modifier.height(24.dp))
        Text("Acceso multimedia necesario", color = PrimaryText, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text(
            "Autoriza el acceso a notificaciones para leer y controlar la sesión multimedia activa.",
            color = SecondaryText,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)) {
            Text("ABRIR AJUSTES", letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun EmptySession() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HIFI DISPLAY", color = Accent, fontSize = 14.sp, letterSpacing = 5.sp)
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
            Text(state.sourceApp ?: "Sesión multimedia", color = Accent, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.weight(0.7f))
            Text(state.title ?: "Título no disponible", color = PrimaryText, fontSize = 38.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            state.artist?.let { Text(it, color = SecondaryText, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            state.album?.let { Text(it, color = SecondaryText.copy(alpha = .7f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.weight(1f))
            PlayerControls(state, onPlay, onPause, onPrevious, onNext)
            Spacer(Modifier.height(20.dp))
            Progress(state, onSeek)
        }
    }
}

@Composable
private fun Artwork(state: MediaUiState, modifier: Modifier) {
    Box(modifier.background(Surface, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
        val artwork = state.artwork
        if (artwork != null) {
            Image(artwork.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.Album, null, tint = SecondaryText.copy(alpha = .35f), modifier = Modifier.size(112.dp))
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
    IconButton(onClick = action, modifier = Modifier.size(if (primary) 68.dp else 52.dp)) {
        Icon(icon, label, tint = if (primary) Accent else PrimaryText, modifier = Modifier.fillMaxSize(if (primary) .85f else .72f))
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
        Text(formatTime(state.positionMs), color = SecondaryText, fontSize = 12.sp)
        Text(duration?.let(::formatTime) ?: "--:--", color = SecondaryText, fontSize = 12.sp)
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
