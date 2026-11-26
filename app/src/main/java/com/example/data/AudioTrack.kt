package com.example.data

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long,
    val mimeType: String?,
)

enum class SortOption {
    TITLE,
    ARTIST,
    RECENT,
}
