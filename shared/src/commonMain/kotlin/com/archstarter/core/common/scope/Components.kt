package com.archstarter.core.common.scope

import com.archstarter.core.common.navigation.Navigation
import com.archstarter.core.common.presenter.PresentersModule
import com.archstarter.feature.catalog.impl.di.CatalogModule
import com.archstarter.feature.settings.impl.di.SettingsModule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@AppScope
@Component
abstract class AppComponent(
    @get:Provides val navigator: Navigation
) : SettingsModule {
    abstract val screenComponentFactory: () -> ScreenComponent
}

@ScreenScope
@Component
abstract class ScreenComponent(
    @Component val parent: AppComponent,
    @get:Component val presentersModule: PresentersModule = PresentersModule()
) : HasPresenterFactories, CatalogModule {
    abstract val subscreenComponentFactory: () -> SubscreenComponent
}

@ScreenScope
@Component
abstract class SubscreenComponent(
    @Component val parent: ScreenComponent,
    @get:Component val presentersModule: PresentersModule = PresentersModule()
) : HasPresenterFactories, CatalogModule