package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Dragon Ball Z Cyber-Anime Obsidian Palette
val DbzVoidBlack = Color(0xFF040712)
val DbzDeepNavy = Color(0xFF0A1128)
val DbzSurfaceDark = Color(0xFF101936)
val DbzSurfaceElevated = Color(0xFF172347)

// Neon Accents
val ElectricBlue = Color(0xFF00D4FF)
val KiCobalt = Color(0xFF2979FF)
val EnergyPurple = Color(0xFF9D4EDD)
val DeepViolet = Color(0xFF6A1B9A)
val KiGold = Color(0xFFFFB703)
val UltraSilver = Color(0xFFD6E6FF)
val RoseCrimson = Color(0xFFFF3E7F)

// Text & Glass Tokens
val TextPrimary = Color(0xFFF3F7FF)
val TextSecondary = Color(0xFFA8B8D8)
val TextMuted = Color(0xFF6B7C9E)
val GlassBorder = Color(0x3800D4FF)
val GlassBackground = Color(0xAA0C1530)

enum class AccentThemePreset(
  val id: String,
  val label: String,
  val primary: Color,
  val secondary: Color,
  val tertiary: Color,
) {
  SAIYAN_BLUE(
    id = "saiyan_blue",
    label = "Azul Super Saiyajin",
    primary = ElectricBlue,
    secondary = EnergyPurple,
    tertiary = KiGold,
  ),
  ULTRA_INSTINCT(
    id = "ultra_instinct",
    label = "Instinto Superior",
    primary = UltraSilver,
    secondary = Color(0xFF7B8CDE),
    tertiary = ElectricBlue,
  ),
  VEGETA_ROYAL(
    id = "vegeta_royal",
    label = "Aura Real Púrpura",
    primary = EnergyPurple,
    secondary = ElectricBlue,
    tertiary = RoseCrimson,
  ),
  SUPER_SAIYAN_GOLD(
    id = "super_saiyan_gold",
    label = "Ki Dourado",
    primary = KiGold,
    secondary = ElectricBlue,
    tertiary = EnergyPurple,
  );

  companion object {
    fun fromId(id: String): AccentThemePreset =
      entries.firstOrNull { it.id == id } ?: SAIYAN_BLUE
  }
}
