package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.FolderGroup
import com.example.data.model.GenreGroup
import com.example.data.model.PlaylistUi
import com.example.data.model.Track
import com.example.player.PlayerUiState
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.PlaylistCoverImage
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRow
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryDetailOverlay(
  target: LibraryDetailTarget,
  allTracks: List<Track>,
  playlists: List<PlaylistUi>,
  folders: List<FolderGroup>,
  albums: List<AlbumGroup>,
  artists: List<ArtistGroup>,
  genres: List<GenreGroup>,
  favoriteUris: Set<String>,
  playerState: PlayerUiState,
  onBack: () -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onAddMultipleTracksToPlaylist: (Long, List<String>) -> Unit,
  onRemoveTrackFromPlaylist: (Long, String) -> Unit,
  onUpdatePlaylistCover: (Long, String?, Uri?) -> Unit,
) {
  BackHandler(onBack = onBack)

  val playlist = (target as? LibraryDetailTarget.Playlist)?.let { t ->
    playlists.firstOrNull { it.id == t.playlistId }
  }
  val folder = (target as? LibraryDetailTarget.Folder)?.let { t ->
    folders.firstOrNull { it.folderPath == t.folderPath }
  }
  val album = (target as? LibraryDetailTarget.Album)?.let { t ->
    albums.firstOrNull { it.albumId == t.albumId && it.name == t.albumName }
  }
  val artist = (target as? LibraryDetailTarget.Artist)?.let { t ->
    artists.firstOrNull { it.name == t.artistName }
  }
  val genre = (target as? LibraryDetailTarget.Genre)?.let { t ->
    genres.firstOrNull { it.name == t.genreName }
  }

  val title = when (target) {
    is LibraryDetailTarget.Playlist -> playlist?.name ?: "Playlist"
    is LibraryDetailTarget.Folder -> folder?.folderName ?: "Pasta"
    is LibraryDetailTarget.Album -> album?.name ?: target.albumName
    is LibraryDetailTarget.Artist -> artist?.name ?: target.artistName
    is LibraryDetailTarget.Genre -> genre?.name ?: target.genreName
  }

  val subtitle = when (target) {
    is LibraryDetailTarget.Playlist -> "${playlist?.tracks?.size ?: 0} faixas na playlist"
    is LibraryDetailTarget.Folder -> folder?.folderPath ?: ""
    is LibraryDetailTarget.Album -> "${album?.artist ?: ""} • ${album?.tracks?.size ?: 0} faixas"
    is LibraryDetailTarget.Artist -> "${artist?.tracks?.size ?: 0} faixas • ${artist?.albumCount ?: 0} álbuns"
    is LibraryDetailTarget.Genre -> "${genre?.tracks?.size ?: 0} faixas"
  }

  val detailTracks: List<Track> = when (target) {
    is LibraryDetailTarget.Playlist -> playlist?.tracks.orEmpty()
    is LibraryDetailTarget.Folder -> folder?.tracks.orEmpty()
    is LibraryDetailTarget.Album -> album?.tracks.orEmpty()
    is LibraryDetailTarget.Artist -> artist?.tracks.orEmpty()
    is LibraryDetailTarget.Genre -> genre?.tracks.orEmpty()
  }

  var showAddSongsDialog by remember { mutableStateOf(false) }

  val coverPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null && playlist != null) {
      onUpdatePlaylistCover(playlist.id, playlist.coverPath, uri)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD9050814)),
  ) {
    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Voltar",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (playlist != null) {
        IconButton(
          onClick = {
            coverPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(Icons.Filled.Image, contentDescription = "Alterar capa", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(
          onClick = { showAddSongsDialog = true },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Adicionar músicas", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Hero artwork for playlist or album
      if (playlist != null || album != null) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (playlist != null) {
              PlaylistCoverImage(playlist = playlist, size = 88.dp, cornerRadius = 18.dp)
            } else if (album != null) {
              TrackArtwork(artUri = album.artUri, contentDescription = album.name, size = 88.dp, cornerRadius = 18.dp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
              if (playlist != null) {
                OutlinedButton(
                  onClick = { showAddSongsDialog = true },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Adicionar músicas")
                }
              }
            }
          }
        }
      }

      if (detailTracks.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Button(
              onClick = { onPlayAll(detailTracks, false) },
              modifier = Modifier.weight(1f).height(48.dp),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reproduzir")
            }
            OutlinedButton(
              onClick = { onPlayAll(detailTracks, true) },
              modifier = Modifier.weight(1f).height(48.dp),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
            ) {
              Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Aleatório", color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }
      } else {
        item {
          EmptyLibraryCard(
            title = "Lista vazia",
            message = if (playlist != null) "Toque em Adicionar músicas para montar sua playlist." else "Nenhuma música nesta categoria.",
          )
        }
      }

      itemsIndexed(detailTracks, key = { idx, t -> "${idx}_${t.uri}" }) { index, track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = favoriteUris.contains(track.uri),
          onClick = { onPlayTrack(detailTracks, index) },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
          extraMenuLabel = if (playlist != null) "Remover desta playlist" else null,
          onExtraMenuClick = if (playlist != null) {
            { onRemoveTrackFromPlaylist(playlist.id, track.uri) }
          } else {
            null
          },
        )
      }
    }
  }

  if (showAddSongsDialog && playlist != null) {
    val existingUris = playlist.tracks.map { it.uri }.toSet()
    val candidates = allTracks.filterNot { existingUris.contains(it.uri) }
    val selectedUris = remember(candidates) { mutableStateListOf<String>() }

    AlertDialog(
      onDismissRequest = { showAddSongsDialog = false },
      title = { Text("Adicionar à ${playlist.name}") },
      text = {
        if (candidates.isEmpty()) {
          Text("Todas as músicas disponíveis já estão nesta playlist.")
        } else {
          LazyColumn(
            modifier = Modifier.height(320.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(candidates, key = { it.uri }) { track ->
              val checked = selectedUris.contains(track.uri)
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    if (checked) selectedUris.remove(track.uri) else selectedUris.add(track.uri)
                  },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = checked,
                    onCheckedChange = {
                      if (it) selectedUris.add(track.uri) else selectedUris.remove(track.uri)
                    },
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.displayArtist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (selectedUris.isNotEmpty()) {
              onAddMultipleTracksToPlaylist(playlist.id, selectedUris.toList())
            }
            showAddSongsDialog = false
          },
        ) {
          Text("Adicionar (${selectedUris.size})")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddSongsDialog = false }) { Text("Cancelar") }
      },
    )
  }
}
