package com.archstarter.feature.settings.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.feature.settings.api.LanguageChooserRole
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.impl.data.SettingsRepository
import com.archstarter.feature.settings.impl.language.LanguageSelectionBus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import androidx.compose.runtime.Composable
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.viewmodel.scopedViewModel

@Inject
class SettingsViewModel(
    private val repo: SettingsRepository,
    private val screenBus: ScreenBus,
    private val languageSelectionBus: LanguageSelectionBus,
    @Assisted private val handle: SavedStateHandle,
) : ViewModel(), SettingsPresenter {
    override val state: StateFlow<SettingsState> = repo.state

    init {
        println("SettingsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
        languageSelectionBus.selections
            .onEach { event ->
                when (event.role) {
                    LanguageChooserRole.Native -> {
                        repo.updateNative(event.language)
                        screenBus.send("Native language changed to ${event.language}")
                    }
                    LanguageChooserRole.Learning -> {
                        repo.updateLearning(event.language)
                        screenBus.send("Learning language changed to ${event.language}")
                    }
                }
            }
            .launchIn(viewModelScope)
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

    override fun initOnce(params: Unit?) {
    }
}

@Inject
class SettingsViewModelFactory(
    private val create: (SavedStateHandle) -> SettingsViewModel,
) : AssistedVmFactory<SettingsViewModel> {
    override fun create(handle: SavedStateHandle): SettingsViewModel = create(handle)
}

interface SettingsScreenBindings {
    @Provides
    @IntoMap
    fun settingsFactory(factory: SettingsViewModelFactory): Pair<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        SettingsViewModel::class.java to factory
}

interface SettingsPresenterBindings {
    @Provides
    @IntoMap
    fun provideSettingsPresenter(): Pair<Class<*>, PresenterProvider<*>> =
        SettingsPresenter::class.java to object : PresenterProvider<SettingsPresenter> {
            @Composable
            override fun provide(key: String?): SettingsPresenter {
                return scopedViewModel<SettingsViewModel>(key)
            }
        }
}
