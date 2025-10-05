package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultScreenComponentNode
import com.archstarter.core.common.scope.ScreenComponentNode
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.di.generated.GeneratedScreenBindings
import me.tatarka.inject.annotations.Component

@ScreenScope
@Component
abstract class ScreenComponent(
    @Component val appComponent: AppComponent,
) : ScreenComponentNode,
    GeneratedScreenBindings {
    protected abstract val node: DefaultScreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
