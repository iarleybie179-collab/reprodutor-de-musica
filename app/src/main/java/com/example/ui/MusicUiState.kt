package com.example.ui

import androidx.media3.common.Player
import com.example.data.AudioTrack
import com.example.data.SortOption
import java.util.Locale

data class MusicUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasAudioPermission: Boolean = false,
    val hasAskedPermission: Boolean = false,
    val isLoadingLibrary: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE,
    val volumeLevel: Float = 0.7f,
    val playbackErrorMessage: String? = null,
)

fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
