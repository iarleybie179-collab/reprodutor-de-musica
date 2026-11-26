package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EqPreset
import com.example.data.model.SortOrder
import com.example.data.model.VisualEffectStyle
import com.example.ui.theme.AccentThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dbz_settings")

data class AppSettings(
  // Personalization
  val customWallpaperPath: String? = null,
  val wallpaperDimIntensity: Float = 0.58f,
  val wallpaperBlurRadius: Float = 0f,
  val accentPreset: AccentThemePreset = AccentThemePreset.SAIYAN_BLUE,
  val visualEffectStyle: VisualEffectStyle = VisualEffectStyle.ENERGY_AURA,
  val animationsEnabled: Boolean = true,
  val particlesEnabled: Boolean = true,

  // Intro
  val introEnabled: Boolean = true,
  val introVoiceEnabled: Boolean = true,
  val introSubtitleEnabled: Boolean = true,
  val introDurationSec: Float = 4.5f,

  // Playback
  val autoplayEnabled: Boolean = true,
  val shuffleEnabled: Boolean = false,
  val repeatMode: Int = 0, // Player.REPEAT_MODE_OFF = 0, ONE = 1, ALL = 2
  val resumeLastPlayback: Boolean = true,
  val crossfadeEnabled: Boolean = true,
  val backgroundPlaybackEnabled: Boolean = true,
  val lastTrackUri: String? = null,
  val lastPositionMs: Long = 0L,

  // Audio / Equalizer
  val eqEnabled: Boolean = false,
  val eqPreset: EqPreset = EqPreset.ANIME,
  val eqBassLevel: Float = 0.2f,    // -1f..1f
  val eqMidLevel: Float = 0.0f,     // -1f..1f
  val eqTrebleLevel: Float = 0.25f, // -1f..1f

  // Library
  val defaultSortOrder: SortOrder = SortOrder.TITLE,
  val hiddenFolders: Set<String> = emptySet(),
  val hideShortClips: Boolean = false,

  // Performance
  val batterySaverMode: Boolean = false,
  val reduceAnimations: Boolean = false,
  val reduceEffects: Boolean = false,
) {
  val effectiveAnimations: Boolean
    get() = animationsEnabled && !batterySaverMode && !reduceAnimations

  val effectiveParticles: Boolean
    get() = particlesEnabled && !batterySaverMode && !reduceEffects

  val effectiveVisualEffect: VisualEffectStyle
    get() = if (batterySaverMode || reduceEffects) VisualEffectStyle.NONE else visualEffectStyle
}

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
    val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
    val WALLPAPER_BLUR = floatPreferencesKey("wallpaper_blur")
    val ACCENT_PRESET = stringPreferencesKey("accent_preset")
    val VISUAL_EFFECT = stringPreferencesKey("visual_effect")
    val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
    val PARTICLES_ENABLED = booleanPreferencesKey("particles_enabled")

    val INTRO_ENABLED = booleanPreferencesKey("intro_enabled")
    val INTRO_VOICE = booleanPreferencesKey("intro_voice")
    val INTRO_SUBTITLE = booleanPreferencesKey("intro_subtitle")
    val INTRO_DURATION = floatPreferencesKey("intro_duration")

    val AUTOPLAY = booleanPreferencesKey("autoplay")
    val SHUFFLE = booleanPreferencesKey("shuffle")
    val REPEAT_MODE = intPreferencesKey("repeat_mode")
    val RESUME_LAST = booleanPreferencesKey("resume_last")
    val CROSSFADE = booleanPreferencesKey("crossfade")
    val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
    val LAST_TRACK_URI = stringPreferencesKey("last_track_uri")
    val LAST_POSITION_MS = longPreferencesKey("last_position_ms")

    val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
    val EQ_PRESET = stringPreferencesKey("eq_preset")
    val EQ_BASS = floatPreferencesKey("eq_bass")
    val EQ_MID = floatPreferencesKey("eq_mid")
    val EQ_TREBLE = floatPreferencesKey("eq_treble")

    val DEFAULT_SORT = stringPreferencesKey("default_sort")
    val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")
    val HIDE_SHORT_CLIPS = booleanPreferencesKey("hide_short_clips")

    val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
    val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    val REDUCE_EFFECTS = booleanPreferencesKey("reduce_effects")
  }

  val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
    AppSettings(
      customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH],
      wallpaperDimIntensity = prefs[Keys.WALLPAPER_DIM] ?: 0.58f,
      wallpaperBlurRadius = prefs[Keys.WALLPAPER_BLUR] ?: 0f,
      accentPreset = AccentThemePreset.fromId(prefs[Keys.ACCENT_PRESET] ?: "saiyan_blue"),
      visualEffectStyle = VisualEffectStyle.fromId(prefs[Keys.VISUAL_EFFECT] ?: "energy_aura"),
      animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
      particlesEnabled = prefs[Keys.PARTICLES_ENABLED] ?: true,

      introEnabled = prefs[Keys.INTRO_ENABLED] ?: true,
      introVoiceEnabled = prefs[Keys.INTRO_VOICE] ?: true,
      introSubtitleEnabled = prefs[Keys.INTRO_SUBTITLE] ?: true,
      introDurationSec = (prefs[Keys.INTRO_DURATION] ?: 4.5f).coerceIn(3.0f, 6.0f),

      autoplayEnabled = prefs[Keys.AUTOPLAY] ?: true,
      shuffleEnabled = prefs[Keys.SHUFFLE] ?: false,
      repeatMode = prefs[Keys.REPEAT_MODE] ?: 0,
      resumeLastPlayback = prefs[Keys.RESUME_LAST] ?: true,
      crossfadeEnabled = prefs[Keys.CROSSFADE] ?: true,
      backgroundPlaybackEnabled = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
      lastTrackUri = prefs[Keys.LAST_TRACK_URI],
      lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,

      eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
      eqPreset = EqPreset.fromId(prefs[Keys.EQ_PRESET] ?: "anime"),
      eqBassLevel = prefs[Keys.EQ_BASS] ?: 0.2f,
      eqMidLevel = prefs[Keys.EQ_MID] ?: 0.0f,
      eqTrebleLevel = prefs[Keys.EQ_TREBLE] ?: 0.25f,

      defaultSortOrder = SortOrder.fromId(prefs[Keys.DEFAULT_SORT] ?: "title"),
      hiddenFolders = prefs[Keys.HIDDEN_FOLDERS] ?: emptySet(),
      hideShortClips = prefs[Keys.HIDE_SHORT_CLIPS] ?: false,

      batterySaverMode = prefs[Keys.BATTERY_SAVER] ?: false,
      reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
      reduceEffects = prefs[Keys.REDUCE_EFFECTS] ?: false,
    )
  }

  suspend fun updateWallpaperPath(path: String?) {
    context.settingsDataStore.edit { prefs ->
      if (path == null) prefs.remove(Keys.CUSTOM_WALLPAPER_PATH)
      else prefs[Keys.CUSTOM_WALLPAPER_PATH] = path
    }
  }

  suspend fun setWallpaperDim(dim: Float) = edit { it[Keys.WALLPAPER_DIM] = dim.coerceIn(0f, 0.92f) }
  suspend fun setWallpaperBlur(blur: Float) = edit { it[Keys.WALLPAPER_BLUR] = blur.coerceIn(0f, 24f) }
  suspend fun setAccentPreset(preset: AccentThemePreset) = edit { it[Keys.ACCENT_PRESET] = preset.id }
  suspend fun setVisualEffect(style: VisualEffectStyle) = edit { it[Keys.VISUAL_EFFECT] = style.id }
  suspend fun setAnimationsEnabled(enabled: Boolean) = edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
  suspend fun setParticlesEnabled(enabled: Boolean) = edit { it[Keys.PARTICLES_ENABLED] = enabled }

  suspend fun setIntroEnabled(enabled: Boolean) = edit { it[Keys.INTRO_ENABLED] = enabled }
  suspend fun setIntroVoiceEnabled(enabled: Boolean) = edit { it[Keys.INTRO_VOICE] = enabled }
  suspend fun setIntroSubtitleEnabled(enabled: Boolean) = edit { it[Keys.INTRO_SUBTITLE] = enabled }
  suspend fun setIntroDuration(sec: Float) = edit { it[Keys.INTRO_DURATION] = sec.coerceIn(3.0f, 6.0f) }

  suspend fun setAutoplayEnabled(enabled: Boolean) = edit { it[Keys.AUTOPLAY] = enabled }
  suspend fun setShuffleEnabled(enabled: Boolean) = edit { it[Keys.SHUFFLE] = enabled }
  suspend fun setRepeatMode(mode: Int) = edit { it[Keys.REPEAT_MODE] = mode }
  suspend fun setResumeLastPlayback(enabled: Boolean) = edit { it[Keys.RESUME_LAST] = enabled }
  suspend fun setCrossfadeEnabled(enabled: Boolean) = edit { it[Keys.CROSSFADE] = enabled }
  suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) = edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
  suspend fun saveLastPlayback(uri: String?, positionMs: Long) {
    context.settingsDataStore.edit { prefs ->
      if (uri != null) prefs[Keys.LAST_TRACK_URI] = uri
      prefs[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
    }
  }

  suspend fun setEqEnabled(enabled: Boolean) = edit { it[Keys.EQ_ENABLED] = enabled }
  suspend fun setEqPreset(preset: EqPreset) = edit { it[Keys.EQ_PRESET] = preset.id }
  suspend fun setEqBands(bass: Float, mid: Float, treble: Float) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.EQ_BASS] = bass.coerceIn(-1f, 1f)
      prefs[Keys.EQ_MID] = mid.coerceIn(-1f, 1f)
      prefs[Keys.EQ_TREBLE] = treble.coerceIn(-1f, 1f)
    }
  }

  suspend fun setDefaultSortOrder(sort: SortOrder) = edit { it[Keys.DEFAULT_SORT] = sort.id }
  suspend fun toggleFolderHidden(folderPath: String) {
    context.settingsDataStore.edit { prefs ->
      val current = prefs[Keys.HIDDEN_FOLDERS]?.toMutableSet() ?: mutableSetOf()
      if (current.contains(folderPath)) current.remove(folderPath) else current.add(folderPath)
      prefs[Keys.HIDDEN_FOLDERS] = current
    }
  }
  suspend fun setHideShortClips(hide: Boolean) = edit { it[Keys.HIDE_SHORT_CLIPS] = hide }

  suspend fun setBatterySaverMode(enabled: Boolean) = edit { it[Keys.BATTERY_SAVER] = enabled }
  suspend fun setReduceAnimations(enabled: Boolean) = edit { it[Keys.REDUCE_ANIMATIONS] = enabled }
  suspend fun setReduceEffects(enabled: Boolean) = edit { it[Keys.REDUCE_EFFECTS] = enabled }

  private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
    context.settingsDataStore.edit(block)
  }
}
