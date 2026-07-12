package com.archstarter.core.common.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.scope.ScreenScopeHolder
import com.archstarter.core.common.scope.ScreenScopeHolderEntryPoint
import com.archstarter.core.common.scope.ScreenViewModel
import com.archstarter.core.common.scope.SubscreenComponent
import com.archstarter.core.common.scope.VmMapEntryPoint
import com.archstarter.core.common.scope.SubVmMapEntryPoint
import dagger.hilt.EntryPoints

class ScreenVmFactory(
  owner: SavedStateRegistryOwner,
  defaultArgs: Bundle?,
  private val component: Any // ScreenComponent or SubscreenComponent
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

  // Get the VM map from whichever component this is
  private val map: Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> by lazy {
    when (component) {
      is ScreenComponent -> EntryPoints.get(component, VmMapEntryPoint::class.java).vmFactories()
      is SubscreenComponent -> EntryPoints.get(component, SubVmMapEntryPoint::class.java).vmFactories()
      else -> error("Unsupported component type")
    }
  }

  // Lifecycle-aware scope holder, only available on the top-level ScreenComponent.
  private val scopeHolder: ScreenScopeHolder? by lazy {
    when (component) {
      is ScreenComponent ->
        EntryPoints.get(component, ScreenScopeHolderEntryPoint::class.java).screenScopeHolder()
      else -> null
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
    val raw = map[modelClass] ?: error("No AssistedVmFactory bound for ${modelClass.name}")
    val vm = (raw as AssistedVmFactory<T>).create(handle)

    vm as? ScreenViewModel
      ?: error("VM ${modelClass.name} must extend ScreenViewModel to receive lifecycle-aware scope")

    // Attach the lifecycle-aware scope so unqualified viewModelScope in the VM is pausable.
    scopeHolder?.let { vm.screenScopeHolder = it }

    return vm
  }
}