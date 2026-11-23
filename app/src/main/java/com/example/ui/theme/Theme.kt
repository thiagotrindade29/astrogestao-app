package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AstroExtendedColors(
    val positive: Color,
    val positiveContainer: Color,
    val negative: Color,
    val negativeContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val review: Color,
    val reviewContainer: Color,
    val borderSubtle: Color
)

private val DarkExtendedColors = AstroExtendedColors(
    positive = EmeraldPulse,
    positiveContainer = EmeraldContainer,
    negative = CoralAlert,
    negativeContainer = CoralContainer,
    info = OrbitalCyan,
    infoContainer = OrbitalCyanContainer,
    review = VioletReview,
    reviewContainer = VioletContainer,
    borderSubtle = BorderIndigo
)

private val LightExtendedColors = AstroExtendedColors(
    positive = LightEmerald,
    positiveContainer = LightEmeraldContainer,
    negative = LightCoral,
    negativeContainer = LightCoralContainer,
    info = DeepTeal,
    infoContainer = DeepTealContainer,
    review = RoyalIndigo,
    reviewContainer = RoyalIndigoContainer,
    borderSubtle = LightBorder
)

val LocalAstroColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
    primary = AstralGold,
    onPrimary = OnAstralGold,
    primaryContainer = AstralGoldContainer,
    onPrimaryContainer = SoftGoldText,
    secondary = OrbitalCyan,
    onSecondary = MidnightVoid,
    secondaryContainer = OrbitalCyanContainer,
    onSecondaryContainer = SoftCyanText,
    tertiary = VioletReview,
    onTertiary = MidnightVoid,
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = StarlightWhite,
    background = MidnightVoid,
    onBackground = StarlightWhite,
    surface = DeepSpace,
    onSurface = StarlightWhite,
    surfaceVariant = SlateNebula,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = MidnightVoid,
    surfaceContainerLow = DeepSpace,
    surfaceContainer = SlateNebula,
    surfaceContainerHigh = ElevatedIndigo,
    outline = BorderIndigo,
    outlineVariant = SlateNebula,
    error = CoralAlert,
    onError = StarlightWhite,
    errorContainer = CoralContainer,
    onErrorContainer = StarlightWhite
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalIndigo,
    onPrimary = CrispWhite,
    primaryContainer = RoyalIndigoContainer,
    onPrimaryContainer = RoyalIndigo,
    secondary = WarmAmber,
    onSecondary = CrispWhite,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = WarmAmber,
    tertiary = DeepTeal,
    onTertiary = CrispWhite,
    tertiaryContainer = DeepTealContainer,
    onTertiaryContainer = DeepTeal,
    background = IvoryCanvas,
    onBackground = InkDark,
    surface = CrispWhite,
    onSurface = InkDark,
    surfaceVariant = SoftMist,
    onSurfaceVariant = SlateText,
    surfaceContainerLowest = CrispWhite,
    surfaceContainerLow = IvoryCanvas,
    surfaceContainer = SoftMist,
    surfaceContainerHigh = Color(0xFFE4E9F4),
    outline = LightBorder,
    outlineVariant = SoftMist,
    error = LightCoral,
    onError = CrispWhite,
    errorContainer = LightCoralContainer,
    onErrorContainer = LightCoral
)

object AstroTheme {
    val extended: AstroExtendedColors
        @Composable get() = LocalAstroColors.current
}

@Composable
fun AstroGestaoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalAstroColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstroTypography,
            shapes = AstroShapes,
            content = content
        )
    }
}
