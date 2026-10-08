package com.example.allufinancas.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val AlluNavy = Color(0xFF00004D)
internal val AlluIndigo = Color(0xFF00006B)
internal val AlluInk = Color(0xFF101828)
internal val AlluCanvas = Color(0xFFF9FAFB)
internal val AlluTint = Color(0xFFE9E9F5)
internal val AlluMuted = Color(0xFF667085)
internal val AlluGreen = Color(0xFF087443)
internal val AlluAmber = Color(0xFF9A5B00)
internal val AlluSuccess = Color(0xFF087443)
internal val AlluWarning = Color(0xFF9A5B00)
internal val AlluCritical = Color(0xFFB42318)
internal val AlluNeutral = Color(0xFF475467)

private val LightColors =
    lightColorScheme(
        primary = AlluIndigo,
        onPrimary = Color.White,
        primaryContainer = AlluTint,
        onPrimaryContainer = AlluNavy,
        secondary = Color(0xFF8A4F00),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE1B5),
        onSecondaryContainer = Color(0xFF4A2800),
        background = AlluCanvas,
        onBackground = AlluInk,
        surface = Color.White,
        onSurface = AlluInk,
        outline = Color(0xFFD0D5DD),
        error = AlluCritical,
        errorContainer = Color(0xFFFEE4E2),
        onErrorContainer = Color(0xFF7A271A),
        tertiary = AlluSuccess,
        tertiaryContainer = Color(0xFFDCFCE7),
        onTertiaryContainer = Color(0xFF14532D),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFC3C3F2),
        onPrimary = AlluNavy,
        primaryContainer = AlluIndigo,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFFB95C),
        onSecondary = Color(0xFF4A2800),
        secondaryContainer = Color(0xFF6B4500),
        onSecondaryContainer = Color(0xFFFFDDB0),
        background = Color(0xFF0F172A),
        onBackground = Color(0xFFF2F4F7),
        surface = Color(0xFF182338),
        onSurface = Color(0xFFF2F4F7),
        outline = Color(0xFF667085),
        error = Color(0xFFFDA29B),
        errorContainer = Color(0xFF7A271A),
        onErrorContainer = Color(0xFFFEE4E2),
        tertiary = Color(0xFF6CE9A6),
        tertiaryContainer = Color(0xFF14532D),
        onTertiaryContainer = Color(0xFFDCFCE7),
    )

private val AlluTypography =
    androidx.compose.material3.Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                fontFeatureSettings = "tnum",
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium =
            TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )

internal val LocalAlluReduceMotion = compositionLocalOf { false }

@Composable
internal fun AlluTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalAlluReduceMotion provides reduceMotion) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AlluTypography,
        content = content,
    )
  }
}
