package com.archstarter.app

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.DefaultScreenComponentNode
import com.archstarter.core.common.scope.ScreenComponentNode
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.catalog.impl.CatalogScreenBindings
import com.archstarter.feature.catalog.impl.CatalogItemScreenBindings
import com.archstarter.feature.detail.impl.DetailScreenBindings
import com.archstarter.feature.onboarding.impl.OnboardingScreenBindings
import com.archstarter.feature.settings.impl.SettingsScreenBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserScreenBindings
import me.tatarka.inject.annotations.Component

@ScreenScope
@Component
abstract class ScreenComponent(
    @Component val appComponent: AppComponent,
) : ScreenComponentNode,
    OnboardingScreenBindings,
    CatalogScreenBindings,
    CatalogItemScreenBindings,
    DetailScreenBindings,
    SettingsScreenBindings,
    LanguageChooserScreenBindings {
    protected abstract val node: DefaultScreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
