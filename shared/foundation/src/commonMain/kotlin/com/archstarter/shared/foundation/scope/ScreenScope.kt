package com.archstarter.shared.foundation.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Scope

/**
 * Marks bindings that should live for the lifetime of a screen. Every `ScreenScope { }` call
 * creates (or reuses) a [ScreenComponent] that owns these bindings and makes them available to
 * presenters, composables, and sub-screens rendered within the scope.
 */
@Scope
annotation class ScreenScope

/**
 * Nested screens receive their own [SubscreenComponent] so they can add additional scoped bindings
 * while still sharing the parent [ScreenComponent]'s dependencies (for example a presenter bus).
 */
@Scope
annotation class SubscreenScope

/**
 * Creates a new root component for a screen. Applications can provide their own factory by
 * overriding [LocalScreenComponentFactory]; otherwise a plain [ScreenComponent] without
 * constructor arguments is created.
 */
fun interface ScreenComponentFactory {
  fun create(): ScreenComponent
}

private data class ScreenGraphNode(
  val root: ScreenComponent,
  val active: Any
)

private val LocalScreenNode = staticCompositionLocalOf<ScreenGraphNode?> { null }

val LocalScreenComponent = staticCompositionLocalOf<ScreenComponent> {
  error("ScreenScope not provided")
}

val LocalScreenComponentProvider = staticCompositionLocalOf<Any> {
  error("ScreenScope not provided")
}

val LocalScreenComponentFactory = staticCompositionLocalOf {
  ScreenComponentFactory { ScreenComponent.create() }
}

/**
 * Provides a screen-scoped dependency graph backed by Kotlin Inject. The top-most call creates a
 * [ScreenComponent]; nested calls either reuse the nearest component or, when [nested] is true,
 * create a [SubscreenComponent] that still has access to the parent's scoped bindings.
 */
@Composable
fun ScreenScope(
  nested: Boolean = false,
  content: @Composable () -> Unit
) {
  val parentNode = LocalScreenNode.current
  val factory = LocalScreenComponentFactory.current

  val node = remember(parentNode, factory, nested) {
    when {
      parentNode == null -> {
        val component = factory.create()
        ScreenGraphNode(component, component)
      }
      nested -> {
        val subscreen = SubscreenComponent.create(parentNode.root)
        ScreenGraphNode(parentNode.root, subscreen)
      }
      else -> parentNode
    }
  }

  DisposableEffect(node) {
    onDispose { /* If Closeable scopes are introduced, hook disposal here. */ }
  }

  CompositionLocalProvider(
    LocalScreenNode provides node,
    LocalScreenComponent provides node.root,
    LocalScreenComponentProvider provides node.active
  ) {
    content()
  }
}

/**
 * Convenience helper used by Kotlin Inject to scope screen level bindings.
 */
@ScreenScope
@Component
abstract class ScreenComponent {
  abstract val screenBus: ScreenBus

  companion object
}

@KmpComponentCreate
expect fun ScreenComponent.Companion.create(): ScreenComponent

/**
 * Independent child scopes can introduce new bindings while still sharing parent screen state.
 */
@SubscreenScope
@Component
abstract class SubscreenComponent(@Component val parent: ScreenComponent) {
  companion object
}

@KmpComponentCreate
expect fun SubscreenComponent.Companion.create(parent: ScreenComponent): SubscreenComponent

/**
 * Simple screen-level event bus used by tests and example features. Because it is annotated with
 * [ScreenScope], every screen (and its nested sub-screens) will share the same instance.
 */
@ScreenScope
@Inject
class ScreenBus {
  private val mutableText = MutableStateFlow("screen bus initialized")
  val text: StateFlow<String> = mutableText.asStateFlow()

  fun send(message: String) {
    mutableText.value = message
  }
}

/**
 * Helper for retrieving a screen-scoped binding from the active component. This mirrors the
 * ergonomics of Hilt's component retrieval but works across all Compose Multiplatform targets.
 */
@Composable
inline fun <reified T : Any> screenComponent(): T {
  val component = LocalScreenComponentProvider.current
  return component as? T
    ?: error("No ${T::class.simpleName} is available in the current ScreenScope")
}
