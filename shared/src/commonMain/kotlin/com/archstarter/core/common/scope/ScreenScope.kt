package com.archstarter.core.common.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}

val LocalScreenComponent = staticCompositionLocalOf<Any> {
    error("No ScreenComponent provided")
}

@Composable
fun ScreenScope(
    nested: Boolean = false,
    content: @Composable () -> Unit
) {
    val parentComponent = if (nested) {
        LocalScreenComponent.current as? ScreenComponent
            ?: error("Nested ScreenScope must be inside another ScreenScope")
    } else {
        LocalAppComponent.current
    }

    val screenComponent = remember(parentComponent) {
        when (parentComponent) {
            is AppComponent -> parentComponent.screenComponentFactory()
            is ScreenComponent -> parentComponent.subscreenComponentFactory()
            else -> error("Invalid parent component for ScreenScope")
        }
    }

    CompositionLocalProvider(
        LocalScreenComponent provides screenComponent,
        content = content
    )
}