package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

  @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
  fun observePlaylists(): Flow<List<PlaylistEntity>>

  @Query("SELECT * FROM playlist_tracks ORDER BY position ASC, addedAt ASC")
  fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
  suspend fun renamePlaylist(playlistId: Long, name: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE playlists SET coverPath = :coverPath, updatedAt = :updatedAt WHERE id = :playlistId")
  suspend fun updatePlaylistCover(playlistId: Long, coverPath: String?, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
  suspend fun touchPlaylist(playlistId: Long, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: Long)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun addTrackToPlaylist(entry: PlaylistTrackEntity)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri")
  suspend fun removeTrackFromPlaylist(playlistId: Long, trackUri: String)

  @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun getPlaylistTrackCount(playlistId: Long): Int

  @Query("SELECT trackUri FROM favorites ORDER BY addedAt DESC")
  fun observeFavoriteUris(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addFavorite(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites WHERE trackUri = :trackUri")
  suspend fun removeFavorite(trackUri: String)

  @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT 40")
  fun observeRecentPlays(): Flow<List<RecentPlayEntity>>

  @Query("SELECT * FROM recent_plays WHERE trackUri = :trackUri LIMIT 1")
  suspend fun getRecentPlay(trackUri: String): RecentPlayEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecentPlay(entry: RecentPlayEntity)
}
