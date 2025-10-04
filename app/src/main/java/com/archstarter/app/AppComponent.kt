package com.archstarter.app

import android.content.Context
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.AppScope
import com.archstarter.core.common.presenter.PresenterResolver
import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.ScreenComponentNode
import com.archstarter.core.common.scope.DefaultScreenComponentNode
import com.archstarter.core.common.scope.DefaultSubscreenComponentNode
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.common.scope.SubscreenComponentNode
import com.archstarter.core.common.scope.SubscreenScope
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.catalog.impl.CatalogAppBindings
import com.archstarter.feature.catalog.impl.CatalogScreenBindings
import com.archstarter.feature.catalog.impl.CatalogItemAppBindings
import com.archstarter.feature.catalog.impl.CatalogItemScreenBindings
import com.archstarter.feature.catalog.impl.data.ArticleDataBindings
import com.archstarter.feature.detail.impl.DetailAppBindings
import com.archstarter.feature.detail.impl.DetailScreenBindings
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import com.archstarter.feature.onboarding.impl.OnboardingAppBindings
import com.archstarter.feature.onboarding.impl.OnboardingScreenBindings
import com.archstarter.feature.settings.impl.SettingsPresenterBindings
import com.archstarter.feature.settings.impl.SettingsScreenBindings
import com.archstarter.feature.settings.impl.data.SettingsDataBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserPresenterBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserScreenBindings
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

interface AppBindings :
    ArticleDataBindings,
    SettingsDataBindings,
    OnboardingAppBindings,
    CatalogAppBindings,
    CatalogItemAppBindings,
    DetailAppBindings,
    SettingsPresenterBindings,
    LanguageChooserPresenterBindings {
    @Provides
    fun presenterResolver(resolver: InjectPresenterResolver): PresenterResolver = resolver
}

interface ScreenBindings :
    OnboardingScreenBindings,
    CatalogScreenBindings,
    CatalogItemScreenBindings,
    DetailScreenBindings,
    SettingsScreenBindings,
    LanguageChooserScreenBindings

@AppScope
@Component
abstract class AppComponent(
    @get:Provides val context: Context,
    @get:Provides val app: App,
) : AppBindings {
    abstract val presenterResolver: PresenterResolver
    abstract val onboardingStatusProvider: OnboardingStatusProvider
}

@ScreenScope
@Component
abstract class ScreenComponent(
    @Component val appComponent: AppComponent,
) : ScreenComponentNode, ScreenBindings {
    protected abstract val node: DefaultScreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}

@SubscreenScope
@Component
abstract class SubscreenComponent(
    @Component val parent: ScreenComponent,
) : SubscreenComponentNode, ScreenBindings {
    protected abstract val node: DefaultSubscreenComponentNode

    override fun viewModelFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        node.viewModelFactories()
}
