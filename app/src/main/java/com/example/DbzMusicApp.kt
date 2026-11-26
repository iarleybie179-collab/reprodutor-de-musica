package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.LibraryRepository
import com.example.data.repository.MediaStoreRepository
import com.example.player.AudioEffectsManager

class DbzMusicApp : Application() {
  lateinit var database: AppDatabase
    private set
  lateinit var settingsDataStore: SettingsDataStore
    private set
  lateinit var mediaStoreRepository: MediaStoreRepository
    private set
  lateinit var libraryRepository: LibraryRepository
    private set
  lateinit var audioEffectsManager: AudioEffectsManager
    private set

  override fun onCreate() {
    super.onCreate()
    database = AppDatabase.getInstance(this)
    settingsDataStore = SettingsDataStore(this)
    mediaStoreRepository = MediaStoreRepository(this)
    libraryRepository = LibraryRepository(mediaStoreRepository, database.musicDao())
    audioEffectsManager = AudioEffectsManager()
  }
}
