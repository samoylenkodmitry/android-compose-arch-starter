package com.example.feature.catalog.impl

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core.common.presenter.PresenterProvider
import com.example.feature.catalog.api.CatalogPresenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object CatalogPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(CatalogPresenter::class)
    fun provideCatalogPresenterProvider(): PresenterProvider<CatalogViewModel> {
        return object : PresenterProvider<CatalogViewModel> {
            @Composable
            override fun provide(key: String?): CatalogViewModel {
                return hiltViewModel<CatalogViewModel>(key = key)
            }
        }
    }
}
