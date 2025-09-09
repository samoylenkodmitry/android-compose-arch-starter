package com.example.feature.settings.impl.data

import com.example.feature.settings.api.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    suspend fun updateNative(language: String) {
        _state.value = _state.value.copy(nativeLanguage = language)
    }

    suspend fun updateLearning(language: String) {
        _state.value = _state.value.copy(learningLanguage = language)
    }
}
