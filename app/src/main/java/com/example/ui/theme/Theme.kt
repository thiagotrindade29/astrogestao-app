package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class AstroExtendedColors(
    val positive: Color,
    val positiveContainer: Color,
    val negative: Color,
    val negativeContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val borderSubtle: Color,
    val kpiNavy: Color,
    val kpiBlue: Color,
    val kpiEmerald: Color
)

private val LightExtendedColors = AstroExtendedColors(
    positive = PositiveEmerald,
    positiveContainer = PositiveEmeraldBg,
    negative = AlertRed,
    negativeContainer = AlertRedContainer,
    warning = LowStockAmberText,
    warningContainer = LowStockAmberBg,
    info = AdjustmentBlue,
    infoContainer = AdjustmentBlueBg,
    borderSubtle = BorderLight,
    kpiNavy = KpiNavyBg,
    kpiBlue = KpiBlueBg,
    kpiEmerald = KpiEmeraldBg
)

private val DarkExtendedColors = AstroExtendedColors(
    positive = Color(0xFF34D399),
    positiveContainer = Color(0xFF064E3B),
    negative = Color(0xFFFB7185),
    negativeContainer = Color(0xFF4C0519),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF451A03),
    info = Color(0xFF60A5FA),
    infoContainer = Color(0xFF1E3A8A),
    borderSubtle = DarkSpaceBorder,
    kpiNavy = Color(0xFF162040),
    kpiBlue = Color(0xFF1E40AF),
    kpiEmerald = Color(0xFF065F46)
)

val LocalAstroColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = CosmicPurple,
    onPrimary = Color.White,
    primaryContainer = CosmicPurpleLight,
    onPrimaryContainer = CosmicPurple,
    secondary = KpiBlueBg,
    onSecondary = Color.White,
    secondaryContainer = AdjustmentBlueBg,
    onSecondaryContainer = AdjustmentBlue,
    tertiary = KpiEmeraldBg,
    onTertiary = Color.White,
    background = WorkspaceBg,
    onBackground = InkPrimary,
    surface = CardWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = SlateMuted,
    outline = BorderLight,
    outlineVariant = SurfaceMuted,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPurple,
    onPrimary = DarkSpaceBg,
    primaryContainer = DarkPurpleContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF60A5FA),
    onSecondary = DarkSpaceBg,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF34D399),
    onTertiary = DarkSpaceBg,
    background = DarkSpaceBg,
    onBackground = DarkStarlight,
    surface = DarkSpaceSurface,
    onSurface = DarkStarlight,
    surfaceVariant = DarkSpaceElevated,
    onSurfaceVariant = DarkMuted,
    outline = DarkSpaceBorder,
    outlineVariant = DarkSpaceElevated,
    error = Color(0xFFFB7185),
    onError = DarkSpaceBg,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFFE4E6)
)

object AstroTheme {
    val extended: AstroExtendedColors
        @Composable get() = LocalAstroColors.current
}

@Composable
fun AstroGestaoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAstroColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstroTypography,
            shapes = AstroShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
