package com.archstarter.core.common.scope

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import me.tatarka.inject.annotations.Inject

interface ScreenGraphNode {
    fun viewModelFactories(): Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>
}

interface ScreenComponentNode : ScreenGraphNode

interface SubscreenComponentNode : ScreenGraphNode

@ScreenScope
@Inject
class DefaultScreenComponentNode(
    private val factories: Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>,
) : ScreenComponentNode {
    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> = factories
}

@SubscreenScope
@Inject
class DefaultSubscreenComponentNode(
    private val factories: Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>,
) : SubscreenComponentNode {
    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> = factories
}

typealias ScreenComponentFactory = () -> ScreenComponentNode
typealias SubscreenComponentFactory = (ScreenComponentNode) -> SubscreenComponentNode
