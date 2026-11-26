package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AppSettings
import com.example.data.model.VisualEffectStyle
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class BackdropParticle(
  val xFraction: Float,
  val yStartFraction: Float,
  val speed: Float,
  val radius: Float,
  val alpha: Float,
  val isPurple: Boolean,
)

@Composable
fun DbzBackdrop(
  settings: AppSettings,
  modifier: Modifier = Modifier,
) {
  val customFile = remember(settings.customWallpaperPath) {
    settings.customWallpaperPath?.let { File(it) }?.takeIf { it.exists() }
  }

  val blurModifier = if (settings.wallpaperBlurRadius > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Modifier.blur(settings.wallpaperBlurRadius.dp)
  } else {
    Modifier
  }

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  Box(modifier = modifier.fillMaxSize().background(Color(0xFF040712))) {
    if (customFile != null) {
      AsyncImage(
        model = customFile,
        contentDescription = "Plano de fundo personalizado",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().then(blurModifier),
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_dbz_default_bg),
        contentDescription = "Plano de fundo Dragon Ball Z",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().then(blurModifier),
      )
    }

    // Extra frosted veil on older Android versions when blur is requested
    val compatFrostAlpha = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && settings.wallpaperBlurRadius > 0.5f) {
      (settings.wallpaperBlurRadius / 24f) * 0.28f
    } else {
      0f
    }

    // Dimming scrim to preserve control legibility
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF040712).copy(alpha = (settings.wallpaperDimIntensity + compatFrostAlpha).coerceIn(0.15f, 0.94f)),
              Color(0xFF060C1E).copy(alpha = (settings.wallpaperDimIntensity * 0.92f + compatFrostAlpha).coerceIn(0.15f, 0.94f)),
              Color(0xFF040712).copy(alpha = (settings.wallpaperDimIntensity + 0.14f).coerceIn(0.25f, 0.96f)),
            )
          )
        )
    )

    val effectStyle = settings.effectiveVisualEffect
    val showParticles = settings.effectiveParticles
    val animate = settings.effectiveAnimations

    if (effectStyle != VisualEffectStyle.NONE || showParticles) {
      val infiniteTransition = rememberInfiniteTransition(label = "backdrop_fx")
      val phase by if (animate) {
        infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
          label = "fx_phase",
        )
      } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.25f) }
      }

      val particles = remember {
        val rng = Random(42)
        List(20) {
          BackdropParticle(
            xFraction = rng.nextFloat(),
            yStartFraction = rng.nextFloat(),
            speed = 0.5f + rng.nextFloat() * 0.9f,
            radius = 1.8f + rng.nextFloat() * 3.2f,
            alpha = 0.25f + rng.nextFloat() * 0.45f,
            isPurple = rng.nextBoolean(),
          )
        }
      }

      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (effectStyle) {
          VisualEffectStyle.ENERGY_AURA -> {
            val pulse = 0.85f + 0.15f * sin(phase * 2f * PI.toFloat())
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.16f * pulse), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.14f),
                radius = w * 0.72f,
              ),
              radius = w * 0.72f,
              center = Offset(w * 0.18f, h * 0.14f),
            )
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.14f * pulse), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.78f),
                radius = w * 0.68f,
              ),
              radius = w * 0.68f,
              center = Offset(w * 0.88f, h * 0.78f),
            )
          }

          VisualEffectStyle.NEON_PULSE -> {
            val cy = h * (0.2f + 0.5f * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat())))
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.18f), secondary.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.5f, cy),
                radius = w * 0.85f,
              ),
              radius = w * 0.85f,
              center = Offset(w * 0.5f, cy),
            )
          }

          VisualEffectStyle.COSMIC_GRID -> {
            val step = 72.dp.toPx()
            val gridColor = primary.copy(alpha = 0.055f)
            var x = 0f
            while (x < w) {
              drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
              x += step
            }
            var y = (phase * step) % step
            while (y < h) {
              drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
              y += step
            }
          }

          VisualEffectStyle.NONE -> Unit
        }

        if (showParticles) {
          particles.forEach { p ->
            val yFrac = (p.yStartFraction - phase * p.speed + 2f) % 1f
            val xDrift = sin((phase * 2f * PI + p.xFraction * 10f).toDouble()).toFloat() * 14f
            val center = Offset(p.xFraction * w + xDrift, yFrac * h)
            val color = if (p.isPurple) secondary else primary
            drawCircle(
              color = color.copy(alpha = p.alpha),
              radius = p.radius.dp.toPx(),
              center = center,
            )
          }
        }
      }
    }
  }
}
