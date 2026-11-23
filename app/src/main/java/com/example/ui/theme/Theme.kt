package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AstroSemanticColors(
    val pageBackground: Color,
    val cardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val primary: Color,
    val spaceDark: Color,
    val success: Color,
    val successBg: Color,
    val warning: Color,
    val warningBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val info: Color,
    val infoBg: Color
)

private val LightSemanticColors = AstroSemanticColors(
    pageBackground = LightPageBg,
    cardSurface = LightSurface,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    border = LightBorder,
    primary = BrandPurple,
    spaceDark = BrandSpaceDark,
    success = SemanticSuccess,
    successBg = SemanticSuccessBgLight,
    warning = SemanticWarning,
    warningBg = SemanticWarningBgLight,
    danger = SemanticDanger,
    dangerBg = SemanticDangerBgLight,
    info = SemanticInfo,
    infoBg = SemanticInfoBgLight
)

private val DarkSemanticColors = AstroSemanticColors(
    pageBackground = DarkPageBg,
    cardSurface = DarkSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    border = DarkBorder,
    primary = BrandPurpleLight,
    spaceDark = BrandSpaceDark,
    success = SemanticSuccess,
    successBg = SemanticSuccessBgDark,
    warning = SemanticWarning,
    warningBg = SemanticWarningBgDark,
    danger = SemanticDanger,
    dangerBg = SemanticDangerBgDark,
    info = SemanticInfo,
    infoBg = SemanticInfoBgDark
)

val LocalAstroColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FA),
    onPrimaryContainer = BrandSpaceDark,
    secondary = SemanticInfo,
    onSecondary = Color.White,
    background = LightPageBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE9EEF7),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = SemanticDanger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = BrandSpaceDark,
    primaryContainer = Color(0xFF33215E),
    onPrimaryContainer = Color(0xFFEADFFD),
    secondary = SemanticInfo,
    onSecondary = Color(0xFF082F49),
    background = DarkPageBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = SemanticDanger,
    onError = Color.White
)

@Composable
fun AstroGestaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalAstroColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
