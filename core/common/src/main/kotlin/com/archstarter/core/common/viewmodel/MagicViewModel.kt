package com.archstarter.core.common.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.archstarter.core.common.scope.LocalAppInstanceId
import com.archstarter.core.common.scope.LocalScreenComponentProvider

@Composable
inline fun <reified VM : ViewModel> scopedViewModel(key: String?): VM {
  val owner = checkNotNull(LocalViewModelStoreOwner.current) {
    "magicViewModel() must be called where a ViewModelStoreOwner exists"
  }
  val savedOwner = owner as? SavedStateRegistryOwner
    ?: error("Owner must implement SavedStateRegistryOwner")

  val component = LocalScreenComponentProvider.current
    ?: error("magicViewModel() must be called within a ScreenScope")
  val defaultArgs = (owner as? NavBackStackEntry)?.arguments
  val appInstanceId = LocalAppInstanceId.current

  val factory = remember(component, owner, defaultArgs) {
    ScreenVmFactory(savedOwner, defaultArgs, component)
  }
  // Key must be unique per (caller key, VM type, component instance, app instance) so a fresh
  // screen/component gets its own ViewModel instead of reusing a stale one from the store. Without
  // the component identity + app instance in the key, re-entering a screen (or recreating the
  // activity) can hand back a VM bound to a disposed graph — the "stale subscription" bug.
  val fullKey = key + VM::class.java.name + System.identityHashCode(component) + appInstanceId
  return viewModel(viewModelStoreOwner = owner, factory = factory, key = fullKey)
}