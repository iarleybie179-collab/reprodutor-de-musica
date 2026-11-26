package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DbzDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepAuraBlue,
    onPrimaryContainer = CrispIceWhite,
    secondary = SubtleViolet,
    onSecondary = CrispIceWhite,
    secondaryContainer = DarkTitaniumElevated,
    onSecondaryContainer = SoftVioletGlow,
    tertiary = CyanAura,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = CrispIceWhite,
    surface = DeepCosmicNavy,
    onSurface = CrispIceWhite,
    surfaceVariant = DarkTitaniumSurface,
    onSurfaceVariant = CoolSilverText,
    outline = GlassBorderBlue,
    outlineVariant = GlassBorderViolet,
    error = ErrorCrimson,
    onError = CrispIceWhite,
)

private val DbzShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ReprodutorDeMusicaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DbzDarkColorScheme,
        typography = AppTypography,
        shapes = DbzShapes,
        content = content,
    )
}
