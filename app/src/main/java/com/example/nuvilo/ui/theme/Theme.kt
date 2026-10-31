package com.example.nuvilo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.nuvilo.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = Color(0xFF002B55),
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = DarkBluePrimary,
    onSecondary = Color(0xFF002B55),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1F2B3A),
    error = StatusMissedDark,
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = HealthcareBluePrimary,
    onPrimary = Color.White,
    primaryContainer = HealthcareBlueContainer,
    onPrimaryContainer = HealthcareNavy,
    secondary = HealthcareNavy,
    onSecondary = Color.White,
    secondaryContainer = HealthcareSurfaceVariant,
    onSecondaryContainer = HealthcareTextPrimary,
    background = HealthcareBackground,
    onBackground = HealthcareTextPrimary,
    surface = HealthcareSurface,
    onSurface = HealthcareTextPrimary,
    surfaceVariant = HealthcareSurfaceVariant,
    onSurfaceVariant = HealthcareTextSecondary,
    outline = HealthcareBorder,
    outlineVariant = Color(0xFFF1F5F9),
    error = StatusMissed,
    onError = Color.White
)

@Composable
fun NoveloTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
