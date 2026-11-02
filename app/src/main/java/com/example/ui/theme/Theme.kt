package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MakiLightColorScheme = lightColorScheme(
    primary = MakiEmerald,
    onPrimary = Color.White,
    primaryContainer = MakiEmeraldContainer,
    onPrimaryContainer = MakiEmeraldDark,
    secondary = MakiChampagneGold,
    onSecondary = MakiCharcoal,
    secondaryContainer = MakiGoldLight,
    onSecondaryContainer = MakiGoldDark,
    tertiary = MakiGoldAccent,
    onTertiary = MakiCharcoal,
    background = MakiWarmIvory,
    onBackground = MakiCharcoal,
    surface = MakiSurfaceWhite,
    onSurface = MakiCharcoal,
    surfaceVariant = MakiWarmIvory,
    onSurfaceVariant = MakiCharcoalLight,
    outline = MakiCardBorder,
    outlineVariant = MakiDivider,
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
