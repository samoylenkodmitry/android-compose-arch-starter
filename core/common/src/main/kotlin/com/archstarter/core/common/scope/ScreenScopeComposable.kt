package com.archstarter.core.common.scope

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.EntryPoints

private val LocalScreenComponent = compositionLocalOf<Any?> {
  null // Allow null default instead of error
}

/**
 * Identifies the current App/activity instance. When it changes (e.g. the activity is recreated on
 * a font-scale change) the top-level [ScreenComponent] is rebuilt so stale Hilt graphs are dropped.
 * Provided by MainActivity.
 */
val LocalAppInstanceId = compositionLocalOf { 0 }

// This will be provided by MainActivity
val LocalScreenBuilder = staticCompositionLocalOf<ScreenComponent.Builder> {
  error("LocalScreenBuilder not provided")
}

@Composable
fun ScreenScope(
  nested: Boolean = false,
  content: @Composable () -> Unit
) {
  val screenBuilder = LocalScreenBuilder.current
  val appInstanceId = LocalAppInstanceId.current
  val viewModelStoreOwner = LocalViewModelStoreOwner.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Find nearest component (if any)
  val parentAny = LocalScreenComponent.current

  // Decide which component to provide to children.
  val provided: Any = when {
    parentAny == null -> {
      // Top-level screen component: store it in a ViewModel so its lifetime matches the
      // ViewModelStore (NavBackStackEntry). This survives recompositions and config changes and
      // is only disposed when the entry leaves the back stack.
      if (viewModelStoreOwner != null) {
        val holder: ScreenComponentHolder = viewModel(
          viewModelStoreOwner = viewModelStoreOwner,
          factory = ScreenComponentHolder.factory(screenBuilder, appInstanceId)
        )

        // Attach lifecycle ONLY for top-level components; nested subscreens share the parent scope.
        LaunchedEffect(holder, lifecycleOwner, appInstanceId) {
          holder.updateAppInstance(appInstanceId)
          log("attaching lifecycle to top-level component inst=${System.identityHashCode(holder.component)}")
          holder.attachLifecycle(lifecycleOwner)
        }

        holder.component
      } else {
        error("ScreenScope requires a ViewModelStoreOwner for top-level screens")
      }
    }
    nested -> {
      // Build a Subscreen from the nearest ScreenComponent.
      val parentScreen: ScreenComponent = when (parentAny) {
        is ScreenComponent -> parentAny
        is SubscreenComponent -> error("Cannot nest inside another SubscreenComponent")
        else -> error("Unsupported parent type")
      }
      val subBuilder = EntryPoints.get(parentScreen, SubscreenBuilderEntryPoint::class.java).subBuilder()
      subBuilder.build().also {
        log("created nested SubscreenComponent inst=${System.identityHashCode(it)}")
      }
    }
    else -> {
      // Reuse nearest component if not nesting.
      parentAny
    }
  }

  CompositionLocalProvider(LocalScreenComponent provides provided) {
    content()
  }
}

// Expose LocalScreenComponent for the ViewModel factory
val LocalScreenComponentProvider = LocalScreenComponent

private fun log(msg: String) = Log.d("ScreenScope", msg)
