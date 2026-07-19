package com.archstarter.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Minimal theme wrapper that keeps Compose Multiplatform material bindings wired
 * for the future shared design system.
 */
@Composable
fun SharedPreviewTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
