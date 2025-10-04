package com.archstarter.core.common.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.archstarter.core.common.scope.ScreenGraphNode

class ScreenVmFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?,
    private val node: ScreenGraphNode,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    private val map: Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val raw = map[modelClass] ?: error("No AssistedVmFactory bound for ${modelClass.name}")
        return (raw as AssistedVmFactory<T>).create(handle)
    }
}
