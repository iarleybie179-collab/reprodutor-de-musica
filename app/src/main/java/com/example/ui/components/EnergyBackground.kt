package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.CyanAura
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleViolet
import kotlin.math.PI
import kotlin.math.sin

private data class EnergyMote(
    val xFraction: Float,
    val yOffset: Float,
    val speedFactor: Float,
    val radiusDp: Float,
    val phase: Float,
    val isViolet: Boolean,
    val baseAlpha: Float,
)

@Composable
fun EnergyBackground(
    modifier: Modifier = Modifier,
    dimIntensity: Float = 0.52f,
) {
    val motes = remember {
        List(18) { index ->
            EnergyMote(
                xFraction = ((index * 37 + 13) % 100) / 100f,
                yOffset = ((index * 53 + 7) % 100) / 100f,
                speedFactor = 0.65f + ((index % 5) * 0.14f),
                radiusDp = 1.6f + (index % 4) * 0.9f,
                phase = index * 0.85f,
                isViolet = index % 4 == 0,
                baseAlpha = 0.18f + (index % 3) * 0.11f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "energy_particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_progress",
    )

    Box(modifier = modifier.fillMaxSize().background(ObsidianBlack)) {
        Image(
            painter = painterResource(id = R.drawable.img_dbz_hero_bg),
            contentDescription = stringResource(R.string.cd_dbz_background),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Multi-stop vertical gradient scrim for crisp contrast and readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to ObsidianBlack.copy(alpha = (dimIntensity + 0.30f).coerceAtMost(0.95f)),
                        0.32f to ObsidianBlack.copy(alpha = dimIntensity * 0.72f),
                        0.60f to ObsidianBlack.copy(alpha = (dimIntensity + 0.18f).coerceAtMost(0.92f)),
                        1.0f to ObsidianBlack.copy(alpha = 0.94f),
                    )
                )
        )

        // Subtle lateral radial vignette + electric ki particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        ObsidianBlack.copy(alpha = 0.55f),
                    ),
                    center = Offset(w * 0.5f, h * 0.38f),
                    radius = maxOf(w, h) * 0.75f,
                )
            )

            motes.forEach { mote ->
                val rawY = (mote.yOffset - progress * mote.speedFactor)
                val normalizedY = ((rawY % 1f) + 1f) % 1f
                val sway = sin((normalizedY * 2f * PI + mote.phase).toDouble()).toFloat() * (w * 0.025f)
                val cx = (mote.xFraction * w + sway).coerceIn(0f, w)
                val cy = normalizedY * h

                // Fade softly near top and bottom edges
                val edgeFade = sin((normalizedY * PI).toDouble()).toFloat().coerceIn(0f, 1f)
                val alpha = mote.baseAlpha * edgeFade
                val color = if (mote.isViolet) SubtleViolet else if (mote.radiusDp > 3f) CyanAura else ElectricBlue
                val radiusPx = mote.radiusDp * density

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 0.95f),
                            color.copy(alpha = alpha * 0.3f),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = radiusPx * 4.2f,
                    ),
                    radius = radiusPx * 4.2f,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = color.copy(alpha = (alpha * 1.4f).coerceAtMost(0.75f)),
                    radius = radiusPx,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}
