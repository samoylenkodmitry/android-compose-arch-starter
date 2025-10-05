package com.archstarter.app

import android.content.Context
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.AppScope
import com.archstarter.core.common.presenter.PresenterResolver
import com.archstarter.feature.catalog.impl.CatalogAppGraphBindings
import com.archstarter.feature.detail.impl.DetailAppGraphBindings
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import com.archstarter.feature.onboarding.impl.OnboardingAppGraphBindings
import com.archstarter.feature.settings.impl.SettingsAppGraphBindings
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@AppScope
@Component
abstract class AppComponent(
    @get:Provides val context: Context,
    @get:Provides val app: App,
) : CatalogAppGraphBindings,
    DetailAppGraphBindings,
    OnboardingAppGraphBindings,
    SettingsAppGraphBindings {
    abstract val presenterResolver: PresenterResolver
    abstract val onboardingStatusProvider: OnboardingStatusProvider

    @Provides
    protected fun providePresenterResolver(resolver: InjectPresenterResolver): PresenterResolver = resolver
}
