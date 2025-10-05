package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultSubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.di.generated.GeneratedSubscreenBindings
import me.tatarka.inject.annotations.Component

@SubscreenScope
@Component
abstract class SubscreenComponent(
    @Component val parent: ScreenComponent,
) : SubscreenComponentNode,
    GeneratedSubscreenBindings {
    protected abstract val node: DefaultSubscreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
