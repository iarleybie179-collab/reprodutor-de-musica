package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceMusicRepository(private val context: Context) {

    suspend fun loadDeviceTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val selection =
            "((${MediaStore.Audio.Media.IS_MUSIC} != 0) OR " +
                "(${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' AND " +
                "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                "${MediaStore.Audio.Media.IS_RINGTONE} = 0)) AND " +
                "${MediaStore.Audio.Media.DURATION} >= 3000"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                    val displayName = cursor.getString(displayNameCol)?.trim().orEmpty()
                    val cleanTitle = when {
                        rawTitle.isNotEmpty() && !rawTitle.equals("<unknown>", ignoreCase = true) -> rawTitle
                        displayName.isNotEmpty() -> displayName.substringBeforeLast('.')
                        else -> "Faixa #$id"
                    }

                    val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                    val cleanArtist = if (rawArtist.equals("<unknown>", ignoreCase = true)) {
                        ""
                    } else {
                        rawArtist
                    }

                    val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                    val cleanAlbum = if (rawAlbum.equals("<unknown>", ignoreCase = true)) {
                        ""
                    } else {
                        rawAlbum
                    }

                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val mimeType = cursor.getString(mimeTypeCol)

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId)
                    } else {
                        null
                    }

                    tracks.add(
                        AudioTrack(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }

        tracks
    }
}
