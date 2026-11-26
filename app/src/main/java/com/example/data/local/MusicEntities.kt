package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val coverPath: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "playlist_tracks",
  primaryKeys = ["playlistId", "trackUri"],
  foreignKeys = [
    ForeignKey(
      entity = PlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["playlistId"])],
)
data class PlaylistTrackEntity(
  val playlistId: Long,
  val trackUri: String,
  val position: Int = 0,
  val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
  @PrimaryKey val trackUri: String,
  val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
  @PrimaryKey val trackUri: String,
  val playedAt: Long = System.currentTimeMillis(),
  val playCount: Int = 1,
)
