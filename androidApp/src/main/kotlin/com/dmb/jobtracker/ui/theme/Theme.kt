package com.dmb.jobtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Teal20,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = CoralContainerLight,
    onSecondaryContainer = Color(0xFF3A0A00),
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = ErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainerLight,
    secondary = Coral80,
    onSecondary = Color(0xFF5B1900),
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = CoralContainerLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = Color(0xFFFFB4AB),
)

@Composable
fun JobTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalStatusPalette provides if (darkTheme) DarkStatusPalette else LightStatusPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}