package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PlaylistUi
import com.example.data.model.Track
import com.example.data.model.formatDuration
import com.example.player.PlayerUiState
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val cardModifier = if (onClick != null) {
    modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
  } else {
    modifier
  }
  Card(
    modifier = cardModifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = GlassBackground),
    border = BorderStroke(1.dp, borderColor),
  ) {
    content()
  }
}

@Composable
fun TrackArtwork(
  artUri: Any?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: Dp = 52.dp,
  cornerRadius: Dp = 12.dp,
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(cornerRadius))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_dbz_album_placeholder),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    if (artUri != null) {
      AsyncImage(
        model = artUri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
fun PlaylistCoverImage(
  playlist: PlaylistUi,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  cornerRadius: Dp = 14.dp,
) {
  val customCover = remember(playlist.coverPath) {
    playlist.coverPath?.let { File(it) }?.takeIf { it.exists() }
  }
  val model: Any? = customCover ?: playlist.tracks.firstOrNull()?.albumArtUri
  TrackArtwork(
    artUri = model,
    contentDescription = "Capa da playlist ${playlist.name}",
    modifier = modifier,
    size = size,
    cornerRadius = cornerRadius,
  )
}

@Composable
fun TrackRow(
  track: Track,
  isCurrentTrack: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAddToPlaylist: () -> Unit,
  modifier: Modifier = Modifier,
  extraMenuLabel: String? = null,
  onExtraMenuClick: (() -> Unit)? = null,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val primary = MaterialTheme.colorScheme.primary
  val containerColor = if (isCurrentTrack) {
    primary.copy(alpha = 0.16f)
  } else {
    Color(0x880C1530)
  }
  val borderColor = if (isCurrentTrack) {
    primary.copy(alpha = 0.65f)
  } else {
    Color(0x22FFFFFF)
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("track_row_${track.id}"),
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center) {
        TrackArtwork(
          artUri = track.albumArtUri,
          contentDescription = "Capa de ${track.title}",
          size = 50.dp,
          cornerRadius = 12.dp,
        )
        if (isCurrentTrack) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0x88050814)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.Pause,
              contentDescription = if (isPlaying) "Reproduzindo" else "Pausada",
              tint = primary,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleMedium,
          color = if (isCurrentTrack) primary else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${track.displayArtist} • ${formatDuration(track.durationMs)}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.testTag("favorite_btn_${track.id}"),
      ) {
        Icon(
          imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
          contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
          tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else TextMuted,
        )
      }

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Opções da faixa",
            tint = TextSecondary,
          )
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text("Reproduzir") },
            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onClick()
            },
          )
          DropdownMenuItem(
            text = { Text("Adicionar à playlist") },
            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onAddToPlaylist()
            },
          )
          if (extraMenuLabel != null && onExtraMenuClick != null) {
            DropdownMenuItem(
              text = { Text(extraMenuLabel) },
              onClick = {
                menuExpanded = false
                onExtraMenuClick()
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun MiniPlayerBar(
  playerState: PlayerUiState,
  animationsEnabled: Boolean,
  onOpenFullPlayer: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val track = playerState.currentTrack ?: return
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  val progress = if (playerState.durationMs > 0L) {
    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  val infiniteTransition = rememberInfiniteTransition(label = "mini_glow")
  val glowAlpha by if (animationsEnabled && playerState.isPlaying) {
    infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 0.9f,
      animationSpec = infiniteRepeatable(
        animation = tween(1100, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "mini_glow_alpha",
    )
  } else {
    remember { mutableStateOf(0.45f) }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onOpenFullPlayer)
      .testTag("mini_player_bar"),
    shape = RoundedCornerShape(18.dp),
    color = Color(0xE60B142C),
    border = BorderStroke(
      1.dp,
      Brush.horizontalGradient(listOf(primary.copy(alpha = glowAlpha), secondary.copy(alpha = 0.65f))),
    ),
    shadowElevation = 8.dp,
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TrackArtwork(
          artUri = track.albumArtUri,
          contentDescription = "Capa atual",
          size = 44.dp,
          cornerRadius = 10.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = track.displayArtist,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.16f))
            .testTag("mini_play_pause_button"),
        ) {
          Icon(
            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playerState.isPlaying) "Pausar" else "Reproduzir",
            tint = primary,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
          onClick = onNext,
          modifier = Modifier.size(48.dp).testTag("mini_next_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Próxima faixa",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = primary,
        trackColor = Color(0x33FFFFFF),
      )
    }
  }
}

@Composable
fun AddToPlaylistDialog(
  track: Track,
  playlists: List<PlaylistUi>,
  onDismiss: () -> Unit,
  onSelectPlaylist: (Long) -> Unit,
  onCreateAndAdd: (String) -> Unit,
) {
  var showCreateField by remember { mutableStateOf(false) }
  var newName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Adicionar à playlist",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (showCreateField) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Nome da nova playlist") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          OutlinedButton(
            onClick = { showCreateField = true },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar nova playlist")
          }
        }

        if (playlists.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier.height((playlists.size * 56).coerceAtMost(220).dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(playlists, key = { it.id }) { playlist ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onSelectPlaylist(playlist.id)
                    onDismiss()
                  },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "${playlist.tracks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (showCreateField) {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              onCreateAndAdd(newName.trim())
              onDismiss()
            }
          },
        ) {
          Text("Criar e Adicionar")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    },
  )
}

@Composable
fun EmptyLibraryCard(
  title: String,
  message: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  GlassCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.MusicNote,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(30.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onAction,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Text(actionLabel, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
