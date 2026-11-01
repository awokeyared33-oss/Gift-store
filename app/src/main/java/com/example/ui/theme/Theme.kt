package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MakiLightColorScheme = lightColorScheme(
    primary = MakiPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = MakiLightBlue,
    onPrimaryContainer = MakiDarkBlue,
    secondary = MakiSecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = MakiGold,
    onTertiary = Color.White,
    background = MakiBackground,
    onBackground = MakiTextDark,
    surface = MakiSurface,
    onSurface = MakiTextDark,
    surfaceVariant = MakiSurfaceVariant,
    onSurfaceVariant = MakiTextSecondary,
    outline = MakiSurfaceBorder,
    outlineVariant = Color(0xFFF3F4F6),
    error = MakiError,
    onError = Color.White,
    errorContainer = MakiErrorLight,
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MakiBrandsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MakiLightColorScheme,
        typography = Typography,
        content = content
    )
}
