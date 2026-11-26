package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import com.example.player.PlayerUiState
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.TrackRow
import com.example.ui.theme.TextSecondary

@Composable
fun FavoritesScreen(
  favoriteTracks: List<Track>,
  favoriteUris: Set<String>,
  playerState: PlayerUiState,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = tertiary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text = "${favoriteTracks.size} músicas favoritadas",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
    }

    if (favoriteTracks.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Sem favoritos ainda",
          message = "Toque no ícone de coração em qualquer música da biblioteca ou do player para salvá-la aqui.",
        )
      }
    } else {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = { onPlayAll(favoriteTracks, false) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("favorites_play_all"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reproduzir")
          }
          OutlinedButton(
            onClick = { onPlayAll(favoriteTracks, true) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("favorites_shuffle_all"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
          ) {
            Icon(
              Icons.Filled.Shuffle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aleatório", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      itemsIndexed(favoriteTracks, key = { _, t -> "fav_${t.uri}" }) { index, track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = favoriteUris.contains(track.uri),
          onClick = { onPlayTrack(favoriteTracks, index) },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
        )
      }
    }
  }
}
