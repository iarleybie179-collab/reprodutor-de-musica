package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackRuntimeBridge {
  private val _audioSessionId = MutableStateFlow(0)
  val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

  @Volatile
  var backgroundPlaybackEnabled: Boolean = true

  @Volatile
  var autoplayEnabled: Boolean = true

  fun updateAudioSessionId(sessionId: Int) {
    if (sessionId > 0) {
      _audioSessionId.value = sessionId
    }
  }
}

class PlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null
  private var player: ExoPlayer? = null

  companion object {
    const val CHANNEL_ID = "dbz_ki_playback_channel"
    const val NOTIFICATION_ID = 1001
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()

    val audioAttributes = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)
      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
      .build()

    val exoPlayer = ExoPlayer.Builder(this)
      .setAudioAttributes(audioAttributes, true)
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build()

    exoPlayer.addAnalyticsListener(object : AnalyticsListener {
      override fun onAudioSessionIdChanged(
        eventTime: AnalyticsListener.EventTime,
        audioSessionId: Int,
      ) {
        PlaybackRuntimeBridge.updateAudioSessionId(audioSessionId)
      }
    })

    exoPlayer.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
          PlaybackRuntimeBridge.updateAudioSessionId(exoPlayer.audioSessionId)
        }
      }
    })

    val launchIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val session = MediaSession.Builder(this, exoPlayer)
      .setSessionActivity(pendingIntent)
      .build()

    val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
      .setChannelId(CHANNEL_ID)
      .setNotificationId(NOTIFICATION_ID)
      .build()
    notificationProvider.setSmallIcon(R.drawable.ic_launcher_foreground)
    setMediaNotificationProvider(notificationProvider)

    player = exoPlayer
    mediaSession = session

    if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
      PlaybackRuntimeBridge.updateAudioSessionId(exoPlayer.audioSessionId)
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    val currentPlayer = mediaSession?.player
    if (currentPlayer == null ||
      !PlaybackRuntimeBridge.backgroundPlaybackEnabled ||
      !currentPlayer.playWhenReady ||
      currentPlayer.mediaItemCount == 0
    ) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    player = null
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java) ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          getString(R.string.notification_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = getString(R.string.notification_channel_desc)
          setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }
}
