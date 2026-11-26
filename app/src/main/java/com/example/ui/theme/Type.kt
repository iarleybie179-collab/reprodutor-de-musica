package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val ChakraPetchFamily = FontFamily(
  Font(R.font.chakra_petch, FontWeight.Normal),
  Font(R.font.chakra_petch, FontWeight.SemiBold),
  Font(R.font.chakra_petch, FontWeight.Bold),
)

val PlusJakartaFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val DbzTypography =
  Typography(
    displaySmall = TextStyle(
      fontFamily = ChakraPetchFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 34.sp,
      lineHeight = 40.sp,
      letterSpacing = 0.8.sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = ChakraPetchFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 26.sp,
      lineHeight = 32.sp,
      letterSpacing = 0.5.sp,
    ),
    headlineSmall = TextStyle(
      fontFamily = ChakraPetchFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 21.sp,
      lineHeight = 28.sp,
      letterSpacing = 0.3.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = ChakraPetchFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 19.sp,
      lineHeight = 25.sp,
      letterSpacing = 0.25.sp,
    ),
    titleMedium = TextStyle(
      fontFamily = PlusJakartaFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
      fontFamily = PlusJakartaFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = PlusJakartaFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = PlusJakartaFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = ChakraPetchFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
      fontFamily = PlusJakartaFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      letterSpacing = 0.4.sp,
    ),
  )
