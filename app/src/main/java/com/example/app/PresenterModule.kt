package com.example.app

import com.example.core.common.presenter.ParamInit
import com.example.core.common.presenter.PresenterProvider
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class PresenterModule {
    @Multibinds
    abstract fun presenterProviders(): Map<Class<out ParamInit<*>>, PresenterProvider<*>>
}