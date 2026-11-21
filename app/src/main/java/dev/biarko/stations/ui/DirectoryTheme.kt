package dev.biarko.stations.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006951),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EFE4),
    onPrimaryContainer = Color(0xFF164C3D),
    secondaryContainer = Color(0xFFCBE6D6),
    onSecondaryContainer = Color(0xFF123E2E),
    inverseSurface = Color(0xFF173C35),
    inverseOnSurface = Color(0xFFF3FBF5),
    inversePrimary = Color(0xFFA8DBBF),
    surface = Color(0xFFF3F6F2),
    surfaceContainerLowest = Color.White,
    onSurface = Color(0xFF182B24),
    onSurfaceVariant = Color(0xFF4F6158),
    outlineVariant = Color(0xFFD4DFD6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA0DEBD),
    onPrimary = Color(0xFF073D2D),
    primaryContainer = Color(0xFF254D40),
    onPrimaryContainer = Color(0xFFBEEDD2),
    secondaryContainer = Color(0xFF305743),
    onSecondaryContainer = Color(0xFFD1F2DB),
    inverseSurface = Color(0xFF214B40),
    inverseOnSurface = Color(0xFFF0FAF1),
    inversePrimary = Color(0xFFB2E5C9),
    surface = Color(0xFF101915),
    surfaceContainerLowest = Color(0xFF1A2620),
    onSurface = Color(0xFFE4EEE6),
    onSurfaceVariant = Color(0xFFB2C3B7),
    outlineVariant = Color(0xFF35483D),
)

@Composable
internal fun DirectoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
