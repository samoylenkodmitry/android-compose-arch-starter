package com.archstarter.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF443355),
        secondary = Color(0xFF221133),
        tertiary = Color(0xFF665577),
        background = Color(0xFF110022),
        surface = Color(0xFF221133),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFEEDDFF),
        onSurface = Color(0xFFEEDDFF),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFEEDDFF),
        secondary = Color(0xFFDDCCEE),
        tertiary = Color(0xFFCCBBFF),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF7F0FF),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onTertiary = Color.Black,
        onBackground = Color(0xFF221133),
        onSurface = Color(0xFF221133),
    )

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
  val colorScheme =
      when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
  MaterialTheme(colorScheme = colorScheme, content = content)
}
