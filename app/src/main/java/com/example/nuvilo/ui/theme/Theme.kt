package com.example.nuvilo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NoveloBlue,
    onPrimary = NeutralSurfaceLight,
    primaryContainer = NoveloLightBlueContainer,
    onPrimaryContainer = NoveloOnPrimaryContainer,
    secondary = NoveloCyanAccent,
    onSecondary = NeutralSurfaceLight,
    secondaryContainer = NoveloTealContainer,
    onSecondaryContainer = NoveloOnTealContainer,
    tertiary = NoveloInteractionBlue,
    onTertiary = NeutralSurfaceLight,
    background = NeutralBackgroundLight,
    onBackground = NeutralTextPrimaryLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralTextPrimaryLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralTextSecondaryLight,
    outline = NeutralOutlineLight,
    outlineVariant = NeutralOutlineVariantLight,
    error = StatusMissed,
    onError = NeutralSurfaceLight,
    errorContainer = StatusMissedContainer,
    onErrorContainer = StatusMissed
)

private val DarkColorScheme = darkColorScheme(
    primary = NoveloBlueDark,
    onPrimary = NoveloOnPrimaryDark,
    primaryContainer = NoveloBlueContainerDark,
    onPrimaryContainer = NoveloOnPrimaryContainerDark,
    secondary = NoveloCyanAccentDark,
    onSecondary = NoveloOnTealDark,
    secondaryContainer = NoveloTealContainerDark,
    onSecondaryContainer = NoveloOnTealContainerDark,
    tertiary = NoveloInteractionBlueDark,
    onTertiary = NeutralSurfaceLight,
    background = NeutralBackgroundDark,
    onBackground = NeutralTextPrimaryDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextPrimaryDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralTextSecondaryDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
    error = StatusMissedDark,
    onError = NeutralBackgroundDark,
    errorContainer = StatusMissedContainerDark,
    onErrorContainer = StatusMissedDark
)

val MaterialTheme.noveloColors: NoveloSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNoveloColors.current

@Composable
fun NoveloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkNoveloSemanticColors else LightNoveloSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalNoveloColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NoveloShapes,
            content = content
        )
    }
}
