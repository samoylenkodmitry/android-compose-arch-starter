package com.example.feature.settings.impl

import androidx.compose.runtime.Composable
import com.example.core.common.presenter.PresenterProvider
import com.example.core.common.viewmodel.scopedViewModel
import com.example.feature.settings.api.SettingsPresenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap


@Module
@InstallIn(SingletonComponent::class)
object SettingsPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(SettingsPresenter::class)
    fun provideSettingsPresenterProvider(): PresenterProvider<*> {
        return object : PresenterProvider<SettingsPresenter> {
            @Composable
            override fun provide(key: String?): SettingsPresenter {
                return scopedViewModel<SettingsViewModel>(key)
            }
        }
    }
}