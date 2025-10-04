package com.archstarter.feature.onboarding.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.onboarding.api.OnboardingPresenter
import com.archstarter.feature.onboarding.api.OnboardingState
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides

@Inject
class OnboardingViewModel(
    private val repository: OnboardingRepository,
    @Assisted private val handle: SavedStateHandle,
) : ViewModel(), OnboardingPresenter {
    private val _state = MutableStateFlow(OnboardingState())
    override val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        repository.hasCompleted
            .onEach { completed ->
                _state.update { current -> current.copy(completed = completed) }
            }
            .launchIn(viewModelScope)
    }

    override fun onContinue() {
        if (_state.value.completed == true) return
        viewModelScope.launch {
            repository.markCompleted()
        }
    }

    override fun initOnce(params: Unit?) = Unit
}

@Inject
class OnboardingViewModelFactory(
    private val create: (SavedStateHandle) -> OnboardingViewModel,
) : AssistedVmFactory<OnboardingViewModel> {
    override fun create(handle: SavedStateHandle): OnboardingViewModel = create(handle)
}

interface OnboardingAppBindings {
    @Provides
    fun provideOnboardingStatus(repo: OnboardingRepository): OnboardingStatusProvider = repo

    @Provides
    @IntoMap
    fun provideOnboardingPresenter(): Pair<Class<*>, PresenterProvider<*>> =
        OnboardingPresenter::class.java to object : PresenterProvider<OnboardingPresenter> {
            @Composable
            override fun provide(key: String?): OnboardingPresenter {
                return scopedViewModel<OnboardingViewModel>(key)
            }
        }
}

interface OnboardingScreenBindings {
    @Provides
    @IntoMap
    fun onboardingFactory(factory: OnboardingViewModelFactory): Pair<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        OnboardingViewModel::class.java to factory
}
