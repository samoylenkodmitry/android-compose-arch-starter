package com.archstarter.shared.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

/**
 * Temporary marker API that will be replaced by the real presenter scope
 * infrastructure as the migration advances. Keeping a composable placeholder
 * ensures the shared module already exercises Compose Multiplatform tooling.
 */
@Immutable
class FoundationPlaceholder(val name: String)

@Composable
fun rememberFoundationPlaceholder(name: String = "SharedFoundation"): FoundationPlaceholder =
  remember(name) { FoundationPlaceholder(name) }
