package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.CyanAura
import com.example.ui.theme.DeepCosmicNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleViolet

@Composable
fun TrackArtwork(
    albumArtUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val context = LocalContext.current
    val fallbackPainter = painterResource(id = R.drawable.img_default_album_cover)

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(albumArtUri ?: R.drawable.img_default_album_cover)
            .crossfade(true)
            .build(),
        placeholder = fallbackPainter,
        error = fallbackPainter,
        fallback = fallbackPainter,
        contentDescription = contentDescription ?: stringResource(R.string.cd_album_cover),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(DeepCosmicNavy),
    )
}

@Composable
fun RotatingAlbumDisc(
    albumArtUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTimeNanos = withFrameNanos { it }
            while (true) {
                val frameTimeNanos = withFrameNanos { it }
                val deltaSeconds = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = frameTimeNanos
                val nextAngle = (rotation.value + deltaSeconds * 16f) % 360f
                rotation.snapTo(nextAngle)
            }
        }
    }

    val discScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.93f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "disc_scale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = modifier.scale(discScale),
        contentAlignment = Alignment.Center,
    ) {
        // Outer luminous ki aura ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.dp.toPx()
            val radius = (minOf(size.width, size.height) - strokePx) / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = if (isPlaying) 0.34f * pulseAlpha else 0.14f),
                        SubtleViolet.copy(alpha = if (isPlaying) 0.20f * pulseAlpha else 0.08f),
                        Color.Transparent,
                    )
                ),
                radius = radius * 1.12f,
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = if (isPlaying) pulseAlpha else 0.38f),
                        SubtleViolet.copy(alpha = if (isPlaying) pulseAlpha * 0.8f else 0.25f),
                        CyanAura.copy(alpha = if (isPlaying) pulseAlpha else 0.38f),
                        ElectricBlue.copy(alpha = if (isPlaying) pulseAlpha else 0.38f),
                    )
                ),
                radius = radius - 6.dp.toPx(),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        // Rotating circular artwork with subtle inner titanium rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(CyanAura.copy(alpha = 0.65f), SubtleViolet.copy(alpha = 0.55f))
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            TrackArtwork(
                albumArtUri = albumArtUri,
                contentDescription = stringResource(R.string.cd_album_cover),
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
            )

            // Subtle vinyl/capsule center hub
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ObsidianBlack.copy(alpha = 0.78f))
                    .border(1.5.dp, CyanAura.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}

@Composable
fun EqualizerIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq3",
    )

    Row(
        modifier = modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
        listOf(bar1, bar2, bar3).forEach { level ->
            val heightFraction = if (isPlaying) level else 0.32f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16f * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(CyanAura, ElectricBlue))
                    )
            )
        }
    }
}
