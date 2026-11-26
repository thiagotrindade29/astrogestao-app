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
    val isDark: Boolean,
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
    val kpiNavyContent: Color,
    val kpiBlue: Color,
    val kpiBlueContent: Color,
    val kpiEmerald: Color,
    val kpiEmeraldContent: Color,
    val kpiAccentBar: Color,
    val kpiAccentText: Color
)

private val LightExtendedColors = AstroExtendedColors(
    isDark = false,
    // Apenas Índigo Noturno (60%), Pêssego Claro (30%) e Ciano Elétrico (10%)
    positive = CianoEletricoInk,
    positiveContainer = CianoEletricoContainer,
    negative = IndigoNoturno,
    negativeContainer = PessegoClaro,
    warning = IndigoNoturnoElevated,
    warningContainer = PessegoClaroSoft,
    info = IndigoNoturno,
    infoContainer = PessegoClaroSoft,
    borderSubtle = PessegoClaroBorder,
    kpiNavy = IndigoNoturno,
    kpiNavyContent = PessegoClaroWorkspace,
    kpiBlue = IndigoNoturnoElevated,
    kpiBlueContent = PessegoClaroWorkspace,
    kpiEmerald = PessegoClaro,
    kpiEmeraldContent = IndigoNoturno,
    kpiAccentBar = CianoEletrico,
    kpiAccentText = CianoEletricoInk
)

private val DarkExtendedColors = AstroExtendedColors(
    isDark = true,
    // Apenas Azul Sideral (60%), Prata de Níquel (30%) e Lilás Órion (10%)
    positive = LilasOrionSoft,
    positiveContainer = LilasOrionContainer,
    negative = AzulSideralBg,
    negativeContainer = PrataNiquel,
    warning = PrataNiquel,
    warningContainer = AzulSideralElevated,
    info = PrataNiquelLight,
    infoContainer = PrataNiquelContainer,
    borderSubtle = AzulSideralBorder,
    kpiNavy = AzulSideralElevated,
    kpiNavyContent = PrataNiquelLight,
    kpiBlue = AzulSideralSurface,
    kpiBlueContent = PrataNiquelLight,
    kpiEmerald = PrataNiquel,
    kpiEmeraldContent = AzulSideralBg,
    kpiAccentBar = LilasOrion,
    kpiAccentText = LilasOrionSoft
)

val LocalAstroColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = IndigoNoturno,
    onPrimary = PessegoClaroWorkspace,
    primaryContainer = PessegoClaro,
    onPrimaryContainer = IndigoNoturno,
    secondary = PessegoClaro,
    onSecondary = IndigoNoturno,
    secondaryContainer = PessegoClaroSoft,
    onSecondaryContainer = IndigoNoturno,
    tertiary = CianoEletrico,
    onTertiary = IndigoNoturno,
    tertiaryContainer = CianoEletricoContainer,
    onTertiaryContainer = IndigoNoturno,
    background = PessegoClaroWorkspace,
    onBackground = IndigoNoturno,
    surface = PessegoClaroCard,
    onSurface = IndigoNoturno,
    surfaceVariant = PessegoClaroSoft,
    onSurfaceVariant = IndigoNoturnoMuted,
    outline = PessegoClaroBorder,
    outlineVariant = PessegoClaroSoft,
    error = IndigoNoturno,
    onError = PessegoClaroWorkspace,
    errorContainer = PessegoClaro,
    onErrorContainer = IndigoNoturno
)

private val DarkColorScheme = darkColorScheme(
    primary = LilasOrion,
    onPrimary = AzulSideralBg,
    primaryContainer = LilasOrionContainer,
    onPrimaryContainer = LilasOrionSoft,
    secondary = PrataNiquel,
    onSecondary = AzulSideralBg,
    secondaryContainer = PrataNiquelContainer,
    onSecondaryContainer = PrataNiquelLight,
    tertiary = LilasOrionSoft,
    onTertiary = AzulSideralBg,
    tertiaryContainer = AzulSideralElevated,
    onTertiaryContainer = PrataNiquelLight,
    background = AzulSideralBg,
    onBackground = PrataNiquelLight,
    surface = AzulSideralSurface,
    onSurface = PrataNiquelLight,
    surfaceVariant = AzulSideralElevated,
    onSurfaceVariant = PrataNiquelMuted,
    outline = AzulSideralBorder,
    outlineVariant = AzulSideralElevated,
    error = PrataNiquel,
    onError = AzulSideralBg,
    errorContainer = PrataNiquelContainer,
    onErrorContainer = PrataNiquelLight
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
