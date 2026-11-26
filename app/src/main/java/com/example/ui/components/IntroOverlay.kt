package com.example.ui.components

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ChakraPetchFamily
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EnergyPurple
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class IntroParticle(
  val x: Float,
  val y: Float,
  val speed: Float,
  val size: Float,
  val isBlue: Boolean,
)

@Composable
fun IntroOverlay(
  durationSeconds: Float,
  voiceEnabled: Boolean,
  subtitleEnabled: Boolean,
  onFinished: () -> Unit,
) {
  val context = LocalContext.current
  val introPhrase = stringResource(id = R.string.intro_phrase)
  val totalMs = (durationSeconds.coerceIn(3.0f, 6.0f) * 1000L).toLong()

  val masterAlpha = remember { Animatable(0f) }
  val zoomScale = remember { Animatable(1.0f) }
  var showSubtitle by remember { mutableStateOf(false) }

  // Optional male-pitched TextToSpeech voice with graceful non-blocking fallback
  DisposableEffect(voiceEnabled) {
    var tts: TextToSpeech? = null
    if (voiceEnabled) {
      try {
        tts = TextToSpeech(context.applicationContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            try {
              val ptBr = Locale("pt", "BR")
              val langResult = tts?.setLanguage(ptBr)
              if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
              }
              // Deeper masculine pitch and firm pacing
              tts?.setPitch(0.72f)
              tts?.setSpeechRate(0.92f)
              val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
              }
              tts?.speak(introPhrase, TextToSpeech.QUEUE_FLUSH, params, "dbz_intro_tts")
            } catch (_: Exception) {
            }
          }
        }
      } catch (_: Exception) {
      }
    }
    onDispose {
      runCatching {
        tts?.stop()
        tts?.shutdown()
      }
    }
  }

  LaunchedEffect(totalMs) {
    masterAlpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    delay(250L)
    showSubtitle = true
    val zoomMs = (totalMs - 1100L).coerceAtLeast(1500L).toInt()
    zoomScale.animateTo(1.15f, animationSpec = tween(zoomMs, easing = FastOutSlowInEasing))
    masterAlpha.animateTo(0f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    onFinished()
  }

  val infinite = rememberInfiniteTransition(label = "intro_ki")
  val auraPulse by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "aura_pulse",
  )

  val particles = remember {
    val rng = Random(77)
    List(32) {
      IntroParticle(
        x = rng.nextFloat(),
        y = rng.nextFloat(),
        speed = 0.7f + rng.nextFloat() * 1.3f,
        size = 2f + rng.nextFloat() * 4.5f,
        isBlue = rng.nextFloat() > 0.25f,
      )
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .alpha(masterAlpha.value),
  ) {
    // Fullscreen DBZ artwork with smooth zoom-in
    Image(
      painter = painterResource(id = R.drawable.img_dbz_intro),
      contentDescription = "Abertura Dragon Ball Z",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = zoomScale.value
          scaleY = zoomScale.value
        },
    )

    // Cinematic vignette
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xAA020612),
              Color(0x22040B20),
              Color(0xCC030612),
            )
          )
        )
    )

    // Blue Ki energy aura rings, light flare, and rising particles
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.45f
      val maxR = size.minDimension * 0.48f

      // Glowing blue Ki core aura
      val wave = 0.85f + 0.15f * sin(auraPulse * 2f * PI.toFloat())
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            ElectricBlue.copy(alpha = 0.34f * wave),
            EnergyPurple.copy(alpha = 0.18f),
            Color.Transparent,
          ),
          center = Offset(cx, cy),
          radius = maxR * 1.4f,
        ),
        radius = maxR * 1.4f,
        center = Offset(cx, cy),
      )

      // Expanding Ki shockwave rings
      for (i in 0..2) {
        val progress = (auraPulse + i * 0.33f) % 1f
        val ringRadius = maxR * (0.35f + progress * 0.95f)
        val ringAlpha = (1f - progress) * 0.55f
        drawCircle(
          color = ElectricBlue.copy(alpha = ringAlpha),
          radius = ringRadius,
          center = Offset(cx, cy),
          style = Stroke(width = (3.5f * (1f - progress) + 1f).dp.toPx(), cap = StrokeCap.Round),
        )
      }

      // Luminous rising Ki particles
      particles.forEach { p ->
        val yPos = ((p.y - auraPulse * p.speed + 2f) % 1f) * size.height
        val xPos = p.x * size.width + sin((auraPulse * 2f * PI + p.x * 8f).toDouble()).toFloat() * 12f
        val color = if (p.isBlue) ElectricBlue else EnergyPurple
        drawCircle(
          color = color.copy(alpha = 0.72f),
          radius = p.size.dp.toPx(),
          center = Offset(xPos, yPos),
        )
      }

      // Sweeping diagonal light flare
      val flareX = size.width * (-0.3f + auraPulse * 1.6f)
      drawLine(
        brush = Brush.linearGradient(
          colors = listOf(Color.Transparent, ElectricBlue.copy(alpha = 0.28f), Color.Transparent),
          start = Offset(flareX - 100f, 0f),
          end = Offset(flareX + 100f, size.height),
        ),
        start = Offset(flareX - 60f, 0f),
        end = Offset(flareX + 60f, size.height),
        strokeWidth = 90.dp.toPx(),
      )
    }

    // Skip button
    TextButton(
      onClick = onFinished,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .safeDrawingPadding()
        .padding(16.dp)
        .testTag("intro_skip_button"),
    ) {
      Text(
        text = "Pular",
        color = Color.White.copy(alpha = 0.8f),
        style = MaterialTheme.typography.labelLarge,
      )
    }

    // Synchronized glowing subtitle
    if (subtitleEnabled) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .safeDrawingPadding()
          .padding(horizontal = 24.dp, vertical = 56.dp),
      ) {
        AnimatedVisibility(
          visible = showSubtitle,
          enter = fadeIn(tween(420)) + scaleIn(
            initialScale = 0.82f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
          ),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Color(0xBB050B1E),
                    Color(0xDD0B193C),
                    Color(0xBB050B1E),
                  )
                )
              )
              .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                  colors = listOf(EnergyPurple.copy(alpha = 0.8f), ElectricBlue, EnergyPurple.copy(alpha = 0.8f))
                ),
                shape = RoundedCornerShape(20.dp),
              )
              .padding(horizontal = 24.dp, vertical = 18.dp),
          ) {
            Text(
              text = introPhrase.uppercase(),
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = ChakraPetchFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                shadow = Shadow(
                  color = ElectricBlue.copy(alpha = 0.9f),
                  offset = Offset(0f, 0f),
                  blurRadius = 22f,
                ),
              ),
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .width(120.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, ElectricBlue, EnergyPurple, Color.Transparent)
                  )
                )
            )
          }
        }
      }
    }
  }
}
