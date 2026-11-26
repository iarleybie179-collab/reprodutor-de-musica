package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DbzMusicApp
import com.example.data.local.AppSettings
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.EqPreset
import com.example.data.model.FolderGroup
import com.example.data.model.GenreGroup
import com.example.data.model.PlaylistUi
import com.example.data.model.SortOrder
import com.example.data.model.Track
import com.example.data.model.VisualEffectStyle
import com.example.player.EqualizerState
import com.example.player.PlaybackRuntimeBridge
import com.example.player.PlayerController
import com.example.player.PlayerUiState
import com.example.ui.theme.AccentThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as DbzMusicApp
  private val settingsStore = app.settingsDataStore
  private val repository = app.libraryRepository
  private val audioEffects = app.audioEffectsManager

  val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

  private val _hasAudioPermission = MutableStateFlow(false)
  val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _allScannedTracks = MutableStateFlow<List<Track>>(emptyList())
  val allScannedTracks: StateFlow<List<Track>> = _allScannedTracks.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _sortOrder = MutableStateFlow<SortOrder?>(null)
  val sortOrder: StateFlow<SortOrder> = combine(_sortOrder, settings) { override, appSettings ->
    override ?: appSettings.defaultSortOrder
  }.stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.TITLE)

  private val _showIntro = MutableStateFlow<Boolean?>(null)
  val showIntro: StateFlow<Boolean?> = _showIntro.asStateFlow()

  private var didAttemptResume = false

  val playerController = PlayerController(
    context = application.applicationContext,
    onTrackStarted = { track ->
      viewModelScope.launch {
        repository.recordTrackPlayed(track.uri)
      }
    },
    onSavePlaybackProgress = { uri, positionMs ->
      viewModelScope.launch {
        settingsStore.saveLastPlayback(uri, positionMs)
      }
    },
  )

  val playerState: StateFlow<PlayerUiState> = playerController.state
  val equalizerState: StateFlow<EqualizerState> = audioEffects.state

  val visibleTracks: StateFlow<List<Track>> = combine(
    _allScannedTracks,
    settings,
    sortOrder,
  ) { tracks, appSettings, currentSort ->
    val filtered = tracks.filter { track ->
      val folderVisible = !appSettings.hiddenFolders.contains(track.folderPath)
      val lengthVisible = !appSettings.hideShortClips || track.durationMs >= 5000L
      folderVisible && lengthVisible
    }
    sortTracks(filtered, currentSort)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredSearchTracks: StateFlow<List<Track>> = combine(
    visibleTracks,
    _searchQuery,
  ) { tracks, query ->
    val q = query.trim()
    if (q.isEmpty()) tracks
    else tracks.filter {
      it.title.contains(q, ignoreCase = true) ||
        it.displayArtist.contains(q, ignoreCase = true) ||
        it.displayAlbum.contains(q, ignoreCase = true)
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val albums: StateFlow<List<AlbumGroup>> = visibleTracks.map { tracks ->
    tracks.groupBy { it.albumId to it.displayAlbum }
      .map { (key, albumTracks) ->
        val first = albumTracks.first()
        AlbumGroup(
          albumId = key.first,
          name = key.second,
          artist = first.displayArtist,
          artUri = first.albumArtUri,
          tracks = albumTracks,
        )
      }
      .sortedBy { it.name.lowercase() }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val artists: StateFlow<List<ArtistGroup>> = visibleTracks.map { tracks ->
    tracks.groupBy { it.displayArtist }
      .map { (artistName, artistTracks) ->
        ArtistGroup(
          name = artistName,
          tracks = artistTracks,
          albumCount = artistTracks.map { it.displayAlbum }.distinct().size,
        )
      }
      .sortedBy { it.name.lowercase() }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val folders: StateFlow<List<FolderGroup>> = visibleTracks.map { tracks ->
    tracks.groupBy { it.folderPath }
      .map { (path, folderTracks) ->
        FolderGroup(
          folderPath = path,
          folderName = folderTracks.first().folderName,
          tracks = folderTracks,
        )
      }
      .sortedBy { it.folderName.lowercase() }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allFolderPaths: StateFlow<List<Pair<String, String>>> = _allScannedTracks.map { tracks ->
    tracks.distinctBy { it.folderPath }
      .map { it.folderPath to it.folderName }
      .sortedBy { it.second.lowercase() }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val genres: StateFlow<List<GenreGroup>> = visibleTracks.map { tracks ->
    tracks.groupBy { it.genre?.takeIf { g -> g.isNotBlank() } }
      .map { (genreName, genreTracks) ->
        GenreGroup(
          name = genreName ?: "Sem gênero informado",
          isUnknown = genreName == null,
          tracks = genreTracks,
        )
      }
      .sortedWith(compareBy<GenreGroup> { it.isUnknown }.thenBy { it.name.lowercase() })
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val favoriteUris: StateFlow<Set<String>> = repository.favoriteUrisFlow
    .map { it.toSet() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

  val favoriteTracks: StateFlow<List<Track>> = combine(
    visibleTracks,
    repository.favoriteUrisFlow,
  ) { tracks, favOrder ->
    val byUri = tracks.associateBy { it.uri }
    favOrder.mapNotNull { byUri[it] }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentTracks: StateFlow<List<Track>> = combine(
    visibleTracks,
    repository.recentPlaysFlow,
  ) { tracks, recents ->
    val byUri = tracks.associateBy { it.uri }
    recents.mapNotNull { byUri[it.trackUri] }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playlists: StateFlow<List<PlaylistUi>> = repository
    .observePlaylistsWithTracks(visibleTracks)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    playerController.connect()

    viewModelScope.launch {
      val initialSettings = settingsStore.settingsFlow.first()
      _showIntro.value = initialSettings.introEnabled
      playerController.applySavedModes(
        shuffle = initialSettings.shuffleEnabled,
        repeatMode = initialSettings.repeatMode,
      )
    }

    viewModelScope.launch {
      settings.collect { appSettings ->
        PlaybackRuntimeBridge.backgroundPlaybackEnabled = appSettings.backgroundPlaybackEnabled
        PlaybackRuntimeBridge.autoplayEnabled = appSettings.autoplayEnabled
        playerController.crossfadeEnabled = appSettings.crossfadeEnabled
        playerController.autoplayEnabled = appSettings.autoplayEnabled
        audioEffects.applySettings(
          enabled = appSettings.eqEnabled,
          preset = appSettings.eqPreset,
          bass = appSettings.eqBassLevel,
          mid = appSettings.eqMidLevel,
          treble = appSettings.eqTrebleLevel,
        )
      }
    }

    viewModelScope.launch {
      PlaybackRuntimeBridge.audioSessionId.collect { sessionId ->
        if (sessionId > 0) {
          audioEffects.attachToAudioSession(sessionId)
        }
      }
    }
  }

  fun onPermissionResult(granted: Boolean) {
    _hasAudioPermission.value = granted
    if (granted) {
      refreshLibrary()
    }
  }

  fun refreshLibrary() {
    viewModelScope.launch {
      _isScanning.value = true
      val tracks = repository.scanDeviceTracks()
      _allScannedTracks.value = tracks
      playerController.updateKnownTracks(tracks)
      _isScanning.value = false

      if (!didAttemptResume && tracks.isNotEmpty()) {
        didAttemptResume = true
        val currentSettings = settings.value
        if (currentSettings.resumeLastPlayback &&
          playerState.value.currentTrack == null &&
          currentSettings.lastTrackUri != null
        ) {
          val idx = tracks.indexOfFirst { it.uri == currentSettings.lastTrackUri }
          if (idx >= 0) {
            playerController.playQueue(
              tracks = tracks,
              startIndex = idx,
              shuffle = currentSettings.shuffleEnabled,
              startPositionMs = currentSettings.lastPositionMs,
              autoPlay = false,
            )
            playerController.applySavedModes(
              shuffle = currentSettings.shuffleEnabled,
              repeatMode = currentSettings.repeatMode,
            )
          }
        }
      }
    }
  }

  fun finishIntro() {
    _showIntro.value = false
  }

  fun replayIntro() {
    _showIntro.value = true
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSortOrder(order: SortOrder) {
    _sortOrder.value = order
  }

  // Playback actions
  fun playTrackFromList(tracks: List<Track>, index: Int) {
    if (tracks.isEmpty()) return
    playerController.playQueue(
      tracks = tracks,
      startIndex = index,
      shuffle = playerState.value.shuffleEnabled,
    )
  }

  fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
    if (tracks.isEmpty()) return
    val startIndex = if (shuffle) tracks.indices.random() else 0
    playerController.playQueue(
      tracks = tracks,
      startIndex = startIndex,
      shuffle = shuffle,
    )
    viewModelScope.launch {
      settingsStore.setShuffleEnabled(shuffle)
    }
  }

  fun togglePlayPause() = playerController.togglePlayPause()
  fun nextTrack() = playerController.next()
  fun previousTrack() = playerController.previous()
  fun seekTo(ms: Long) = playerController.seekTo(ms)
  fun setVolume(ratio: Float) = playerController.setVolumeRatio(ratio)

  fun toggleShuffle() {
    val enabled = playerController.toggleShuffle()
    viewModelScope.launch { settingsStore.setShuffleEnabled(enabled) }
  }

  fun cycleRepeatMode() {
    val mode = playerController.cycleRepeatMode()
    viewModelScope.launch { settingsStore.setRepeatMode(mode) }
  }

  // Favorites
  fun toggleFavorite(trackUri: String) {
    viewModelScope.launch {
      repository.toggleFavorite(trackUri, favoriteUris.value.contains(trackUri))
    }
  }

  // Playlists
  fun createPlaylist(name: String, coverUri: Uri?) {
    viewModelScope.launch {
      repository.createPlaylist(name, coverUri)
    }
  }

  fun createPlaylistAndAddTrack(name: String, trackUri: String) {
    viewModelScope.launch {
      val playlistId = repository.createPlaylist(name, null)
      repository.addTracksToPlaylist(playlistId, listOf(trackUri))
    }
  }

  fun renamePlaylist(playlistId: Long, newName: String) {
    viewModelScope.launch {
      repository.renamePlaylist(playlistId, newName)
    }
  }

  fun updatePlaylistCover(playlistId: Long, oldCoverPath: String?, sourceUri: Uri?) {
    viewModelScope.launch {
      repository.updatePlaylistCover(playlistId, oldCoverPath, sourceUri)
    }
  }

  fun deletePlaylist(playlist: PlaylistUi) {
    viewModelScope.launch {
      repository.deletePlaylist(playlist.id, playlist.coverPath)
    }
  }

  fun addTracksToPlaylist(playlistId: Long, trackUris: List<String>) {
    viewModelScope.launch {
      repository.addTracksToPlaylist(playlistId, trackUris)
    }
  }

  fun removeTrackFromPlaylist(playlistId: Long, trackUri: String) {
    viewModelScope.launch {
      repository.removeTrackFromPlaylist(playlistId, trackUri)
    }
  }

  // Wallpaper & Personalization
  fun selectCustomWallpaper(uri: Uri) {
    viewModelScope.launch {
      val oldPath = settings.value.customWallpaperPath
      val newPath = repository.saveCustomWallpaper(oldPath, uri)
      if (newPath != null) {
        settingsStore.updateWallpaperPath(newPath)
      }
    }
  }

  fun restoreDefaultWallpaper() {
    viewModelScope.launch {
      val oldPath = settings.value.customWallpaperPath
      repository.clearCustomWallpaper(oldPath)
      settingsStore.updateWallpaperPath(null)
    }
  }

  fun setWallpaperDim(dim: Float) = viewModelScope.launch { settingsStore.setWallpaperDim(dim) }
  fun setWallpaperBlur(blur: Float) = viewModelScope.launch { settingsStore.setWallpaperBlur(blur) }
  fun setAccentPreset(preset: AccentThemePreset) = viewModelScope.launch { settingsStore.setAccentPreset(preset) }
  fun setVisualEffect(style: VisualEffectStyle) = viewModelScope.launch { settingsStore.setVisualEffect(style) }
  fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAnimationsEnabled(enabled) }
  fun setParticlesEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setParticlesEnabled(enabled) }

  // Intro settings
  fun setIntroEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setIntroEnabled(enabled) }
  fun setIntroVoiceEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setIntroVoiceEnabled(enabled) }
  fun setIntroSubtitleEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setIntroSubtitleEnabled(enabled) }
  fun setIntroDuration(sec: Float) = viewModelScope.launch { settingsStore.setIntroDuration(sec) }

  // Playback settings
  fun setAutoplayEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAutoplayEnabled(enabled) }
  fun setShuffleSetting(enabled: Boolean) {
    playerController.applySavedModes(enabled, settings.value.repeatMode)
    viewModelScope.launch { settingsStore.setShuffleEnabled(enabled) }
  }
  fun setRepeatSetting(mode: Int) {
    playerController.applySavedModes(settings.value.shuffleEnabled, mode)
    viewModelScope.launch { settingsStore.setRepeatMode(mode) }
  }
  fun setResumeLastPlayback(enabled: Boolean) = viewModelScope.launch { settingsStore.setResumeLastPlayback(enabled) }
  fun setCrossfadeEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setCrossfadeEnabled(enabled) }
  fun setBackgroundPlaybackEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setBackgroundPlaybackEnabled(enabled) }

  // Equalizer settings
  fun setEqEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setEqEnabled(enabled) }
  fun setEqPreset(preset: EqPreset) = viewModelScope.launch { settingsStore.setEqPreset(preset) }
  fun setEqBands(bass: Float, mid: Float, treble: Float) = viewModelScope.launch {
    settingsStore.setEqPreset(EqPreset.PERSONALIZADO)
    settingsStore.setEqBands(bass, mid, treble)
  }

  // Library settings
  fun setDefaultSortOrder(sort: SortOrder) = viewModelScope.launch {
    _sortOrder.value = null
    settingsStore.setDefaultSortOrder(sort)
  }
  fun toggleFolderHidden(folderPath: String) = viewModelScope.launch { settingsStore.toggleFolderHidden(folderPath) }
  fun setHideShortClips(hide: Boolean) = viewModelScope.launch { settingsStore.setHideShortClips(hide) }

  // Performance settings
  fun setBatterySaverMode(enabled: Boolean) = viewModelScope.launch { settingsStore.setBatterySaverMode(enabled) }
  fun setReduceAnimations(enabled: Boolean) = viewModelScope.launch { settingsStore.setReduceAnimations(enabled) }
  fun setReduceEffects(enabled: Boolean) = viewModelScope.launch { settingsStore.setReduceEffects(enabled) }

  private fun sortTracks(tracks: List<Track>, order: SortOrder): List<Track> = when (order) {
    SortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
    SortOrder.ARTIST -> tracks.sortedWith(compareBy<Track> { it.displayArtist.lowercase() }.thenBy { it.title.lowercase() })
    SortOrder.DURATION -> tracks.sortedByDescending { it.durationMs }
    SortOrder.DATE_ADDED -> tracks.sortedByDescending { it.dateAdded }
  }

  override fun onCleared() {
    playerController.release()
    audioEffects.releaseEffects()
    super.onCleared()
  }
}
