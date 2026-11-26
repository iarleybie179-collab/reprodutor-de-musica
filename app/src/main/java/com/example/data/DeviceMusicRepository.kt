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
            MediaStore.Audio.Media.SIZE,
        )

        val selection =
            "((${MediaStore.Audio.Media.IS_MUSIC} != 0) OR " +
                "(${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' AND " +
                "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                "${MediaStore.Audio.Media.IS_RINGTONE} = 0)) AND " +
                "${MediaStore.Audio.Media.DURATION} >= 3000 AND " +
                "${MediaStore.Audio.Media.SIZE} > 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                if (idCol < 0) return@use

                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idCol)
                        if (id <= 0L) continue

                        val rawTitle = if (titleCol >= 0) cursor.getString(titleCol)?.trim().orEmpty() else ""
                        val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol)?.trim().orEmpty() else ""
                        val cleanTitle = when {
                            rawTitle.isNotEmpty() && !rawTitle.equals("<unknown>", ignoreCase = true) -> rawTitle
                            displayName.isNotEmpty() -> displayName.substringBeforeLast('.').ifBlank { displayName }
                            else -> "Faixa #$id"
                        }

                        val rawArtist = if (artistCol >= 0) cursor.getString(artistCol)?.trim().orEmpty() else ""
                        val cleanArtist = if (rawArtist.equals("<unknown>", ignoreCase = true)) "" else rawArtist

                        val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol)?.trim().orEmpty() else ""
                        val cleanAlbum = if (rawAlbum.equals("<unknown>", ignoreCase = true)) "" else rawAlbum

                        val albumId = if (albumIdCol >= 0 && !cursor.isNull(albumIdCol)) cursor.getLong(albumIdCol) else 0L
                        val durationMs = if (durationCol >= 0 && !cursor.isNull(durationCol)) cursor.getLong(durationCol).coerceAtLeast(0L) else 0L
                        val dateAdded = if (dateAddedCol >= 0 && !cursor.isNull(dateAddedCol)) cursor.getLong(dateAddedCol) else 0L
                        val mimeType = if (mimeTypeCol >= 0) cursor.getString(mimeTypeCol) else null

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
                    } catch (_: Exception) {
                        // Skip malformed MediaStore rows safely
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        tracks
    }
}
