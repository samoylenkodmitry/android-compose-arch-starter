package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultScreenComponentNode
import com.archstarter.core.common.scope.ScreenComponentNode
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.catalog.impl.CatalogScreenGraphBindings
import com.archstarter.feature.detail.impl.DetailScreenGraphBindings
import com.archstarter.feature.onboarding.impl.OnboardingScreenGraphBindings
import com.archstarter.feature.settings.impl.SettingsScreenGraphBindings
import me.tatarka.inject.annotations.Component

@ScreenScope
@Component
abstract class ScreenComponent(
    @Component val appComponent: AppComponent,
) : ScreenComponentNode,
    OnboardingScreenGraphBindings,
    CatalogScreenGraphBindings,
    DetailScreenGraphBindings,
    SettingsScreenGraphBindings {
    protected abstract val node: DefaultScreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
