package com.example.feature.settings.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.scope.ScreenBus
import com.example.core.common.scope.ScreenComponent
import com.example.core.common.viewmodel.AssistedVmFactory
import com.example.core.common.viewmodel.VmKey
import com.example.feature.settings.api.SettingsPresenter
import com.example.feature.settings.api.SettingsState
import com.example.feature.settings.impl.data.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel @AssistedInject constructor(
    private val repo: SettingsRepository,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), SettingsPresenter {
    override val state: StateFlow<SettingsState> = repo.state

    init {
        println("SettingsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onCleared() {
        super.onCleared()
        println("SettingsViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onNativeSelected(language: String) {
        viewModelScope.launch {
            repo.updateNative(language)
            screenBus.send("Native language changed to $language")
        }
    }

    override fun onLearningSelected(language: String) {
        viewModelScope.launch {
            repo.updateLearning(language)
            screenBus.send("Learning language changed to $language")
        }
    }

    override fun initOnce(params: Unit) {
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<SettingsViewModel>
}

@Module
@InstallIn(ScreenComponent::class)
abstract class SettingsVmBindingModule {

    @Binds
    @IntoMap
    @VmKey(SettingsViewModel::class)
    abstract fun settingsFactory(f: SettingsViewModel.Factory): AssistedVmFactory<out ViewModel>
}
