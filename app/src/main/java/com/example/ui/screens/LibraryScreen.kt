package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.FolderGroup
import com.example.data.model.GenreGroup
import com.example.data.model.LibraryTab
import com.example.data.model.PlaylistUi
import com.example.data.model.SortOrder
import com.example.data.model.Track
import com.example.player.PlayerUiState
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.GlassCard
import com.example.ui.components.PlaylistCoverImage
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRow
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

sealed interface LibraryDetailTarget {
  data class Playlist(val playlistId: Long) : LibraryDetailTarget
  data class Folder(val folderPath: String) : LibraryDetailTarget
  data class Album(val albumId: Long, val albumName: String) : LibraryDetailTarget
  data class Artist(val artistName: String) : LibraryDetailTarget
  data class Genre(val genreName: String) : LibraryDetailTarget
}

@Composable
fun LibraryScreen(
  selectedTab: LibraryTab,
  onTabSelected: (LibraryTab) -> Unit,
  hasPermission: Boolean,
  allTracks: List<Track>,
  filteredTracks: List<Track>,
  searchQuery: String,
  sortOrder: SortOrder,
  recentTracks: List<Track>,
  favoriteTracks: List<Track>,
  favoriteUris: Set<String>,
  playlists: List<PlaylistUi>,
  folders: List<FolderGroup>,
  albums: List<AlbumGroup>,
  artists: List<ArtistGroup>,
  genres: List<GenreGroup>,
  playerState: PlayerUiState,
  onRequestPermission: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onCreatePlaylist: (String, Uri?) -> Unit,
  onRenamePlaylist: (Long, String) -> Unit,
  onUpdatePlaylistCover: (Long, String?, Uri?) -> Unit,
  onDeletePlaylist: (PlaylistUi) -> Unit,
  onOpenDetail: (LibraryDetailTarget) -> Unit,
) {
  val tabs = LibraryTab.entries
  val pagerState = rememberPagerState(initialPage = selectedTab.ordinal, pageCount = { tabs.size })
  val coroutineScope = rememberCoroutineScope()
  val primary = MaterialTheme.colorScheme.primary

  LaunchedEffect(selectedTab) {
    if (pagerState.currentPage != selectedTab.ordinal) {
      pagerState.animateScrollToPage(selectedTab.ordinal)
    }
  }

  LaunchedEffect(pagerState.currentPage) {
    val tab = tabs[pagerState.currentPage]
    if (tab != selectedTab) {
      onTabSelected(tab)
    }
  }

  var showCreatePlaylistDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Biblioteca",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "${allTracks.size} faixas",
        style = MaterialTheme.typography.labelLarge,
        color = primary,
      )
    }

    // Horizontal swipeable tab bar
    ScrollableTabRow(
      selectedTabIndex = pagerState.currentPage,
      containerColor = Color.Transparent,
      contentColor = primary,
      edgePadding = 12.dp,
      divider = {},
      indicator = { tabPositions ->
        if (pagerState.currentPage < tabPositions.size) {
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
            height = 3.dp,
            color = primary,
          )
        }
      },
    ) {
      tabs.forEachIndexed { index, tab ->
        val selected = pagerState.currentPage == index
        Tab(
          selected = selected,
          onClick = {
            coroutineScope.launch { pagerState.animateScrollToPage(index) }
          },
          modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}"),
          text = {
            Text(
              text = tab.title,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) primary else TextSecondary,
            )
          },
        )
      }
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
    ) { page ->
      when (tabs[page]) {
        LibraryTab.FOR_YOU -> ForYouTabContent(
          hasPermission = hasPermission,
          allTracks = allTracks,
          recentTracks = recentTracks,
          favoriteTracks = favoriteTracks,
          favoriteUris = favoriteUris,
          playlists = playlists,
          playerState = playerState,
          onRequestPermission = onRequestPermission,
          onPlayAll = onPlayAll,
          onPlayTrack = onPlayTrack,
          onToggleFavorite = onToggleFavorite,
          onAddToPlaylist = onAddToPlaylist,
          onJumpToTab = { target ->
            coroutineScope.launch { pagerState.animateScrollToPage(target.ordinal) }
          },
          onOpenPlaylist = { onOpenDetail(LibraryDetailTarget.Playlist(it.id)) },
        )

        LibraryTab.TRACKS -> TracksTabContent(
          hasPermission = hasPermission,
          tracks = filteredTracks,
          searchQuery = searchQuery,
          sortOrder = sortOrder,
          favoriteUris = favoriteUris,
          playerState = playerState,
          onRequestPermission = onRequestPermission,
          onSearchQueryChange = onSearchQueryChange,
          onSortOrderChange = onSortOrderChange,
          onPlayAll = onPlayAll,
          onPlayTrack = onPlayTrack,
          onToggleFavorite = onToggleFavorite,
          onAddToPlaylist = onAddToPlaylist,
        )

        LibraryTab.PLAYLISTS -> PlaylistsTabContent(
          playlists = playlists,
          onCreateClick = { showCreatePlaylistDialog = true },
          onOpenPlaylist = { onOpenDetail(LibraryDetailTarget.Playlist(it.id)) },
          onPlayAll = onPlayAll,
          onRenamePlaylist = onRenamePlaylist,
          onUpdatePlaylistCover = onUpdatePlaylistCover,
          onDeletePlaylist = onDeletePlaylist,
        )

        LibraryTab.FOLDERS -> FoldersTabContent(
          folders = folders,
          onOpenFolder = { onOpenDetail(LibraryDetailTarget.Folder(it.folderPath)) },
        )

        LibraryTab.ALBUMS -> AlbumsTabContent(
          albums = albums,
          onOpenAlbum = { onOpenDetail(LibraryDetailTarget.Album(it.albumId, it.name)) },
        )

        LibraryTab.ARTISTS -> ArtistsTabContent(
          artists = artists,
          onOpenArtist = { onOpenDetail(LibraryDetailTarget.Artist(it.name)) },
        )

        LibraryTab.GENRES -> GenresTabContent(
          genres = genres,
          onOpenGenre = { onOpenDetail(LibraryDetailTarget.Genre(it.name)) },
        )
      }
    }
  }

  if (showCreatePlaylistDialog) {
    CreatePlaylistDialog(
      onDismiss = { showCreatePlaylistDialog = false },
      onCreate = { name, uri ->
        onCreatePlaylist(name, uri)
        showCreatePlaylistDialog = false
      },
    )
  }
}

@Composable
private fun ForYouTabContent(
  hasPermission: Boolean,
  allTracks: List<Track>,
  recentTracks: List<Track>,
  favoriteTracks: List<Track>,
  favoriteUris: Set<String>,
  playlists: List<PlaylistUi>,
  playerState: PlayerUiState,
  onRequestPermission: () -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onJumpToTab: (LibraryTab) -> Unit,
  onOpenPlaylist: (PlaylistUi) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (!hasPermission) {
      item {
        EmptyLibraryCard(
          title = "Permissão necessária",
          message = "Autorize a leitura de áudio para explorar suas músicas.",
          actionLabel = "Conceder Permissão",
          onAction = onRequestPermission,
        )
      }
    } else if (allTracks.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = { onPlayAll(allTracks, false) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tocar Todas")
          }
          OutlinedButton(
            onClick = { onPlayAll(allTracks, true) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, secondary.copy(alpha = 0.7f)),
          ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, tint = secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aleatório", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }

    // Category shortcuts
    item {
      Text("Atalhos rápidos", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val shortcuts = listOf(
          Triple(LibraryTab.TRACKS, Icons.Filled.MusicNote, "Faixas"),
          Triple(LibraryTab.PLAYLISTS, Icons.Filled.QueueMusic, "Playlists"),
          Triple(LibraryTab.FOLDERS, Icons.Filled.Folder, "Pastas"),
          Triple(LibraryTab.ALBUMS, Icons.Filled.Album, "Álbuns"),
          Triple(LibraryTab.ARTISTS, Icons.Filled.Person, "Artistas"),
          Triple(LibraryTab.GENRES, Icons.Filled.Category, "Gêneros"),
        )
        items(shortcuts) { (tab, icon, label) ->
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable { onJumpToTab(tab) },
            shape = RoundedCornerShape(14.dp),
            color = Color(0x990C1530),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(label, style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    // Recent playlists
    if (playlists.isNotEmpty()) {
      item {
        Text("Playlists recentes", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(playlists.take(8), key = { it.id }) { playlist ->
            GlassCard(
              onClick = { onOpenPlaylist(playlist) },
              modifier = Modifier.width(146.dp),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                PlaylistCoverImage(playlist = playlist, size = 126.dp, cornerRadius = 14.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = playlist.name,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${playlist.tracks.size} faixas",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
            }
          }
        }
      }
    }

    // Recent tracks
    if (recentTracks.isNotEmpty()) {
      item { Text("Músicas recentes", style = MaterialTheme.typography.titleLarge) }
      itemsIndexed(recentTracks.take(6), key = { _, t -> "fy_rec_${t.uri}" }) { index, track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = favoriteUris.contains(track.uri),
          onClick = { onPlayTrack(recentTracks, index) },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
        )
      }
    }

    // Favorites
    if (favoriteTracks.isNotEmpty()) {
      item { Text("Favoritos", style = MaterialTheme.typography.titleLarge) }
      itemsIndexed(favoriteTracks.take(6), key = { _, t -> "fy_fav_${t.uri}" }) { index, track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = true,
          onClick = { onPlayTrack(favoriteTracks, index) },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
        )
      }
    }
  }
}

@Composable
private fun TracksTabContent(
  hasPermission: Boolean,
  tracks: List<Track>,
  searchQuery: String,
  sortOrder: SortOrder,
  favoriteUris: Set<String>,
  playerState: PlayerUiState,
  onRequestPermission: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onPlayTrack: (List<Track>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Buscar faixa, artista ou álbum...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("tracks_search_input"),
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SortOrder.entries) { option ->
          FilterChip(
            selected = sortOrder == option,
            onClick = { onSortOrderChange(option) },
            label = { Text(option.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = primary.copy(alpha = 0.22f),
              selectedLabelColor = primary,
            ),
          )
        }
      }
    }

    if (tracks.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onPlayAll(tracks, false) },
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reproduzir Todas")
          }
          OutlinedButton(
            onClick = { onPlayAll(tracks, true) },
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aleatório")
          }
        }
      }
    }

    if (!hasPermission) {
      item {
        EmptyLibraryCard(
          title = "Permissão de áudio pendente",
          message = "Conceda permissão para carregar as músicas do armazenamento.",
          actionLabel = "Conceder",
          onAction = onRequestPermission,
        )
      }
    } else if (tracks.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Nenhuma faixa encontrada",
          message = if (searchQuery.isNotBlank()) "Nenhuma música corresponde à busca \"$searchQuery\"." else "Nenhum arquivo de áudio visível na biblioteca.",
        )
      }
    } else {
      itemsIndexed(tracks, key = { _, t -> t.uri }) { index, track ->
        TrackRow(
          track = track,
          isCurrentTrack = playerState.currentTrack?.uri == track.uri,
          isPlaying = playerState.isPlaying,
          isFavorite = favoriteUris.contains(track.uri),
          onClick = { onPlayTrack(tracks, index) },
          onToggleFavorite = { onToggleFavorite(track.uri) },
          onAddToPlaylist = { onAddToPlaylist(track) },
        )
      }
    }
  }
}

@Composable
private fun PlaylistsTabContent(
  playlists: List<PlaylistUi>,
  onCreateClick: () -> Unit,
  onOpenPlaylist: (PlaylistUi) -> Unit,
  onPlayAll: (List<Track>, Boolean) -> Unit,
  onRenamePlaylist: (Long, String) -> Unit,
  onUpdatePlaylistCover: (Long, String?, Uri?) -> Unit,
  onDeletePlaylist: (PlaylistUi) -> Unit,
) {
  var renameTarget by remember { mutableStateOf<PlaylistUi?>(null) }
  var coverTarget by remember { mutableStateOf<PlaylistUi?>(null) }

  val coverPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    val target = coverTarget
    if (uri != null && target != null) {
      onUpdatePlaylistCover(target.id, target.coverPath, uri)
    }
    coverTarget = null
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Button(
        onClick = onCreateClick,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_playlist_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Criar Nova Playlist")
      }
    }

    if (playlists.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Nenhuma playlist criada",
          message = "Crie playlists personalizadas com capa própria e salve suas seleções localmente.",
        )
      }
    } else {
      items(playlists, key = { it.id }) { playlist ->
        var menuOpen by remember { mutableStateOf(false) }
        GlassCard(
          onClick = { onOpenPlaylist(playlist) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PlaylistCoverImage(playlist = playlist, size = 60.dp, cornerRadius = 14.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${playlist.tracks.size} músicas",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
            if (playlist.tracks.isNotEmpty()) {
              IconButton(onClick = { onPlayAll(playlist.tracks, false) }) {
                Icon(
                  Icons.Filled.PlayArrow,
                  contentDescription = "Reproduzir playlist",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Box {
              IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Gerenciar playlist")
              }
              DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                  text = { Text("Editar nome") },
                  leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    renameTarget = playlist
                  },
                )
                DropdownMenuItem(
                  text = { Text("Personalizar capa") },
                  leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    coverTarget = playlist
                    coverPicker.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                )
                if (playlist.tracks.isNotEmpty()) {
                  DropdownMenuItem(
                    text = { Text("Reproduzir em ordem aleatória") },
                    leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                    onClick = {
                      menuOpen = false
                      onPlayAll(playlist.tracks, true)
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text("Excluir playlist") },
                  leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                  onClick = {
                    menuOpen = false
                    onDeletePlaylist(playlist)
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  renameTarget?.let { target ->
    var name by remember(target.id) { mutableStateOf(target.name) }
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      title = { Text("Editar nome da playlist") },
      text = {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onRenamePlaylist(target.id, name)
              renameTarget = null
            }
          },
        ) {
          Text("Salvar")
        }
      },
      dismissButton = {
        TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
      },
    )
  }
}

@Composable
private fun FoldersTabContent(
  folders: List<FolderGroup>,
  onOpenFolder: (FolderGroup) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (folders.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Nenhuma pasta visível",
          message = "As pastas com arquivos de áudio do dispositivo aparecerão aqui.",
        )
      }
    } else {
      items(folders, key = { it.folderPath }) { folder ->
        GlassCard(onClick = { onOpenFolder(folder) }, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(folder.folderName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(folder.folderPath, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${folder.tracks.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
private fun AlbumsTabContent(
  albums: List<AlbumGroup>,
  onOpenAlbum: (AlbumGroup) -> Unit,
) {
  if (albums.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      EmptyLibraryCard(
        title = "Nenhum álbum encontrado",
        message = "Os álbuns extraídos dos metadados das suas músicas aparecerão aqui.",
      )
    }
    return
  }

  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 156.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(albums, key = { "${it.albumId}_${it.name}" }) { album ->
      GlassCard(onClick = { onOpenAlbum(album) }) {
        Column(modifier = Modifier.padding(10.dp)) {
          TrackArtwork(
            artUri = album.artUri,
            contentDescription = album.name,
            size = 140.dp,
            cornerRadius = 14.dp,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            text = "${album.artist} • ${album.tracks.size} faixas",
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

@Composable
private fun ArtistsTabContent(
  artists: List<ArtistGroup>,
  onOpenArtist: (ArtistGroup) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (artists.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Nenhum artista encontrado",
          message = "Os artistas dos seus arquivos de áudio aparecerão agrupados aqui.",
        )
      }
    } else {
      items(artists, key = { it.name }) { artist ->
        GlassCard(onClick = { onOpenArtist(artist) }, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                text = "${artist.tracks.size} músicas • ${artist.albumCount} álbuns",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GenresTabContent(
  genres: List<GenreGroup>,
  onOpenGenre: (GenreGroup) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (genres.isEmpty()) {
      item {
        EmptyLibraryCard(
          title = "Nenhum gênero disponível",
          message = "Os gêneros musicais aparecerão quando estiverem presentes nos metadados dos arquivos.",
        )
      }
    } else {
      items(genres, key = { it.name }) { genre ->
        GlassCard(onClick = { onOpenGenre(genre) }, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Filled.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(genre.name, style = MaterialTheme.typography.titleMedium)
              Text(
                text = if (genre.isUnknown) "${genre.tracks.size} faixas sem tag de gênero" else "${genre.tracks.size} faixas",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onCreate: (String, Uri?) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var pickedUri by remember { mutableStateOf<Uri?>(null) }

  val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri -> pickedUri = uri }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Nova Playlist") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome da playlist") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new_playlist_name_input"),
        )
        OutlinedButton(
          onClick = {
            imagePicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Image, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (pickedUri != null) "Capa selecionada ✓" else "Escolher capa (opcional)")
        }
        pickedUri?.let { uri ->
          AsyncImage(
            model = uri,
            contentDescription = "Pré-visualização da capa",
            modifier = Modifier
              .size(96.dp)
              .clip(RoundedCornerShape(12.dp))
              .align(Alignment.CenterHorizontally),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name.ifBlank { "Minha Playlist" }, pickedUri) },
        modifier = Modifier.testTag("confirm_create_playlist"),
      ) {
        Text("Criar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}
