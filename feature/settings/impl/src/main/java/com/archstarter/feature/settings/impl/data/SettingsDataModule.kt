package com.archstarter.feature.settings.impl.data

import com.archstarter.feature.settings.api.SettingsStateProvider
import me.tatarka.inject.annotations.Provides

interface SettingsDataBindings {
  @Provides
  fun provideSettingsStateProvider(
    repository: SettingsRepository,
  ): SettingsStateProvider = repository
}
