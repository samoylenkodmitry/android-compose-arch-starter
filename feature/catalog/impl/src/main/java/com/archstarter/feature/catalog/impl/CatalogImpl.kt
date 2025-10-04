package com.archstarter.feature.catalog.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.catalog.api.CatalogPresenter
import com.archstarter.feature.catalog.api.CatalogState
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides

@Inject
class CatalogViewModel(
    private val repo: ArticleRepo,
    private val app: App,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus,
    @Assisted private val handle: SavedStateHandle,
) : ViewModel(), CatalogPresenter {
    private val _state = MutableStateFlow(CatalogState())
    override val state: StateFlow<CatalogState> = _state

    val busText = screenBus.text.stateIn(viewModelScope, SharingStarted.Eagerly, screenBus.text.value)

    init {
        println("CatalogViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
        bridge.setDelegate(this)
        val articles = repo.articles
        articles
            .onEach { list ->
                _state.update { current ->
                    current.copy(items = list.map { it.id })
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            val hasArticles = articles.first().isNotEmpty()
            if (!hasArticles) {
                _state.update { it.copy(isRefreshing = true) }
                try {
                    repeat(10) { repo.refresh() }
                } finally {
                    _state.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("CatalogViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onRefresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                repo.refresh()
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
        screenBus.send("Catalog refreshed at ${System.currentTimeMillis()}")
    }

    override fun onSettingsClick() {
        app.navigation.openSettings()
    }

    override fun onItemClick(id: Int) {
        app.navigation.openDetail(id)
    }

    override fun initOnce(params: Unit?) {
    }
}

@Inject
class CatalogViewModelFactory(
    private val create: (SavedStateHandle) -> CatalogViewModel,
) : AssistedVmFactory<CatalogViewModel> {
    override fun create(handle: SavedStateHandle): CatalogViewModel = create(handle)
}

interface CatalogAppBindings {
    @Provides
    @IntoMap
    fun provideCatalogProvider(): Pair<Class<*>, PresenterProvider<*>> =
        CatalogPresenter::class.java to object : PresenterProvider<CatalogPresenter> {
            @Composable
            override fun provide(key: String?): CatalogPresenter {
                return scopedViewModel<CatalogViewModel>(key)
            }
        }
}

interface CatalogScreenBindings {
    @Provides
    @IntoMap
    fun catalogFactory(factory: CatalogViewModelFactory): Pair<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> =
        CatalogViewModel::class.java to factory
}
