package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaStoreRepository(private val context: Context) {

  suspend fun scanAudioFiles(): List<Track> = withContext(Dispatchers.IO) {
    val tracks = mutableListOf<Track>()
    val genreMap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      loadLegacyGenresMap()
    } else {
      emptyMap()
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projectionList = mutableListOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.ALBUM_ID,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.DATE_ADDED,
      MediaStore.Audio.Media.SIZE,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      projectionList.add(MediaStore.Audio.Media.RELATIVE_PATH)
    } else {
      @Suppress("DEPRECATION")
      projectionList.add(MediaStore.Audio.Media.DATA)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      projectionList.add(MediaStore.Audio.Media.GENRE)
    }

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
      context.contentResolver.query(
        collection,
        projectionList.toTypedArray(),
        selection,
        null,
        sortOrder,
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
          -1
        }
        val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
          @Suppress("DEPRECATION")
          cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        } else {
          -1
        }
        val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
        } else {
          -1
        }

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idCol)
          val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: continue
          val artist = cursor.getString(artistCol) ?: ""
          val album = cursor.getString(albumCol) ?: ""
          val albumId = cursor.getLong(albumIdCol)
          val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
          val dateAdded = cursor.getLong(dateAddedCol)
          val sizeBytes = cursor.getLong(sizeCol)

          val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id,
          ).toString()

          val (folderPath, folderName) = extractFolderInfo(
            relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null,
            dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null,
          )

          val rawGenre = if (genreCol >= 0) {
            cursor.getString(genreCol)?.trim()?.takeIf { it.isNotEmpty() }
          } else {
            genreMap[id]?.trim()?.takeIf { it.isNotEmpty() }
          }

          tracks.add(
            Track(
              id = id,
              uri = contentUri,
              title = title,
              artist = artist,
              album = album,
              albumId = albumId,
              durationMs = duration,
              folderPath = folderPath,
              folderName = folderName,
              genre = rawGenre,
              dateAdded = dateAdded,
              sizeBytes = sizeBytes,
            )
          )
        }
      }
    } catch (_: SecurityException) {
      return@withContext emptyList()
    } catch (_: Exception) {
      return@withContext emptyList()
    }

    tracks
  }

  private fun extractFolderInfo(relativePath: String?, dataPath: String?): Pair<String, String> {
    if (!relativePath.isNullOrBlank()) {
      val trimmed = relativePath.trimEnd('/')
      val folderName = trimmed.substringAfterLast('/', trimmed).ifBlank { "Armazenamento" }
      return trimmed to folderName
    }
    if (!dataPath.isNullOrBlank()) {
      val parent = File(dataPath).parentFile
      val path = parent?.absolutePath ?: "/"
      val name = parent?.name?.ifBlank { "Armazenamento" } ?: "Armazenamento"
      return path to name
    }
    return "Armazenamento" to "Armazenamento"
  }

  private fun loadLegacyGenresMap(): Map<Long, String> {
    val result = mutableMapOf<Long, String>()
    try {
      val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
      val projection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
      context.contentResolver.query(genresUri, projection, null, null, null)?.use { genreCursor ->
        val idIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
        val nameIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
        while (genreCursor.moveToNext()) {
          val genreId = genreCursor.getLong(idIdx)
          val genreName = genreCursor.getString(nameIdx)?.trim()
          if (genreName.isNullOrBlank()) continue
          val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
          context.contentResolver.query(
            membersUri,
            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
            null,
            null,
            null,
          )?.use { memberCursor ->
            val audioIdIdx = memberCursor.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
            if (audioIdIdx >= 0) {
              while (memberCursor.moveToNext()) {
                val audioId = memberCursor.getLong(audioIdIdx)
                result.putIfAbsent(audioId, genreName)
              }
            }
          }
        }
      }
    } catch (_: Exception) {
      // Ignore if device restricts genre querying
    }
    return result
  }

  suspend fun persistPickedImage(sourceUri: Uri, targetFileName: String): String? =
    withContext(Dispatchers.IO) {
      try {
        val dir = File(context.filesDir, "user_media").apply { mkdirs() }
        val destFile = File(dir, targetFileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        } ?: return@withContext null
        destFile.absolutePath
      } catch (_: Exception) {
        null
      }
    }

  suspend fun deleteInternalImage(path: String?) = withContext(Dispatchers.IO) {
    if (path.isNullOrBlank()) return@withContext
    try {
      val file = File(path)
      if (file.exists() && file.absolutePath.startsWith(context.filesDir.absolutePath)) {
        file.delete()
      }
    } catch (_: Exception) {
    }
  }
}
