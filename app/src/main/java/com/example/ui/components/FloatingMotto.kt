package com.example.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrispIceWhite
import com.example.ui.theme.CyanAura
import com.example.ui.theme.DeepCosmicNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SubtleViolet

@Composable
fun FloatingMotto(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_motto")

    val verticalOffset by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "motto_float_y",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "motto_glow_alpha",
    )

    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .offset(y = verticalOffset.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DeepCosmicNavy.copy(alpha = 0.72f),
                        Color(0xB80E1A33),
                        DeepCosmicNavy.copy(alpha = 0.72f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = glowAlpha * 0.75f),
                        SubtleViolet.copy(alpha = glowAlpha * 0.55f),
                        CyanAura.copy(alpha = glowAlpha * 0.75f),
                    )
                ),
                shape = shape,
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanAura, ElectricBlue.copy(alpha = glowAlpha))
                        )
                    )
            )
            Text(
                text = stringResource(R.string.floating_quote),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    color = CrispIceWhite,
                    shadow = Shadow(
                        color = ElectricBlue.copy(alpha = glowAlpha),
                        offset = Offset(0f, 0f),
                        blurRadius = 18f,
                    ),
                ),
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanAura, SubtleViolet.copy(alpha = glowAlpha))
                        )
                    )
            )
        }
    }
}
