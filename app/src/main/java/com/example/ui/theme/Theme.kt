package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = DarkGlassCard,
    onPrimaryContainer = NeonCyan,
    secondary = CyberGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = DarkGlassSurface,
    onSecondaryContainer = CyberGold,
    tertiary = NeonGreen,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextHigh,
    surface = DarkGlassSurface,
    onSurface = TextHigh,
    surfaceVariant = DarkGlassCard,
    onSurfaceVariant = TextMedium,
    outline = DarkGlassBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Camera interface is always optimized for dark pro-viewfinder ergonomics
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
