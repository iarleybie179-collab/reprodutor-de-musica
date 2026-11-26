package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LibraryTab
import com.example.data.model.PlaylistUi
import com.example.data.model.Track
import com.example.player.PlayerUiState
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.GlassCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRow
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
  hasPermission: Boolean,
  isScanning: Boolean,
  tracks: List<Track>,
  albumCount: Int,
  artistCount: Int,
  playlists: List<PlaylistUi>,
  recentTracks: List<Track>,
  favoriteTracks: List<Track>,
  favoriteUris: Set<String>,
  playerState: PlayerUiState,
  onRequestPermission: () -> Unit,
  onRefreshLibrary: () -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onOpenFullPlayer: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onOpenLibraryTab: (LibraryTab) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Top Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Bolt,
              contentDescription = null,
              tint = primary,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "DBZ KI PLAYER",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = if (hasPermission) "${tracks.size} músicas no dispositivo" else "Armazenamento aguardando permissão",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
        IconButton(
          onClick = onRefreshLibrary,
          enabled = hasPermission && !isScanning,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.14f))
            .testTag("home_refresh_button"),
        ) {
          if (isScanning) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = primary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Atualizar biblioteca",
              tint = primary,
            )
          }
        }
      }
    }

    // Permission / Empty check
    if (!hasPermission) {
      item {
        EmptyLibraryCard(
          title = "Acesso às músicas",
          message = "Permita o acesso aos arquivos de áudio do aparelho para listar suas faixas, álbuns, artistas e pastas reais.",
          actionLabel = "Conceder Permissão",
          onAction = onRequestPermission,
        )
      }
    } else if (tracks.isEmpty() && !isScanning) {
      item {
        EmptyLibraryCard(
          title = "Nenhuma música encontrada",
          message = "Não encontramos arquivos de áudio no armazenamento do dispositivo. Adicione arquivos MP3, FLAC, WAV ou M4A e toque em Reexaminar.",
          actionLabel = "Reexaminar Músicas",
          onAction = onRefreshLibrary,
        )
      }
    }

    // Now Playing / Resume Hero Card
    playerState.currentTrack?.let { current ->
      item {
        GlassCard(
          borderColor = primary.copy(alpha = 0.65f),
          onClick = onOpenFullPlayer,
          modifier = Modifier.fillMaxWidth().testTag("home_now_playing_card"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(primary.copy(alpha = 0.20f), secondary.copy(alpha = 0.14f), Color.Transparent)
                )
              )
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TrackArtwork(
              artUri = current.albumArtUri,
              contentDescription = "Capa em reprodução",
              size = 66.dp,
              cornerRadius = 16.dp,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (playerState.isPlaying) "TOCANDO AGORA" else "RETOMAR REPRODUÇÃO",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = current.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = current.displayArtist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = onTogglePlayPause,
              modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(primary),
            ) {
              Icon(
                imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playerState.isPlaying) "Pausar" else "Reproduzir",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }
    }

    // Quick Play / Shuffle actions
    if (tracks.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = { onPlayAll(tracks, false) },
            modifier = Modifier.weight(1f).height(50.dp).testTag("home_play_all_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reproduzir Tudo", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          OutlinedButton(
            onClick = { onPlayAll(tracks, true) },
            modifier = Modifier.weight(1f).height(50.dp).testTag("home_shuffle_all_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, secondary.copy(alpha = 0.7f)),
          ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, tint = secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aleatório", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }

    // Category Quick Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        HomeStatChip(
          icon = Icons.Filled.MusicNote,
          label = "Faixas",
          value = "${tracks.size}",
          accent = primary,
          onClick = { onOpenLibraryTab(LibraryTab.TRACKS) },
          modifier = Modifier.weight(1f),
        )
        HomeStatChip(
          icon = Icons.Filled.QueueMusic,
          label = "Playlists",
          value = "${playlists.size}",
          accent = secondary,
          onClick = { onOpenLibraryTab(LibraryTab.PLAYLISTS) },
          modifier = Modifier.weight(1f),
        )
        HomeStatChip(
          icon = Icons.Filled.Album,
          label = "Álbuns",
          value = "$albumCount",
          accent = MaterialTheme.colorScheme.tertiary,
          onClick = { onOpenLibraryTab(LibraryTab.ALBUMS) },
          modifier = Modifier.weight(1f),
        )
        HomeStatChip(
          icon = Icons.Filled.Person,
          label = "Artistas",
          value = "$artistCount",
          accent = primary,
          onClick = { onOpenLibraryTab(LibraryTab.ARTISTS) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Recent Tracks
    if (recentTracks.isNotEmpty()) {
      item {
        Text(
          text = "Tocadas Recentemente",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(recentTracks.take(12), key = { it.uri }) { track ->
            GlassCard(
              onClick = {
                val idx = recentTracks.indexOf(track).coerceAtLeast(0)
                onPlayTrack(recentTracks, idx)
              },
              modifier = Modifier.width(142.dp),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                TrackArtwork(
                  artUri = track.albumArtUri,
                  contentDescription = track.title,
                  size = 122.dp,
                  cornerRadius = 14.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
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
            }
          }
        }
      }
    }

    // Favorite highlights
    if (favoriteTracks.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Suas Favoritas",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
      items(favoriteTracks.take(5), key = { "home_fav_${it.uri}" }) { track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = favoriteUris.contains(track.uri),
          onClick = {
            val idx = favoriteTracks.indexOf(track).coerceAtLeast(0)
            onPlayTrack(favoriteTracks, idx)
          },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
        )
      }
    }
  }
}

@Composable
private fun HomeStatChip(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = Color(0x990C1530),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
