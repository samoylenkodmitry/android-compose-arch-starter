package com.example.feature.detail.impl

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core.common.presenter.PresenterProvider
import com.example.feature.detail.api.DetailPresenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object DetailPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(DetailPresenter::class)
    fun provideDetailPresenterProvider(): PresenterProvider<DetailViewModel> {
        return object : PresenterProvider<DetailViewModel> {
            @Composable
            override fun provide(key: String?): DetailViewModel {
                return hiltViewModel<DetailViewModel>(key = key)
            }
        }
    }
}