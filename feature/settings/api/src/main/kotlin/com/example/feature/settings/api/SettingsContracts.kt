package com.example.feature.settings.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Settings

val supportedLanguages = listOf("English", "Spanish", "French", "German")

data class SettingsState(
    val nativeLanguage: String = supportedLanguages.first(),
    val learningLanguage: String = supportedLanguages[1]
)

interface SettingsPresenter {
    val state: StateFlow<SettingsState>
    fun onNativeSelected(language: String)
    fun onLearningSelected(language: String)
}
