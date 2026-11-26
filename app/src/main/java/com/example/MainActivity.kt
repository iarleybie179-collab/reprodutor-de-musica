package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LibraryTab
import com.example.data.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.DbzBackdrop
import com.example.ui.components.IntroOverlay
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FullPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryDetailOverlay
import com.example.ui.screens.LibraryDetailTarget
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DbzMusicTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

enum class MainNavDestination(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  HOME("Início", Icons.Filled.Home, Icons.Outlined.Home),
  LIBRARY("Biblioteca", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
  FAVORITES("Favoritos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
  SETTINGS("Configurações", Icons.Filled.Settings, Icons.Outlined.Settings),
}

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      DbzMusicTheme(accentPreset = settings.accentPreset) {
        DbzPlayerAppRoot(viewModel = viewModel)
      }
    }
  }
}

@Composable
private fun DbzPlayerAppRoot(viewModel: MainViewModel) {
  val context = LocalContext.current

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val hasPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val visibleTracks by viewModel.visibleTracks.collectAsStateWithLifecycle()
  val filteredTracks by viewModel.filteredSearchTracks.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
  val albums by viewModel.albums.collectAsStateWithLifecycle()
  val artists by viewModel.artists.collectAsStateWithLifecycle()
  val folders by viewModel.folders.collectAsStateWithLifecycle()
  val allFolders by viewModel.allFolderPaths.collectAsStateWithLifecycle()
  val genres by viewModel.genres.collectAsStateWithLifecycle()
  val playlists by viewModel.playlists.collectAsStateWithLifecycle()
  val favoriteUris by viewModel.favoriteUris.collectAsStateWithLifecycle()
  val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
  val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle()
  val playerState by viewModel.playerState.collectAsStateWithLifecycle()
  val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
  val showIntro by viewModel.showIntro.collectAsStateWithLifecycle()

  val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
  } else {
    Manifest.permission.READ_EXTERNAL_STORAGE
  }

  val permissionsToRequest = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { results ->
    val audioGranted = results[audioPermission] == true ||
      ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
    viewModel.onPermissionResult(audioGranted)
  }

  LaunchedEffect(Unit) {
    val granted = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
    if (granted) {
      viewModel.onPermissionResult(true)
    } else {
      permissionLauncher.launch(permissionsToRequest)
    }
  }

  var currentDestination by rememberSaveable { mutableStateOf(MainNavDestination.HOME) }
  var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.FOR_YOU) }
  var detailTarget by remember { mutableStateOf<LibraryDetailTarget?>(null) }
  var showFullPlayer by remember { mutableStateOf(false) }
  var trackPendingPlaylist by remember { mutableStateOf<Track?>(null) }

  // Back button on non-home tabs navigates back to Home
  BackHandler(enabled = currentDestination != MainNavDestination.HOME && detailTarget == null && !showFullPlayer) {
    currentDestination = MainNavDestination.HOME
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Persistent customizable DBZ background
    DbzBackdrop(settings = settings)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 700.dp

      Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          if (!useRail && !showFullPlayer) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            ) {
              if (playerState.currentTrack != null) {
                MiniPlayerBar(
                  playerState = playerState,
                  animationsEnabled = settings.effectiveAnimations,
                  onOpenFullPlayer = { showFullPlayer = true },
                  onTogglePlayPause = viewModel::togglePlayPause,
                  onNext = viewModel::nextTrack,
                )
              }
              DbzBottomNavigationBar(
                current = currentDestination,
                onSelect = { dest ->
                  detailTarget = null
                  currentDestination = dest
                },
              )
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding(),
        ) {
          if (useRail && !showFullPlayer) {
            DbzNavigationRail(
              current = currentDestination,
              onSelect = { dest ->
                detailTarget = null
                currentDestination = dest
              },
            )
          }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (currentDestination) {
              MainNavDestination.HOME -> HomeScreen(
                hasPermission = hasPermission,
                isScanning = isScanning,
                tracks = visibleTracks,
                albumCount = albums.size,
                artistCount = artists.size,
                playlists = playlists,
                recentTracks = recentTracks,
                favoriteTracks = favoriteTracks,
                favoriteUris = favoriteUris,
                playerState = playerState,
                onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                onRefreshLibrary = viewModel::refreshLibrary,
                onPlayAll = viewModel::playAll,
                onPlayTrack = viewModel::playTrackFromList,
                onTogglePlayPause = viewModel::togglePlayPause,
                onOpenFullPlayer = { showFullPlayer = true },
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { trackPendingPlaylist = it },
                onOpenLibraryTab = { tab ->
                  selectedLibraryTab = tab
                  detailTarget = null
                  currentDestination = MainNavDestination.LIBRARY
                },
              )

              MainNavDestination.LIBRARY -> LibraryScreen(
                selectedTab = selectedLibraryTab,
                onTabSelected = { selectedLibraryTab = it },
                hasPermission = hasPermission,
                allTracks = visibleTracks,
                filteredTracks = filteredTracks,
                searchQuery = searchQuery,
                sortOrder = sortOrder,
                recentTracks = recentTracks,
                favoriteTracks = favoriteTracks,
                favoriteUris = favoriteUris,
                playlists = playlists,
                folders = folders,
                albums = albums,
                artists = artists,
                genres = genres,
                playerState = playerState,
                onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSortOrderChange = viewModel::setSortOrder,
                onPlayAll = viewModel::playAll,
                onPlayTrack = viewModel::playTrackFromList,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { trackPendingPlaylist = it },
                onCreatePlaylist = viewModel::createPlaylist,
                onRenamePlaylist = viewModel::renamePlaylist,
                onUpdatePlaylistCover = viewModel::updatePlaylistCover,
                onDeletePlaylist = viewModel::deletePlaylist,
                onOpenDetail = { detailTarget = it },
              )

              MainNavDestination.FAVORITES -> FavoritesScreen(
                favoriteTracks = favoriteTracks,
                favoriteUris = favoriteUris,
                playerState = playerState,
                onPlayAll = viewModel::playAll,
                onPlayTrack = viewModel::playTrackFromList,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { trackPendingPlaylist = it },
              )

              MainNavDestination.SETTINGS -> SettingsScreen(
                settings = settings,
                equalizerState = equalizerState,
                allFolders = allFolders,
                onPickWallpaper = viewModel::selectCustomWallpaper,
                onRestoreDefaultWallpaper = viewModel::restoreDefaultWallpaper,
                onDimChange = viewModel::setWallpaperDim,
                onBlurChange = viewModel::setWallpaperBlur,
                onAccentChange = viewModel::setAccentPreset,
                onVisualEffectChange = viewModel::setVisualEffect,
                onAnimationsChange = viewModel::setAnimationsEnabled,
                onParticlesChange = viewModel::setParticlesEnabled,
                onIntroEnabledChange = viewModel::setIntroEnabled,
                onIntroVoiceChange = viewModel::setIntroVoiceEnabled,
                onIntroSubtitleChange = viewModel::setIntroSubtitleEnabled,
                onIntroDurationChange = viewModel::setIntroDuration,
                onPreviewIntro = viewModel::replayIntro,
                onAutoplayChange = viewModel::setAutoplayEnabled,
                onShuffleChange = viewModel::setShuffleSetting,
                onRepeatModeChange = viewModel::setRepeatSetting,
                onResumePlaybackChange = viewModel::setResumeLastPlayback,
                onCrossfadeChange = viewModel::setCrossfadeEnabled,
                onBackgroundPlaybackChange = viewModel::setBackgroundPlaybackEnabled,
                onEqEnabledChange = viewModel::setEqEnabled,
                onEqPresetChange = viewModel::setEqPreset,
                onEqBandsChange = viewModel::setEqBands,
                onRefreshLibrary = viewModel::refreshLibrary,
                onDefaultSortChange = viewModel::setDefaultSortOrder,
                onToggleFolderHidden = viewModel::toggleFolderHidden,
                onHideShortClipsChange = viewModel::setHideShortClips,
                onBatterySaverChange = viewModel::setBatterySaverMode,
                onReduceAnimationsChange = viewModel::setReduceAnimations,
                onReduceEffectsChange = viewModel::setReduceEffects,
              )
            }

            // Library Detail Overlay (Playlist, Folder, Album, Artist, Genre)
            detailTarget?.let { target ->
              LibraryDetailOverlay(
                target = target,
                allTracks = visibleTracks,
                playlists = playlists,
                folders = folders,
                albums = albums,
                artists = artists,
                genres = genres,
                favoriteUris = favoriteUris,
                playerState = playerState,
                onBack = { detailTarget = null },
                onPlayAll = viewModel::playAll,
                onPlayTrack = viewModel::playTrackFromList,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { trackPendingPlaylist = it },
                onAddMultipleTracksToPlaylist = viewModel::addTracksToPlaylist,
                onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
                onUpdatePlaylistCover = viewModel::updatePlaylistCover,
              )
            }

            if (useRail && !showFullPlayer && playerState.currentTrack != null) {
              MiniPlayerBar(
                playerState = playerState,
                animationsEnabled = settings.effectiveAnimations,
                onOpenFullPlayer = { showFullPlayer = true },
                onTogglePlayPause = viewModel::togglePlayPause,
                onNext = viewModel::nextTrack,
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .navigationBarsPadding()
                  .padding(bottom = 8.dp),
              )
            }
          }
        }
      }
    }

    // Full Player Overlay
    AnimatedVisibility(
      visible = showFullPlayer,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
      val currentUri = playerState.currentTrack?.uri
      FullPlayerScreen(
        playerState = playerState,
        equalizerState = equalizerState,
        settings = settings,
        isFavorite = currentUri != null && favoriteUris.contains(currentUri),
        onClose = { showFullPlayer = false },
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::nextTrack,
        onPrevious = viewModel::previousTrack,
        onSeekTo = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onToggleFavorite = viewModel::toggleFavorite,
        onVolumeChange = viewModel::setVolume,
        onAddToPlaylist = { trackPendingPlaylist = it },
        onEqEnabledChange = viewModel::setEqEnabled,
        onEqPresetChange = viewModel::setEqPreset,
        onEqBandsChange = viewModel::setEqBands,
      )
    }

    // Add to Playlist Dialog
    trackPendingPlaylist?.let { track ->
      AddToPlaylistDialog(
        track = track,
        playlists = playlists,
        onDismiss = { trackPendingPlaylist = null },
        onSelectPlaylist = { playlistId ->
          viewModel.addTracksToPlaylist(playlistId, listOf(track.uri))
        },
        onCreateAndAdd = { name ->
          viewModel.createPlaylistAndAddTrack(name, track.uri)
        },
      )
    }

    // Cinematic DBZ Opening Intro
    if (showIntro == true) {
      IntroOverlay(
        durationSeconds = settings.introDurationSec,
        voiceEnabled = settings.introVoiceEnabled,
        subtitleEnabled = settings.introSubtitleEnabled,
        onFinished = viewModel::finishIntro,
      )
    }
  }
}

@Composable
private fun DbzBottomNavigationBar(
  current: MainNavDestination,
  onSelect: (MainNavDestination) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  NavigationBar(
    containerColor = Color(0xEC060B1D),
    tonalElevation = 0.dp,
    windowInsets = WindowInsets(0, 0, 0, 0),
    modifier = Modifier.border(
      width = 1.dp,
      color = primary.copy(alpha = 0.22f),
    ),
  ) {
    MainNavDestination.entries.forEach { dest ->
      val selected = dest == current
      NavigationBarItem(
        selected = selected,
        onClick = { onSelect(dest) },
        modifier = Modifier.testTag("nav_tab_${dest.name.lowercase()}"),
        icon = {
          Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label,
          )
        },
        label = { Text(dest.label, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = primary,
          selectedTextColor = primary,
          indicatorColor = primary.copy(alpha = 0.18f),
          unselectedIconColor = TextSecondary,
          unselectedTextColor = TextSecondary,
        ),
      )
    }
  }
}

@Composable
private fun DbzNavigationRail(
  current: MainNavDestination,
  onSelect: (MainNavDestination) -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  NavigationRail(
    containerColor = Color(0xDF060B1D),
    modifier = Modifier
      .fillMaxHeight()
      .navigationBarsPadding()
      .border(1.dp, primary.copy(alpha = 0.22f)),
  ) {
    MainNavDestination.entries.forEach { dest ->
      val selected = dest == current
      NavigationRailItem(
        selected = selected,
        onClick = { onSelect(dest) },
        icon = {
          Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label,
          )
        },
        label = { Text(dest.label) },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = primary,
          selectedTextColor = primary,
          indicatorColor = primary.copy(alpha = 0.18f),
          unselectedIconColor = TextSecondary,
          unselectedTextColor = TextSecondary,
        ),
      )
    }
  }
}
