package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultSubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.catalog.impl.CatalogScreenGraphBindings
import com.archstarter.feature.detail.impl.DetailScreenGraphBindings
import com.archstarter.feature.onboarding.impl.OnboardingScreenGraphBindings
import com.archstarter.feature.settings.impl.SettingsScreenGraphBindings
import me.tatarka.inject.annotations.Component

@SubscreenScope
@Component
abstract class SubscreenComponent(
    @Component val parent: ScreenComponent,
) : SubscreenComponentNode,
    OnboardingScreenGraphBindings,
    CatalogScreenGraphBindings,
    DetailScreenGraphBindings,
    SettingsScreenGraphBindings {
    protected abstract val node: DefaultSubscreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
