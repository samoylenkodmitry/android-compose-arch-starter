package com.example.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.settings.api.SettingsPresenter
import com.example.feature.settings.api.SettingsState
import com.example.feature.settings.impl.data.SettingsRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsBindings {
    @dagger.Provides
    @IntoMap
    @ClassKey(SettingsPresenter::class)
    fun bindSettingsPresenter(): Class<out ViewModel> = SettingsViewModel::class.java
}
