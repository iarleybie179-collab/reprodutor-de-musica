package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAura
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderBlue
import com.example.ui.theme.GlassBorderViolet
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.SubtleViolet

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    highlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = if (elevated) GlassSurfaceElevated else GlassSurfaceDark
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                CyanAura.copy(alpha = 0.85f),
                ElectricBlue.copy(alpha = 0.65f),
                SubtleViolet.copy(alpha = 0.65f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassBorderBlue,
                GlassBorderViolet,
                Color.White.copy(alpha = 0.08f),
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content,
    )
}

@Composable
fun AuraIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    active: Boolean = false,
    containerColor: Color = Color.White.copy(alpha = 0.07f),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "aura_btn_scale",
    )

    val borderBrush = if (active) {
        Brush.linearGradient(listOf(CyanAura.copy(alpha = 0.8f), SubtleViolet.copy(alpha = 0.7f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f)))
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(if (active) ElectricBlue.copy(alpha = 0.18f) else containerColor)
            .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CyanAura),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
