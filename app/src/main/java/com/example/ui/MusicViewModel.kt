package com.example.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
    private var isConnectingController: Boolean = false
    private var positionTickerJob: Job? = null

    private var activeQueue: List<AudioTrack> = emptyList()
    private var activeQueueWindowStart: Int = 0
    private var pendingPlayRequest: Pair<AudioTrack, List<AudioTrack>>? = null
    private var consecutiveErrorCount: Int = 0

    private val defaultArtworkUri: Uri = Uri.parse(
        "android.resource://${application.packageName}/${R.drawable.img_default_album_cover}"
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                consecutiveErrorCount = 0
                _uiState.update { it.copy(isPlaying = true, playbackErrorMessage = null) }
                startPositionTicker()
            } else {
                _uiState.update { it.copy(isPlaying = false) }
                stopPositionTicker()
                syncControllerState()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncCurrentTrack(mediaItem)
            syncControllerState()
            maybeRebalanceWindow()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                consecutiveErrorCount = 0
            }
            syncControllerState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackFailure()
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
        if (isConnectingController) return
        val existing = mediaController
        if (existing != null && existing.isConnected) return

        val app = getApplication<Application>()
        isConnectingController = true
        try {
            val sessionToken = SessionToken(app, ComponentName(app, MusicPlaybackService::class.java))
            val future = MediaController.Builder(app, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    isConnectingController = false
                    try {
                        val controller = future.get()
                        mediaController?.removeListener(playerListener)
                        mediaController = controller
                        controller.addListener(playerListener)
                        syncCurrentTrack(controller.currentMediaItem)
                        syncControllerState()
                        if (controller.isPlaying) {
                            startPositionTicker()
                        }

                        val pending = pendingPlayRequest
                        if (pending != null) {
                            pendingPlayRequest = null
                            playTrack(pending.first, pending.second)
                        }
                    } catch (_: Exception) {
                        mediaController = null
                        pendingPlayRequest = null
                    }
                },
                ContextCompat.getMainExecutor(app),
            )
        } catch (_: Exception) {
            isConnectingController = false
        }
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
        val app = getApplication<Application>()
        if (!hasAudioPermission(app)) {
            _uiState.update {
                it.copy(
                    hasAudioPermission = false,
                    playbackErrorMessage = app.getString(R.string.permission_title),
                )
            }
            return
        }

        val effectiveQueue = when {
            queueTracks.any { it.id == track.id } -> queueTracks
            _uiState.value.tracks.any { it.id == track.id } -> _uiState.value.tracks
            else -> listOf(track)
        }
        activeQueue = effectiveQueue
        consecutiveErrorCount = 0

        val controller = mediaController
        if (controller == null || !controller.isConnected) {
            pendingPlayRequest = track to effectiveQueue
            _uiState.update {
                it.copy(
                    currentTrack = track,
                    durationMs = track.durationMs,
                    currentPositionMs = 0L,
                    playbackErrorMessage = null,
                )
            }
            connectToMediaService()
            return
        }

        val globalIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        loadWindowAndPlay(controller, effectiveQueue, globalIndex)
    }

    private fun loadWindowAndPlay(
        controller: MediaController,
        queue: List<AudioTrack>,
        globalIndex: Int,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
    ) {
        if (queue.isEmpty()) return
        val safeGlobalIndex = globalIndex.coerceIn(0, queue.lastIndex)
        val selectedTrack = queue[safeGlobalIndex]

        val windowStart = if (queue.size <= MAX_BINDER_QUEUE_SIZE) {
            0
        } else {
            val half = MAX_BINDER_QUEUE_SIZE / 2
            (safeGlobalIndex - half).coerceIn(0, queue.size - MAX_BINDER_QUEUE_SIZE)
        }
        val windowEnd = (windowStart + MAX_BINDER_QUEUE_SIZE).coerceAtMost(queue.size)
        val windowTracks = queue.subList(windowStart, windowEnd)
        val localIndex = (safeGlobalIndex - windowStart).coerceIn(0, windowTracks.lastIndex)

        activeQueueWindowStart = windowStart
        val mediaItems = windowTracks.map { it.toMediaItem() }

        _uiState.update {
            it.copy(
                currentTrack = selectedTrack,
                durationMs = selectedTrack.durationMs,
                currentPositionMs = startPositionMs,
                playbackErrorMessage = null,
            )
        }

        try {
            controller.setMediaItems(mediaItems, localIndex, startPositionMs)
            controller.prepare()
            if (autoPlay) {
                controller.play()
            }
        } catch (_: Exception) {
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

    private fun maybeRebalanceWindow() {
        val controller = mediaController ?: return
        if (!controller.isConnected || activeQueue.size <= MAX_BINDER_QUEUE_SIZE) return

        val localIndex = controller.currentMediaItemIndex
        val itemCount = controller.mediaItemCount
        if (localIndex < 0 || itemCount <= 0) return

        val globalIndex = (activeQueueWindowStart + localIndex).coerceIn(0, activeQueue.lastIndex)
        val nearStart = localIndex <= 2 && activeQueueWindowStart > 0
        val nearEnd = localIndex >= itemCount - 3 && (activeQueueWindowStart + itemCount) < activeQueue.size

        if (nearStart || nearEnd) {
            val currentPos = controller.currentPosition.coerceAtLeast(0L)
            val wasPlaying = controller.playWhenReady
            loadWindowAndPlay(
                controller = controller,
                queue = activeQueue,
                globalIndex = globalIndex,
                startPositionMs = currentPos,
                autoPlay = wasPlaying,
            )
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null || !controller.isConnected) {
            val fallbackTrack = _uiState.value.currentTrack
                ?: _uiState.value.filteredTracks.firstOrNull()
                ?: _uiState.value.tracks.firstOrNull()
            if (fallbackTrack != null) {
                playTrack(fallbackTrack)
            } else {
                connectToMediaService()
            }
            return
        }

        try {
            if (controller.mediaItemCount == 0) {
                val firstTrack = _uiState.value.currentTrack
                    ?: _uiState.value.filteredTracks.firstOrNull()
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
                if (controller.playbackState == Player.STATE_IDLE) {
                    controller.prepare()
                }
                controller.play()
            }
        } catch (_: Exception) {
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

    fun skipToNext() {
        val controller = mediaController
        if (controller == null || !controller.isConnected) {
            connectToMediaService()
            return
        }

        try {
            if (controller.mediaItemCount == 0) {
                val firstTrack = _uiState.value.tracks.firstOrNull() ?: return
                playTrack(firstTrack, _uiState.value.tracks)
                return
            }

            if (activeQueue.size > MAX_BINDER_QUEUE_SIZE) {
                val currentGlobal = getCurrentGlobalIndex(controller)
                val nextGlobal = if (_uiState.value.shuffleEnabled && activeQueue.size > 1) {
                    (activeQueue.indices - currentGlobal).random()
                } else if (currentGlobal + 1 < activeQueue.size) {
                    currentGlobal + 1
                } else {
                    0
                }
                loadWindowAndPlay(controller, activeQueue, nextGlobal)
                return
            }

            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                if (!controller.isPlaying) controller.play()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
                if (!controller.isPlaying) controller.play()
            }
        } catch (_: Exception) {
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        if (controller == null || !controller.isConnected) {
            connectToMediaService()
            return
        }

        try {
            if (controller.mediaItemCount == 0) return
            if (controller.currentPosition > 3000L) {
                controller.seekTo(0L)
                return
            }

            if (activeQueue.size > MAX_BINDER_QUEUE_SIZE) {
                val currentGlobal = getCurrentGlobalIndex(controller)
                val prevGlobal = if (currentGlobal - 1 >= 0) {
                    currentGlobal - 1
                } else {
                    activeQueue.lastIndex
                }
                loadWindowAndPlay(controller, activeQueue, prevGlobal)
                return
            }

            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
                if (!controller.isPlaying) controller.play()
            } else {
                controller.seekTo(0L)
            }
        } catch (_: Exception) {
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
        val maxDur = _uiState.value.durationMs.coerceAtLeast(0L)
        val clamped = positionMs.coerceIn(0L, maxDur)
        try {
            controller.seekTo(clamped)
            _uiState.update { it.copy(currentPositionMs = clamped) }
        } catch (_: Exception) {
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
        try {
            val next = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = next
            _uiState.update { it.copy(shuffleEnabled = next) }
        } catch (_: Exception) {
        }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
        try {
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _uiState.update { it.copy(repeatMode = nextMode) }
        } catch (_: Exception) {
        }
    }

    fun setVolume(fraction: Float) {
        volumeObserver.setNormalizedVolume(fraction)
        _uiState.update { it.copy(volumeLevel = fraction.coerceIn(0f, 1f)) }
    }

    fun dismissError() {
        _uiState.update { it.copy(playbackErrorMessage = null) }
    }

    private fun handlePlaybackFailure() {
        val app = getApplication<Application>()
        val controller = mediaController
        consecutiveErrorCount++

        val canSkipNext = controller != null &&
            controller.isConnected &&
            activeQueue.size > 1 &&
            consecutiveErrorCount < MAX_CONSECUTIVE_SKIP_ERRORS &&
            consecutiveErrorCount < activeQueue.size

        if (canSkipNext) {
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    playbackErrorMessage = app.getString(R.string.playback_error_skipping),
                )
            }
            skipToNext()
        } else {
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    playbackErrorMessage = app.getString(R.string.playback_error_message),
                )
            }
        }
    }

    private fun getCurrentGlobalIndex(controller: MediaController): Int {
        val currentId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        if (currentId != null) {
            val idx = activeQueue.indexOfFirst { it.id == currentId }
            if (idx >= 0) return idx
        }
        return (activeQueueWindowStart + controller.currentMediaItemIndex)
            .coerceIn(0, (activeQueue.size - 1).coerceAtLeast(0))
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isConnected) {
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
        val matched = activeQueue.find { it.id == id }
            ?: _uiState.value.tracks.find { it.id == id }
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
        val controller = mediaController ?: return null
        if (!controller.isConnected) return null
        val id = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return null
        return tracks.find { it.id == id }
    }

    private fun syncControllerState() {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
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
        val safeTitle = title.ifBlank { "Faixa #$id" }
        val artistDisplay = artist.ifBlank { app.getString(R.string.unknown_artist) }
        val metadata = MediaMetadata.Builder()
            .setTitle(safeTitle)
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

    private fun hasAudioPermission(app: Application): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        stopPositionTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        super.onCleared()
    }

    companion object {
        private const val MAX_BINDER_QUEUE_SIZE = 31
        private const val MAX_CONSECUTIVE_SKIP_ERRORS = 3
    }
}
