package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DbzShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun DbzMusicTheme(
  accentPreset: AccentThemePreset = AccentThemePreset.SAIYAN_BLUE,
  content: @Composable () -> Unit,
) {
  val colorScheme = darkColorScheme(
    primary = accentPreset.primary,
    onPrimary = DbzVoidBlack,
    primaryContainer = accentPreset.primary.copy(alpha = 0.20f),
    onPrimaryContainer = TextPrimary,
    secondary = accentPreset.secondary,
    onSecondary = TextPrimary,
    secondaryContainer = accentPreset.secondary.copy(alpha = 0.22f),
    onSecondaryContainer = TextPrimary,
    tertiary = accentPreset.tertiary,
    onTertiary = DbzVoidBlack,
    background = DbzVoidBlack,
    onBackground = TextPrimary,
    surface = DbzDeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = DbzSurfaceDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DbzSurfaceDark,
    surfaceContainerHigh = DbzSurfaceElevated,
    outline = accentPreset.primary.copy(alpha = 0.32f),
    outlineVariant = Color(0x28FFFFFF),
    error = RoseCrimson,
    onError = Color.White,
  )

  MaterialTheme(
    colorScheme = colorScheme,
    typography = DbzTypography,
    shapes = DbzShapes,
    content = content,
  )
}
