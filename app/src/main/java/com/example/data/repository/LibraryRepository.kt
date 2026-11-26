package com.example.data.repository

import android.net.Uri
import com.example.data.local.FavoriteEntity
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.RecentPlayEntity
import com.example.data.model.PlaylistUi
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LibraryRepository(
  private val mediaStoreRepository: MediaStoreRepository,
  private val musicDao: MusicDao,
) {

  val favoriteUrisFlow: Flow<List<String>> = musicDao.observeFavoriteUris()
  val recentPlaysFlow: Flow<List<RecentPlayEntity>> = musicDao.observeRecentPlays()

  fun observePlaylistsWithTracks(allTracksFlow: Flow<List<Track>>): Flow<List<PlaylistUi>> {
    return combine(
      musicDao.observePlaylists(),
      musicDao.observeAllPlaylistTracks(),
      allTracksFlow,
    ) { playlists, playlistTracks, tracks ->
      val trackByUri = tracks.associateBy { it.uri }
      val tracksByPlaylist = playlistTracks.groupBy { it.playlistId }
      playlists.map { playlist ->
        val entries = tracksByPlaylist[playlist.id].orEmpty()
        val resolvedTracks = entries.mapNotNull { trackByUri[it.trackUri] }
        PlaylistUi(
          id = playlist.id,
          name = playlist.name,
          coverPath = playlist.coverPath,
          tracks = resolvedTracks,
          updatedAt = playlist.updatedAt,
        )
      }
    }
  }

  suspend fun scanDeviceTracks(): List<Track> = mediaStoreRepository.scanAudioFiles()

  suspend fun toggleFavorite(trackUri: String, currentlyFavorite: Boolean) {
    if (currentlyFavorite) {
      musicDao.removeFavorite(trackUri)
    } else {
      musicDao.addFavorite(FavoriteEntity(trackUri = trackUri))
    }
  }

  suspend fun recordTrackPlayed(trackUri: String) {
    val existing = musicDao.getRecentPlay(trackUri)
    musicDao.upsertRecentPlay(
      RecentPlayEntity(
        trackUri = trackUri,
        playedAt = System.currentTimeMillis(),
        playCount = (existing?.playCount ?: 0) + 1,
      )
    )
  }

  suspend fun createPlaylist(name: String, coverSourceUri: Uri?): Long {
    val trimmed = name.trim().ifBlank { "Nova Playlist" }
    val id = musicDao.insertPlaylist(PlaylistEntity(name = trimmed))
    if (coverSourceUri != null) {
      val savedPath = mediaStoreRepository.persistPickedImage(
        sourceUri = coverSourceUri,
        targetFileName = "playlist_cover_${id}_${System.currentTimeMillis()}.jpg",
      )
      if (savedPath != null) {
        musicDao.updatePlaylistCover(id, savedPath)
      }
    }
    return id
  }

  suspend fun renamePlaylist(playlistId: Long, newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isNotEmpty()) {
      musicDao.renamePlaylist(playlistId, trimmed)
    }
  }

  suspend fun updatePlaylistCover(playlistId: Long, oldCoverPath: String?, sourceUri: Uri?) {
    if (sourceUri == null) {
      mediaStoreRepository.deleteInternalImage(oldCoverPath)
      musicDao.updatePlaylistCover(playlistId, null)
      return
    }
    val savedPath = mediaStoreRepository.persistPickedImage(
      sourceUri = sourceUri,
      targetFileName = "playlist_cover_${playlistId}_${System.currentTimeMillis()}.jpg",
    )
    if (savedPath != null) {
      mediaStoreRepository.deleteInternalImage(oldCoverPath)
      musicDao.updatePlaylistCover(playlistId, savedPath)
    }
  }

  suspend fun deletePlaylist(playlistId: Long, coverPath: String?) {
    mediaStoreRepository.deleteInternalImage(coverPath)
    musicDao.deletePlaylist(playlistId)
  }

  suspend fun addTracksToPlaylist(playlistId: Long, trackUris: List<String>) {
    var pos = musicDao.getPlaylistTrackCount(playlistId)
    trackUris.forEach { uri ->
      musicDao.addTrackToPlaylist(
        PlaylistTrackEntity(
          playlistId = playlistId,
          trackUri = uri,
          position = pos++,
        )
      )
    }
    musicDao.touchPlaylist(playlistId)
  }

  suspend fun removeTrackFromPlaylist(playlistId: Long, trackUri: String) {
    musicDao.removeTrackFromPlaylist(playlistId, trackUri)
    musicDao.touchPlaylist(playlistId)
  }

  suspend fun saveCustomWallpaper(oldPath: String?, sourceUri: Uri): String? {
    val saved = mediaStoreRepository.persistPickedImage(
      sourceUri = sourceUri,
      targetFileName = "custom_wallpaper_${System.currentTimeMillis()}.jpg",
    )
    if (saved != null && oldPath != saved) {
      mediaStoreRepository.deleteInternalImage(oldPath)
    }
    return saved
  }

  suspend fun clearCustomWallpaper(oldPath: String?) {
    mediaStoreRepository.deleteInternalImage(oldPath)
  }
}
