package com.example.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

class VolumeObserver(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun getNormalizedVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    fun setNormalizedVolume(fraction: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun observeVolumeChanges(): Flow<Float> = callbackFlow {
        trySend(getNormalizedVolume())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == VOLUME_CHANGED_ACTION) {
                    trySend(getNormalizedVolume())
                }
            }
        }

        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
