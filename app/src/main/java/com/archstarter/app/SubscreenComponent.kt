package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultSubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.catalog.impl.CatalogScreenBindings
import com.archstarter.feature.catalog.impl.CatalogItemScreenBindings
import com.archstarter.feature.detail.impl.DetailScreenBindings
import com.archstarter.feature.onboarding.impl.OnboardingScreenBindings
import com.archstarter.feature.settings.impl.SettingsScreenBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserScreenBindings
import me.tatarka.inject.annotations.Component

@SubscreenScope
@Component
abstract class SubscreenComponent(
    @Component val parent: ScreenComponent,
) : SubscreenComponentNode,
    OnboardingScreenBindings,
    CatalogScreenBindings,
    CatalogItemScreenBindings,
    DetailScreenBindings,
    SettingsScreenBindings,
    LanguageChooserScreenBindings {
    protected abstract val node: DefaultSubscreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
