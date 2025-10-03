package com.archstarter.feature.catalog.impl.di

import com.archstarter.core.common.presenter.PresenterFactory
import com.archstarter.feature.catalog.api.CatalogItemPresenter
import com.archstarter.feature.catalog.api.CatalogPresenter
import com.archstarter.feature.catalog.impl.CatalogItemPresenterImpl
import com.archstarter.feature.catalog.impl.CatalogPresenterImpl
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import kotlin.reflect.KClass

interface CatalogModule {
    @Provides
    @IntoMap
    fun provideCatalogPresenter(
        factory: (Unit?) -> CatalogPresenterImpl
    ): Pair<KClass<*>, PresenterFactory> = CatalogPresenter::class to factory

    @Provides
    @IntoMap
    fun provideCatalogItemPresenter(
        factory: (Int?) -> CatalogItemPresenterImpl
    ): Pair<KClass<*>, PresenterFactory> = CatalogItemPresenter::class to factory
}