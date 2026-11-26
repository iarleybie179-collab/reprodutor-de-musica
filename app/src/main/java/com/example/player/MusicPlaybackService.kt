package com.example.player

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("reprodutor_playback_channel")
            .setChannelName(R.string.notification_channel_name)
            .build().apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(CacheBitmapLoader(SafeBitmapLoader(this)))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // Ignore foreground start restriction when triggered from background state
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
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
        super.onDestroy()
    }
}
