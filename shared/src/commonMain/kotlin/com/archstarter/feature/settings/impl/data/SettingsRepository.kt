package com.archstarter.feature.settings.impl.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getStringFlow
import com.archstarter.core.common.scope.AppScope
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class SettingsRepository(
    private val settings: Settings,
) : SettingsStateProvider {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val state: StateFlow<SettingsState> =
        combine(
            settings.getStringFlow(NATIVE_LANGUAGE_KEY, "English"),
            settings.getStringFlow(LEARNING_LANGUAGE_KEY, "Spanish")
        ) { native, learning ->
            SettingsState(nativeLanguage = native, learningLanguage = learning)
        }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun updateNative(language: String) {
        settings.putString(NATIVE_LANGUAGE_KEY, language)
    }

    fun updateLearning(language: String) {
        settings.putString(LEARNING_LANGUAGE_KEY, language)
    }

    private companion object {
        const val NATIVE_LANGUAGE_KEY = "native_language"
        const val LEARNING_LANGUAGE_KEY = "learning_language"
    }
}