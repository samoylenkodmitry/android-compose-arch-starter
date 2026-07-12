package com.archstarter.core.common.scope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Base class for screen-scoped ViewModels that want a lifecycle-aware [viewModelScope].
 *
 * When resolved through [com.archstarter.core.common.viewmodel.ScreenVmFactory] the factory sets
 * [screenScopeHolder] right after construction. From then on, an unqualified `viewModelScope`
 * inside subclasses resolves to the member extension below, which returns the holder's
 * lifecycle-aware scope instead of the default [ViewModel.viewModelScope]. That scope is cancelled
 * when the screen goes off-screen, so jobs launched from it (e.g. in `initOnce`) stop rather than
 * leaking as stale subscriptions.
 *
 * Caveat: the base-class constructor runs before subclass property initializers, and the factory
 * sets [screenScopeHolder] only after construction completes. So any `stateIn(viewModelScope)` in a
 * property initializer captures the default scope; move such work into `initOnce` to get the
 * lifecycle-aware behaviour.
 */
abstract class ScreenViewModel : ViewModel() {
  lateinit var screenScopeHolder: ScreenScopeHolder

  val ScreenViewModel.viewModelScope
    get() =
      if (::screenScopeHolder.isInitialized) screenScopeHolder.screenScope()
      else (ViewModel::viewModelScope).get(this)
}
