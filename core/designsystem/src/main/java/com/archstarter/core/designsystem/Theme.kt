package com.archstarter.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DayColorScheme =
  lightColorScheme(
    primary = Color(0xFF19647E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3EBFF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF6C5B7B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAD8FF),
    onSecondaryContainer = Color(0xFF25103A),
    tertiary = Color(0xFFFF7B5C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD1),
    onTertiaryContainer = Color(0xFF2C1200),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFDBE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787C),
    outlineVariant = Color(0xFFBEC6CA),
    scrim = Color(0x66000000),
    inverseSurface = Color(0xFF303033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF53D6FF)
  )

private val NightColorScheme =
  darkColorScheme(
    primary = Color(0xFF53D6FF),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB3EBFF),
    secondary = Color(0xFFD4BFFF),
    onSecondary = Color(0xFF3A1D56),
    secondaryContainer = Color(0xFF51336E),
    onSecondaryContainer = Color(0xFFEAD8FF),
    tertiary = Color(0xFFFFB4A1),
    onTertiary = Color(0xFF4A1E0A),
    tertiaryContainer = Color(0xFF672E1F),
    onTertiaryContainer = Color(0xFFFFDAD1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF13151A),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CC),
    outline = Color(0xFF8A9194),
    outlineVariant = Color(0xFF40484C),
    scrim = Color(0x99000000),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2C2F33),
    inversePrimary = Color(0xFF19647E)
  )

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) NightColorScheme else DayColorScheme
  MaterialTheme(colorScheme = colorScheme, content = content)
}
