package com.archstarter.feature.settings.impl.di

import com.russhwolf.settings.Settings
import com.archstarter.core.common.scope.AppScope
import com.archstarter.feature.settings.api.SettingsStateProvider
import com.archstarter.feature.settings.impl.data.SettingsRepository
import me.tatarka.inject.annotations.Binds
import me.tatarka.inject.annotations.Provides

interface SettingsModule {
    @AppScope
    @Provides
    fun provideSettings(): Settings = Settings()

    @AppScope
    @Binds
    fun bindSettingsStateProvider(impl: SettingsRepository): SettingsStateProvider
}