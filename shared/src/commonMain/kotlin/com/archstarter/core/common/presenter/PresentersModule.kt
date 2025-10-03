package com.archstarter.core.common.presenter

import com.archstarter.core.common.scope.PresenterFactory
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import kotlin.reflect.KClass

abstract class PresentersModule {
    @Provides
    @IntoMap
    fun providePresenterFactories(
        factories: Map<KClass<*>, PresenterFactory>
    ): Map<KClass<*>, PresenterFactory> = factories
}