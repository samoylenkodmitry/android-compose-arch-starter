package com.archstarter.feature.settings.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Settings

val languageCodes: Map<String, String> = mapOf(
    "English" to "en",
    "Spanish" to "es",
    "French" to "fr",
    "German" to "de",
    "Italian" to "it",
    "Portuguese" to "pt",
    "Russian" to "ru",
    "Japanese" to "ja",
    "Korean" to "ko",
    "Chinese" to "zh",
)

val supportedLanguages = languageCodes.keys.toList()

data class SettingsState(
    val nativeLanguage: String = "English",
    val learningLanguage: String = "Spanish"
)

interface SettingsStateProvider {
    val state: StateFlow<SettingsState>
}

interface SettingsPresenter : ParamInit<Unit> {
    val state: StateFlow<SettingsState>
    fun onNativeSelected(language: String)
    fun onLearningSelected(language: String)
}