package com.example.player

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
  val currentTrack: Track? = null,
  val queue: List<Track> = emptyList(),
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val shuffleEnabled: Boolean = false,
  val repeatMode: Int = Player.REPEAT_MODE_OFF,
  val volumeRatio: Float = 0.7f,
)

class PlayerController(
  private val context: Context,
  private val onTrackStarted: (Track) -> Unit,
  private val onSavePlaybackProgress: (String?, Long) -> Unit,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var progressJob: Job? = null
  private var fadeJob: Job? = null
  private var knownTracksByUri: Map<String, Track> = emptyMap()

  var crossfadeEnabled: Boolean = true
  var autoplayEnabled: Boolean = true

  private val _state = MutableStateFlow(PlayerUiState(volumeRatio = readDeviceVolumeRatio()))
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
      _state.update { it.copy(volumeRatio = readDeviceVolumeRatio()) }
    }
  }

  private val playerListener = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
      _state.update { it.copy(isPlaying = isPlaying) }
      if (!isPlaying) {
        val current = _state.value.currentTrack
        val pos = mediaController?.currentPosition ?: 0L
        onSavePlaybackProgress(current?.uri, pos)
      }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
      val track = mediaItem?.mediaId?.let { knownTracksByUri[it] }
      val dur = (mediaController?.duration ?: 0L).takeIf { it > 0L } ?: (track?.durationMs ?: 0L)
      _state.update {
        it.copy(
          currentTrack = track ?: it.currentTrack,
          positionMs = 0L,
          durationMs = dur,
        )
      }
      if (track != null) {
        onTrackStarted(track)
        onSavePlaybackProgress(track.uri, 0L)
      }
      if (!autoplayEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
        mediaController?.pause()
      }
      if (crossfadeEnabled && reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
        runSoftVolumeRamp()
      }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
      _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
      _state.update { it.copy(repeatMode = repeatMode) }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
      val controller = mediaController ?: return
      val dur = controller.duration.takeIf { it > 0L } ?: (_state.value.currentTrack?.durationMs ?: 0L)
      _state.update { it.copy(durationMs = dur, positionMs = controller.currentPosition.coerceAtLeast(0L)) }
    }
  }

  fun connect() {
    if (controllerFuture != null) return
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        try {
          val controller = future.get()
          mediaController = controller
          controller.addListener(playerListener)
          syncFromController(controller)
          startProgressPolling()
        } catch (_: Exception) {
        }
      },
      ContextCompat.getMainExecutor(context),
    )
    runCatching {
      context.contentResolver.registerContentObserver(
        Settings.System.CONTENT_URI,
        true,
        volumeObserver,
      )
    }
  }

  fun updateKnownTracks(tracks: List<Track>) {
    knownTracksByUri = tracks.associateBy { it.uri }
    val currentUri = mediaController?.currentMediaItem?.mediaId ?: _state.value.currentTrack?.uri
    if (currentUri != null) {
      knownTracksByUri[currentUri]?.let { matched ->
        _state.update { it.copy(currentTrack = matched) }
      }
    }
  }

  fun playQueue(
    tracks: List<Track>,
    startIndex: Int = 0,
    shuffle: Boolean = false,
    startPositionMs: Long = 0L,
    autoPlay: Boolean = true,
  ) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    knownTracksByUri = knownTracksByUri + tracks.associateBy { it.uri }

    val controller = mediaController
    if (controller == null) {
      // Connect and retry once connected
      connect()
      scope.launch {
        delay(300)
        mediaController?.let { playQueueInternal(it, tracks, safeIndex, shuffle, startPositionMs, autoPlay) }
      }
      return
    }
    playQueueInternal(controller, tracks, safeIndex, shuffle, startPositionMs, autoPlay)
  }

  private fun playQueueInternal(
    controller: MediaController,
    tracks: List<Track>,
    startIndex: Int,
    shuffle: Boolean,
    startPositionMs: Long,
    autoPlay: Boolean,
  ) {
    val mediaItems = tracks.map { it.toMediaItem() }
    controller.shuffleModeEnabled = shuffle
    controller.setMediaItems(mediaItems, startIndex, startPositionMs)
    controller.prepare()
    if (autoPlay) {
      if (crossfadeEnabled) {
        runSoftVolumeRamp()
      } else {
        controller.volume = 1f
      }
      controller.play()
    }
    val selected = tracks[startIndex]
    _state.update {
      it.copy(
        currentTrack = selected,
        queue = tracks,
        shuffleEnabled = shuffle,
        durationMs = selected.durationMs,
        positionMs = startPositionMs,
        isPlaying = autoPlay,
      )
    }
    if (autoPlay) {
      onTrackStarted(selected)
    }
    onSavePlaybackProgress(selected.uri, startPositionMs)
  }

  fun togglePlayPause() {
    val controller = mediaController ?: return
    if (controller.isPlaying) {
      controller.pause()
    } else {
      if (controller.playbackState == Player.STATE_IDLE && _state.value.queue.isNotEmpty()) {
        controller.prepare()
      }
      controller.play()
    }
  }

  fun next() {
    val controller = mediaController ?: return
    if (controller.hasNextMediaItem()) {
      controller.seekToNextMediaItem()
      controller.play()
    } else if (controller.mediaItemCount > 0) {
      controller.seekTo(0, 0L)
      controller.play()
    }
  }

  fun previous() {
    val controller = mediaController ?: return
    if (controller.currentPosition > 3500L) {
      controller.seekTo(0L)
    } else if (controller.hasPreviousMediaItem()) {
      controller.seekToPreviousMediaItem()
      controller.play()
    } else {
      controller.seekTo(0L)
    }
  }

  fun seekTo(positionMs: Long) {
    val controller = mediaController ?: return
    val clamped = positionMs.coerceAtLeast(0L)
    controller.seekTo(clamped)
    _state.update { it.copy(positionMs = clamped) }
    onSavePlaybackProgress(_state.value.currentTrack?.uri, clamped)
  }

  fun toggleShuffle(): Boolean {
    val controller = mediaController
    val nextValue = !(controller?.shuffleModeEnabled ?: _state.value.shuffleEnabled)
    controller?.shuffleModeEnabled = nextValue
    _state.update { it.copy(shuffleEnabled = nextValue) }
    return nextValue
  }

  fun cycleRepeatMode(): Int {
    val current = mediaController?.repeatMode ?: _state.value.repeatMode
    val nextMode = when (current) {
      Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
      Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
      else -> Player.REPEAT_MODE_OFF
    }
    mediaController?.repeatMode = nextMode
    _state.update { it.copy(repeatMode = nextMode) }
    return nextMode
  }

  fun applySavedModes(shuffle: Boolean, repeatMode: Int) {
    mediaController?.let {
      it.shuffleModeEnabled = shuffle
      it.repeatMode = repeatMode
    }
    _state.update { it.copy(shuffleEnabled = shuffle, repeatMode = repeatMode) }
  }

  fun setVolumeRatio(ratio: Float) {
    val clamped = ratio.coerceIn(0f, 1f)
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val target = (clamped * maxVol).toInt().coerceIn(0, maxVol)
    runCatching {
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
    _state.update { it.copy(volumeRatio = clamped) }
  }

  private fun readDeviceVolumeRatio(): Float {
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    return (curr.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
  }

  private fun syncFromController(controller: MediaController) {
    val currentMediaId = controller.currentMediaItem?.mediaId
    val track = currentMediaId?.let { knownTracksByUri[it] }
    _state.update {
      it.copy(
        currentTrack = track ?: it.currentTrack,
        isPlaying = controller.isPlaying,
        positionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = controller.duration.takeIf { d -> d > 0L } ?: (track?.durationMs ?: it.durationMs),
        shuffleEnabled = controller.shuffleModeEnabled,
        repeatMode = controller.repeatMode,
        volumeRatio = readDeviceVolumeRatio(),
      )
    }
  }

  private fun startProgressPolling() {
    progressJob?.cancel()
    progressJob = scope.launch {
      var ticks = 0
      while (isActive) {
        val controller = mediaController
        if (controller != null && controller.isPlaying) {
          val pos = controller.currentPosition.coerceAtLeast(0L)
          val dur = controller.duration.takeIf { d -> d > 0L }
            ?: (_state.value.currentTrack?.durationMs ?: 0L)
          _state.update { it.copy(positionMs = pos, durationMs = dur, isPlaying = true) }
          ticks++
          if (ticks % 12 == 0) {
            onSavePlaybackProgress(_state.value.currentTrack?.uri, pos)
          }
        }
        delay(250L)
      }
    }
  }

  private fun runSoftVolumeRamp() {
    val controller = mediaController ?: return
    fadeJob?.cancel()
    fadeJob = scope.launch {
      controller.volume = 0.25f
      val steps = 8
      for (i in 1..steps) {
        delay(45L)
        controller.volume = 0.25f + (0.75f * i / steps)
      }
      controller.volume = 1f
    }
  }

  fun release() {
    progressJob?.cancel()
    fadeJob?.cancel()
    runCatching { context.contentResolver.unregisterContentObserver(volumeObserver) }
    controllerFuture?.let { MediaController.releaseFuture(it) }
    controllerFuture = null
    mediaController = null
  }

  private fun Track.toMediaItem(): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
      .setTitle(title)
      .setArtist(displayArtist)
      .setAlbumTitle(displayAlbum)
    albumArtUri?.let { metadataBuilder.setArtworkUri(it) }
    return MediaItem.Builder()
      .setMediaId(uri)
      .setUri(Uri.parse(uri))
      .setMediaMetadata(metadataBuilder.build())
      .build()
  }
}
