package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.example.data.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
  val isSupported: Boolean = true,
  val isAttached: Boolean = false,
  val bandCount: Int = 0,
  val bandCenterFreqsHz: List<Int> = emptyList(),
  val minLevelMb: Short = -1500,
  val maxLevelMb: Short = 1500,
  val bandLevelsMb: List<Short> = emptyList(),
  val errorMessage: String? = null,
)

class AudioEffectsManager {
  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var currentSessionId: Int = 0

  private var pendingEnabled: Boolean = false
  private var pendingPreset: EqPreset = EqPreset.ANIME
  private var pendingBass: Float = 0.2f
  private var pendingMid: Float = 0f
  private var pendingTreble: Float = 0.25f

  private val _state = MutableStateFlow(EqualizerState())
  val state: StateFlow<EqualizerState> = _state.asStateFlow()

  fun attachToAudioSession(audioSessionId: Int) {
    if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
    releaseEffects()
    currentSessionId = audioSessionId
    try {
      val eq = Equalizer(0, audioSessionId)
      val numBands = eq.numberOfBands.toInt()
      val range = eq.bandLevelRange
      val minMb = range.getOrNull(0) ?: (-1500).toShort()
      val maxMb = range.getOrNull(1) ?: 1500.toShort()
      val freqs = (0 until numBands).map { band -> eq.getCenterFreq(band.toShort()) / 1000 }

      val bb = runCatching { BassBoost(0, audioSessionId) }.getOrNull()

      equalizer = eq
      bassBoost = bb

      _state.value = EqualizerState(
        isSupported = true,
        isAttached = true,
        bandCount = numBands,
        bandCenterFreqsHz = freqs,
        minLevelMb = minMb,
        maxLevelMb = maxMb,
        bandLevelsMb = (0 until numBands).map { eq.getBandLevel(it.toShort()) },
      )
      applySettings(
        enabled = pendingEnabled,
        preset = pendingPreset,
        bass = pendingBass,
        mid = pendingMid,
        treble = pendingTreble,
      )
    } catch (e: Exception) {
      _state.value = EqualizerState(
        isSupported = false,
        isAttached = false,
        errorMessage = "Equalizador de hardware indisponível neste dispositivo.",
      )
    }
  }

  fun applySettings(
    enabled: Boolean,
    preset: EqPreset,
    bass: Float,
    mid: Float,
    treble: Float,
  ) {
    pendingEnabled = enabled
    pendingPreset = preset
    pendingBass = bass
    pendingMid = mid
    pendingTreble = treble

    val eq = equalizer ?: return
    try {
      eq.enabled = enabled
      bassBoost?.enabled = enabled

      val numBands = eq.numberOfBands.toInt()
      if (numBands <= 0) return

      val range = eq.bandLevelRange
      val minMb = range[0].toInt()
      val maxMb = range[1].toInt()

      val (b, m, t) = presetTriplet(preset, bass, mid, treble)

      for (band in 0 until numBands) {
        val ratio = if (numBands == 1) 0.5f else band.toFloat() / (numBands - 1).toFloat()
        val weight = when {
          ratio < 0.35f -> b
          ratio <= 0.68f -> m
          else -> t
        }
        val targetMb = if (weight >= 0f) {
          (weight * maxMb).toInt()
        } else {
          (-weight * minMb).toInt()
        }.coerceIn(minMb, maxMb).toShort()
        eq.setBandLevel(band.toShort(), targetMb)
      }

      bassBoost?.let { bb ->
        if (bb.strengthSupported) {
          val strength = ((b.coerceIn(0f, 1f)) * 700f).toInt().coerceIn(0, 1000).toShort()
          bb.setStrength(strength)
        }
      }

      _state.value = _state.value.copy(
        isAttached = true,
        bandLevelsMb = (0 until numBands).map { eq.getBandLevel(it.toShort()) },
      )
    } catch (_: Exception) {
    }
  }

  private fun presetTriplet(
    preset: EqPreset,
    customBass: Float,
    customMid: Float,
    customTreble: Float,
  ): Triple<Float, Float, Float> = when (preset) {
    EqPreset.NORMAL -> Triple(0f, 0f, 0f)
    EqPreset.ROCK -> Triple(0.55f, -0.1f, 0.5f)
    EqPreset.POP -> Triple(0.25f, 0.35f, 0.3f)
    EqPreset.CLASSICO -> Triple(0.35f, 0.1f, 0.4f)
    EqPreset.ANIME -> Triple(0.60f, 0.15f, 0.55f)
    EqPreset.PERSONALIZADO -> Triple(customBass, customMid, customTreble)
  }

  fun releaseEffects() {
    runCatching { equalizer?.release() }
    runCatching { bassBoost?.release() }
    equalizer = null
    bassBoost = null
    currentSessionId = 0
  }
}
