package com.archstarter.app

import android.content.Context
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.AppScope
import com.archstarter.core.common.presenter.PresenterResolver
import com.archstarter.feature.catalog.impl.CatalogAppBindings
import com.archstarter.feature.catalog.impl.CatalogItemAppBindings
import com.archstarter.feature.catalog.impl.data.ArticleDataBindings
import com.archstarter.feature.detail.impl.DetailAppBindings
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import com.archstarter.feature.onboarding.impl.OnboardingAppBindings
import com.archstarter.feature.settings.impl.SettingsPresenterBindings
import com.archstarter.feature.settings.impl.data.SettingsDataBindings
import com.archstarter.feature.settings.impl.language.LanguageChooserPresenterBindings
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@AppScope
@Component
abstract class AppComponent(
    @get:Provides val context: Context,
    @get:Provides val app: App,
) : ArticleDataBindings,
    SettingsDataBindings,
    OnboardingAppBindings,
    CatalogAppBindings,
    CatalogItemAppBindings,
    DetailAppBindings,
    SettingsPresenterBindings,
    LanguageChooserPresenterBindings {
    abstract val presenterResolver: PresenterResolver
    abstract val onboardingStatusProvider: OnboardingStatusProvider

    @Provides
    protected fun providePresenterResolver(resolver: InjectPresenterResolver): PresenterResolver = resolver
}
