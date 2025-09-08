package com.example.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.settings.api.SettingsPresenter
import com.example.feature.settings.api.SettingsState
import com.example.feature.settings.impl.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel(), SettingsPresenter {
    override val state: StateFlow<SettingsState> = repo.state

    override fun onNativeSelected(language: String) {
        viewModelScope.launch { repo.updateNative(language) }
    }

    override fun onLearningSelected(language: String) {
        viewModelScope.launch { repo.updateLearning(language) }
    }

    override fun initOnce(params: Unit) {
    }
}
