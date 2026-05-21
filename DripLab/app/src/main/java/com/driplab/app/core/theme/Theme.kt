package com.driplab.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun dripColorsToLightScheme(colors: DripColors) = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.primaryContainer,
    onPrimaryContainer = colors.onPrimaryContainer,
    secondary = colors.secondary,
    onSecondary = colors.onSecondary,
    secondaryContainer = colors.secondaryContainer,
    onSecondaryContainer = colors.onSecondaryContainer,
    tertiary = colors.tertiary,
    onTertiary = colors.onTertiary,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    error = colors.error,
    onError = colors.onError,
    outline = colors.outline
)

private fun dripColorsToDarkScheme(colors: DripColors) = darkColorScheme(
    primary = colors.accent,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.primaryContainer,
    onPrimaryContainer = colors.onPrimaryContainer,
    secondary = colors.accent,
    onSecondary = colors.onSecondary,
    secondaryContainer = colors.secondaryContainer,
    onSecondaryContainer = colors.onSecondaryContainer,
    tertiary = colors.tertiary,
    onTertiary = colors.onTertiary,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    error = colors.error,
    onError = colors.onError,
    outline = colors.outline
)

@Composable
fun DripLabTheme(
    theme: DripTheme = DripTheme.CLASSIC,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = DripColorPalette.fromTheme(theme)
    val colorScheme = if (darkTheme) {
        dripColorsToDarkScheme(colors)
    } else {
        dripColorsToLightScheme(colors)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DripLabTypography,
        content = content
    )
}