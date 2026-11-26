package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.data.local.AppSettings
import com.example.data.model.EqPreset
import com.example.data.model.Track
import com.example.data.model.formatDuration
import com.example.player.EqualizerState
import com.example.player.PlayerUiState
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
  playerState: PlayerUiState,
  equalizerState: EqualizerState,
  settings: AppSettings,
  isFavorite: Boolean,
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onEqEnabledChange: (Boolean) -> Unit,
  onEqPresetChange: (EqPreset) -> Unit,
  onEqBandsChange: (Float, Float, Float) -> Unit,
) {
  BackHandler(onBack = onClose)

  val track = playerState.currentTrack
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary

  var showEqualizerSheet by remember { mutableStateOf(false) }

  var isSeeking by remember { mutableStateOf(false) }
  var seekDraftMs by remember(playerState.positionMs) {
    mutableFloatStateOf(playerState.positionMs.toFloat())
  }
  val displayPosition = if (isSeeking) seekDraftMs.toLong() else playerState.positionMs
  val durationMs = playerState.durationMs.coerceAtLeast(1L)

  val infiniteTransition = rememberInfiniteTransition(label = "player_art_anim")
  val auraScale by if (settings.effectiveAnimations && playerState.isPlaying) {
    infiniteTransition.animateFloat(
      initialValue = 1.0f,
      targetValue = 1.05f,
      animationSpec = infiniteRepeatable(
        animation = tween(1600, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "art_scale",
    )
  } else {
    remember { mutableFloatStateOf(1.0f) }
  }

  val ringRotation by if (settings.effectiveAnimations && playerState.isPlaying) {
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(14000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "ring_rot",
    )
  } else {
    remember { mutableFloatStateOf(0f) }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xF5060C1D),
            Color(0xF5040814),
            Color(0xFF03050F),
          )
        )
      )
      .safeDrawingPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 560.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Action Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(48.dp).testTag("player_close_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Minimizar player",
            modifier = Modifier.size(30.dp),
          )
        }
        Text(
          text = "AGORA TOCANDO",
          style = MaterialTheme.typography.labelLarge,
          color = primary,
        )
        Row {
          IconButton(
            onClick = { showEqualizerSheet = true },
            modifier = Modifier.size(48.dp).testTag("player_eq_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Equalizer,
              contentDescription = "Equalizador",
              tint = if (settings.eqEnabled) primary else TextSecondary,
            )
          }
          if (track != null) {
            IconButton(
              onClick = { onAddToPlaylist(track) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.PlaylistAdd,
                contentDescription = "Adicionar à playlist",
                tint = TextSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Album Artwork with Ki Aura Ring
      Box(
        modifier = Modifier
          .fillMaxWidth(0.78f)
          .aspectRatio(1f),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = ringRotation },
        ) {
          drawCircle(
            brush = Brush.sweepGradient(
              colors = listOf(
                primary.copy(alpha = 0.7f),
                secondary.copy(alpha = 0.4f),
                Color.Transparent,
                primary.copy(alpha = 0.7f),
              )
            ),
            radius = size.minDimension * 0.49f,
            style = Stroke(width = 3.dp.toPx()),
          )
        }
        TrackArtwork(
          artUri = track?.albumArtUri,
          contentDescription = track?.title ?: "Capa",
          size = 240.dp,
          cornerRadius = 28.dp,
          modifier = Modifier
            .fillMaxSize(0.90f)
            .graphicsLayer {
              scaleX = auraScale
              scaleY = auraScale
            }
            .shadow(22.dp, RoundedCornerShape(28.dp), spotColor = primary)
            .border(1.5.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Title, Artist & Favorite
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track?.title ?: "Nenhuma música selecionada",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = track?.let { "${it.displayArtist} • ${it.displayAlbum}" } ?: "Escolha uma faixa na biblioteca",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (track != null) {
          IconButton(
            onClick = { onToggleFavorite(track.uri) },
            modifier = Modifier.size(48.dp).testTag("player_favorite_button"),
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = if (isFavorite) "Remover dos favoritos" else "Favoritar",
              tint = if (isFavorite) tertiary else TextMuted,
              modifier = Modifier.size(28.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Interactive Seek Bar
      Slider(
        value = displayPosition.toFloat().coerceIn(0f, durationMs.toFloat()),
        onValueChange = {
          isSeeking = true
          seekDraftMs = it
        },
        onValueChangeFinished = {
          onSeekTo(seekDraftMs.toLong())
          isSeeking = false
        },
        valueRange = 0f..durationMs.toFloat(),
        colors = SliderDefaults.colors(
          thumbColor = primary,
          activeTrackColor = primary,
          inactiveTrackColor = Color(0x33FFFFFF),
        ),
        modifier = Modifier.fillMaxWidth().testTag("player_seek_slider"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = formatDuration(displayPosition),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
        Text(
          text = formatDuration(playerState.durationMs),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Transport Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        IconButton(
          onClick = onToggleShuffle,
          modifier = Modifier.size(48.dp).testTag("player_shuffle_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Shuffle,
            contentDescription = "Ordem aleatória",
            tint = if (playerState.shuffleEnabled) primary else TextMuted,
          )
        }

        IconButton(
          onClick = onPrevious,
          modifier = Modifier.size(54.dp).testTag("player_prev_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "Faixa anterior",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(36.dp),
          )
        }

        Surface(
          onClick = onTogglePlayPause,
          shape = CircleShape,
          color = primary,
          shadowElevation = 14.dp,
          modifier = Modifier.size(72.dp).testTag("player_play_pause_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playerState.isPlaying) "Pausar" else "Reproduzir",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(36.dp),
            )
          }
        }

        IconButton(
          onClick = onNext,
          modifier = Modifier.size(54.dp).testTag("player_next_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Próxima faixa",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(36.dp),
          )
        }

        IconButton(
          onClick = onCycleRepeat,
          modifier = Modifier.size(48.dp).testTag("player_repeat_button"),
        ) {
          val repeatIcon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
            Icons.Filled.RepeatOne
          } else {
            Icons.Filled.Repeat
          }
          val repeatActive = playerState.repeatMode != Player.REPEAT_MODE_OFF
          Icon(
            imageVector = repeatIcon,
            contentDescription = "Repetição",
            tint = if (repeatActive) primary else TextMuted,
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Device Media Volume Control
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x880C1530),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
            contentDescription = "Volume mínimo",
            tint = TextSecondary,
          )
          Slider(
            value = playerState.volumeRatio,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("player_volume_slider"),
            colors = SliderDefaults.colors(
              thumbColor = secondary,
              activeTrackColor = secondary,
            ),
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Volume máximo",
            tint = TextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showEqualizerSheet) {
    ModalBottomSheet(
      onDismissRequest = { showEqualizerSheet = false },
      containerColor = Color(0xFF0B132B),
    ) {
      EqualizerSheetContent(
        equalizerState = equalizerState,
        settings = settings,
        onEqEnabledChange = onEqEnabledChange,
        onEqPresetChange = onEqPresetChange,
        onEqBandsChange = onEqBandsChange,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerSheetContent(
  equalizerState: EqualizerState,
  settings: AppSettings,
  onEqEnabledChange: (Boolean) -> Unit,
  onEqPresetChange: (EqPreset) -> Unit,
  onEqBandsChange: (Float, Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Equalizador de Áudio", style = MaterialTheme.typography.titleLarge)
        Text(
          text = when {
            !equalizerState.isSupported -> equalizerState.errorMessage ?: "Equalizador indisponível"
            equalizerState.isAttached -> "${equalizerState.bandCount} bandas de hardware ativas"
            else -> "Reproduza uma música para vincular a sessão de áudio"
          },
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
      Switch(
        checked = settings.eqEnabled,
        onCheckedChange = onEqEnabledChange,
        enabled = equalizerState.isSupported,
      )
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      EqPreset.entries.forEach { preset ->
        FilterChip(
          selected = settings.eqPreset == preset,
          onClick = { onEqPresetChange(preset) },
          label = { Text(preset.label) },
          enabled = equalizerState.isSupported,
        )
      }
    }

    EqBandSlider(
      label = "Graves",
      value = settings.eqBassLevel,
      enabled = settings.eqEnabled && equalizerState.isSupported,
      onValueChange = { onEqBandsChange(it, settings.eqMidLevel, settings.eqTrebleLevel) },
    )
    EqBandSlider(
      label = "Médios",
      value = settings.eqMidLevel,
      enabled = settings.eqEnabled && equalizerState.isSupported,
      onValueChange = { onEqBandsChange(settings.eqBassLevel, it, settings.eqTrebleLevel) },
    )
    EqBandSlider(
      label = "Agudos",
      value = settings.eqTrebleLevel,
      enabled = settings.eqEnabled && equalizerState.isSupported,
      onValueChange = { onEqBandsChange(settings.eqBassLevel, settings.eqMidLevel, it) },
    )

    if (equalizerState.bandCenterFreqsHz.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        equalizerState.bandCenterFreqsHz.forEachIndexed { idx, hz ->
          val label = if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
          val db = (equalizerState.bandLevelsMb.getOrNull(idx) ?: 0) / 100
          Text(
            text = "$label\n${if (db >= 0) "+$db" else "$db"}dB",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    } else {
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun EqBandSlider(
  label: String,
  value: Float,
  enabled: Boolean,
  onValueChange: (Float) -> Unit,
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.titleSmall)
      Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = -1f..1f,
      enabled = enabled,
    )
  }
}
