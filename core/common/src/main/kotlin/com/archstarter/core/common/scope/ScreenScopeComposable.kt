package com.archstarter.core.common.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
val LocalScreenComponentFactory = staticCompositionLocalOf<ScreenComponentFactory> {
    error("LocalScreenComponentFactory not provided")
}

val LocalSubscreenComponentFactory = staticCompositionLocalOf<SubscreenComponentFactory> {
    error("LocalSubscreenComponentFactory not provided")
}

private val LocalScreenGraphNode = staticCompositionLocalOf<ScreenGraphNode?> {
    null
}

val LocalScreenComponentProvider = LocalScreenGraphNode

@Composable
fun ScreenScope(
    nested: Boolean = false,
    content: @Composable () -> Unit,
) {
    val screenFactory = LocalScreenComponentFactory.current
    val subscreenFactory = LocalSubscreenComponentFactory.current
    val parent = LocalScreenGraphNode.current

    val provided = remember(parent, nested) {
        when {
            parent == null -> screenFactory()
            nested -> {
                val parentScreen = parent as? ScreenComponentNode
                    ?: error("Cannot nest inside another subscreen component")
                subscreenFactory(parentScreen)
            }
            else -> parent
        }
    }

    DisposableEffect(provided) {
        onDispose { }
    }

    CompositionLocalProvider(LocalScreenGraphNode provides provided) {
        content()
    }
}
