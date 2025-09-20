package com.archstarter.feature.catalog.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.catalog.api.CatalogItem
import com.archstarter.feature.catalog.api.CatalogItemPresenter
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CatalogItemViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), CatalogItemPresenter {
    private val _state = MutableStateFlow(CatalogItem(0, "", ""))
    override val state: StateFlow<CatalogItem> = _state
    private var translationJob: Job? = null
    private var observeJob: Job? = null

    init {
        println("CatalogItemViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }
    override fun onCleared() {
        super.onCleared()
        println("CatalogItemViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun initOnce(params: Int) {
        if (observeJob != null) return
        observeJob = viewModelScope.launch {
            repo.article(params).collect { entity ->
                if (entity == null) return@collect
                val summary = entity.summaryTranslated ?: entity.summaryOriginal
                _state.value = CatalogItem(entity.id, entity.title, summary)
                if (entity.summaryTranslated == null && translationJob?.isActive != true) {
                    translationJob = launch {
                        repo.translateArticle(entity.id)
                    }.also { job ->
                        job.invokeOnCompletion { if (translationJob === job) translationJob = null }
                    }
                }
            }
        }
    }

    override fun onClick() {
        bridge.onItemClick(_state.value.id)
        screenBus.send("Item ${_state.value.id} clicked at ${System.currentTimeMillis()}")
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<CatalogItemViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object CatalogItemBindings {

    @Provides
    @IntoMap
    @ClassKey(CatalogItemPresenter::class)
    fun provideCatalogItemProvider(): PresenterProvider<*> {
        return object : PresenterProvider<CatalogItemPresenter> {
            @Composable
            override fun provide(key: String?): CatalogItemPresenter {
                return scopedViewModel<CatalogItemViewModel>(key = key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class CatalogItemBindingModule {

    @Binds
    @IntoMap
    @VmKey(CatalogItemViewModel::class)
    abstract fun catalogItemFactory(f: CatalogItemViewModel.Factory): AssistedVmFactory<out ViewModel>
}
