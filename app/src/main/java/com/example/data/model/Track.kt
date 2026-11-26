package com.example.data.model

import android.net.Uri

data class Track(
  val id: Long,
  val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  val folderPath: String,
  val folderName: String,
  val genre: String?,
  val dateAdded: Long,
  val sizeBytes: Long,
) {
  val albumArtUri: Uri?
    get() = if (albumId > 0L) {
      Uri.parse("content://media/external/audio/albumart/$albumId")
    } else {
      null
    }

  val displayArtist: String
    get() = artist.takeIf {
      it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
    } ?: "Artista desconhecido"

  val displayAlbum: String
    get() = album.takeIf {
      it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
    } ?: "Álbum desconhecido"
}

data class AlbumGroup(
  val albumId: Long,
  val name: String,
  val artist: String,
  val artUri: Uri?,
  val tracks: List<Track>,
)

data class ArtistGroup(
  val name: String,
  val tracks: List<Track>,
  val albumCount: Int,
)

data class FolderGroup(
  val folderPath: String,
  val folderName: String,
  val tracks: List<Track>,
)

data class GenreGroup(
  val name: String,
  val isUnknown: Boolean,
  val tracks: List<Track>,
)

data class PlaylistUi(
  val id: Long,
  val name: String,
  val coverPath: String?,
  val tracks: List<Track>,
  val updatedAt: Long,
)

enum class SortOrder(val id: String, val label: String) {
  TITLE("title", "Nome"),
  ARTIST("artist", "Artista"),
  DURATION("duration", "Duração"),
  DATE_ADDED("date_added", "Recentes");

  companion object {
    fun fromId(id: String): SortOrder = entries.firstOrNull { it.id == id } ?: TITLE
  }
}

enum class LibraryTab(val title: String) {
  FOR_YOU("Para você"),
  TRACKS("Faixas"),
  PLAYLISTS("Playlists"),
  FOLDERS("Pastas"),
  ALBUMS("Álbuns"),
  ARTISTS("Artistas"),
  GENRES("Gêneros"),
}

enum class VisualEffectStyle(val id: String, val label: String) {
  NONE("none", "Nenhum"),
  NEON_PULSE("neon_pulse", "Pulso Neon"),
  ENERGY_AURA("energy_aura", "Aura de Ki"),
  COSMIC_GRID("cosmic_grid", "Grade Cósmica");

  companion object {
    fun fromId(id: String): VisualEffectStyle =
      entries.firstOrNull { it.id == id } ?: ENERGY_AURA
  }
}

enum class EqPreset(val id: String, val label: String) {
  NORMAL("normal", "Normal"),
  ROCK("rock", "Rock"),
  POP("pop", "Pop"),
  CLASSICO("classico", "Clássico"),
  ANIME("anime", "Anime"),
  PERSONALIZADO("personalizado", "Personalizado");

  companion object {
    fun fromId(id: String): EqPreset = entries.firstOrNull { it.id == id } ?: NORMAL
  }
}

fun formatDuration(ms: Long): String {
  if (ms <= 0L) return "0:00"
  val totalSeconds = ms / 1000L
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%d:%02d".format(minutes, seconds)
  }
}
