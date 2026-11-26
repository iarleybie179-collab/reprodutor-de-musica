package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.R
import com.example.data.AudioTrack
import com.example.data.DeviceMusicRepository
import com.example.data.SortOption
import com.example.player.MusicPlaybackService
import com.example.player.VolumeObserver
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceMusicRepository(application)
    private val volumeObserver = VolumeObserver(application)

    private val _uiState = MutableStateFlow(
        MusicUiState(volumeLevel = volumeObserver.getNormalizedVolume())
    )
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null

    private val defaultArtworkUri: Uri = Uri.parse(
        "android.resource://${application.packageName}/${R.drawable.img_default_album_cover}"
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                syncControllerState()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncCurrentTrack(mediaItem)
            syncControllerState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncControllerState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    playbackErrorMessage = getApplication<Application>().getString(
                        R.string.playback_error_message
                    ),
                )
            }
        }
    }

    init {
        connectToMediaService()
        viewModelScope.launch {
            volumeObserver.observeVolumeChanges().collect { volume ->
                _uiState.update { it.copy(volumeLevel = volume) }
            }
        }
    }

    private fun connectToMediaService() {
        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, MusicPlaybackService::class.java))
        val future = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncCurrentTrack(controller.currentMediaItem)
                    syncControllerState()
                    if (controller.isPlaying) {
                        startPositionTicker()
                    }
                } catch (_: Exception) {
                }
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    fun onPermissionStatusChanged(granted: Boolean, asked: Boolean = true) {
        val previouslyGranted = _uiState.value.hasAudioPermission
        _uiState.update {
            it.copy(
                hasAudioPermission = granted,
                hasAskedPermission = it.hasAskedPermission || asked,
            )
        }
        if (granted && (!previouslyGranted || _uiState.value.tracks.isEmpty())) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        if (!_uiState.value.hasAudioPermission) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLibrary = true) }
            val loadedTracks = repository.loadDeviceTracks()
            _uiState.update { state ->
                val filtered = applyFilterAndSort(
                    tracks = loadedTracks,
                    query = state.searchQuery,
                    sortOption = state.sortOption,
                )
                val updatedCurrent = state.currentTrack?.let { active ->
                    loadedTracks.find { it.id == active.id } ?: active
                } ?: syncTrackFromController(loadedTracks)

                state.copy(
                    tracks = loadedTracks,
                    filteredTracks = filtered,
                    currentTrack = updatedCurrent,
                    isLoadingLibrary = false,
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTracks = applyFilterAndSort(state.tracks, query, state.sortOption),
            )
        }
    }

    fun updateSortOption(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                filteredTracks = applyFilterAndSort(state.tracks, state.searchQuery, option),
            )
        }
    }

    fun playTrack(track: AudioTrack, queueTracks: List<AudioTrack> = _uiState.value.filteredTracks) {
        val controller = mediaController ?: return
        val effectiveQueue = if (queueTracks.any { it.id == track.id }) {
            queueTracks
        } else {
            _uiState.value.tracks.ifEmpty { listOf(track) }
        }
        val startIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = effectiveQueue.map { it.toMediaItem() }

        _uiState.update {
            it.copy(
                currentTrack = track,
                durationMs = track.durationMs,
                currentPositionMs = 0L,
                playbackErrorMessage = null,
            )
        }

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            val firstTrack = _uiState.value.filteredTracks.firstOrNull()
                ?: _uiState.value.tracks.firstOrNull()
            if (firstTrack != null) {
                playTrack(firstTrack)
            }
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            val firstTrack = _uiState.value.tracks.firstOrNull() ?: return
            playTrack(firstTrack, _uiState.value.tracks)
            return
        }
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            if (!controller.isPlaying) controller.play()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
            if (!controller.isPlaying) controller.play()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            if (!controller.isPlaying) controller.play()
        } else {
            controller.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, _uiState.value.durationMs.coerceAtLeast(0L))
        controller.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val next = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = next
        _uiState.update { it.copy(shuffleEnabled = next) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun setVolume(fraction: Float) {
        volumeObserver.setNormalizedVolume(fraction)
        _uiState.update { it.copy(volumeLevel = fraction.coerceIn(0f, 1f)) }
    }

    fun dismissError() {
        _uiState.update { it.copy(playbackErrorMessage = null) }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L }
                        ?: _uiState.value.currentTrack?.durationMs
                        ?: 0L
                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun syncCurrentTrack(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId?.toLongOrNull() ?: return
        val matched = _uiState.value.tracks.find { it.id == id }
        if (matched != null) {
            _uiState.update {
                it.copy(
                    currentTrack = matched,
                    durationMs = matched.durationMs,
                )
            }
        } else {
            val meta = mediaItem.mediaMetadata
            _uiState.update {
                it.copy(
                    currentTrack = AudioTrack(
                        id = id,
                        title = meta.title?.toString().orEmpty().ifBlank { "Faixa #$id" },
                        artist = meta.artist?.toString().orEmpty(),
                        album = meta.albumTitle?.toString().orEmpty(),
                        durationMs = it.durationMs,
                        contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
                        albumArtUri = meta.artworkUri,
                        dateAdded = 0L,
                        mimeType = null,
                    )
                )
            }
        }
    }

    private fun syncTrackFromController(tracks: List<AudioTrack>): AudioTrack? {
        val id = mediaController?.currentMediaItem?.mediaId?.toLongOrNull() ?: return null
        return tracks.find { it.id == id }
    }

    private fun syncControllerState() {
        val controller = mediaController ?: return
        val duration = controller.duration.takeIf { it > 0L }
            ?: _uiState.value.currentTrack?.durationMs
            ?: 0L
        _uiState.update {
            it.copy(
                isPlaying = controller.isPlaying,
                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
            )
        }
    }

    private fun AudioTrack.toMediaItem(): MediaItem {
        val app = getApplication<Application>()
        val artistDisplay = artist.ifBlank { app.getString(R.string.unknown_artist) }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artistDisplay)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri ?: defaultArtworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun applyFilterAndSort(
        tracks: List<AudioTrack>,
        query: String,
        sortOption: SortOption,
    ): List<AudioTrack> {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            tracks
        } else {
            tracks.filter { track ->
                track.title.contains(trimmed, ignoreCase = true) ||
                    track.artist.contains(trimmed, ignoreCase = true) ||
                    track.album.contains(trimmed, ignoreCase = true)
            }
        }
        return when (sortOption) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedWith(
                compareBy<AudioTrack>(
                    { it.artist.isBlank() },
                    { it.artist.lowercase() },
                    { it.title.lowercase() },
                )
            )
            SortOption.RECENT -> filtered.sortedByDescending { it.dateAdded }
        }
    }

    override fun onCleared() {
        stopPositionTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        super.onCleared()
    }
}
