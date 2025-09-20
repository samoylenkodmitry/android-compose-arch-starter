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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogItemViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), CatalogItemPresenter {
    private val articleIds = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    private val translationRequests = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private val articleState = articleIds
        .flatMapLatest { id -> repo.article(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val articles = articleState.filterNotNull()

    override val state: StateFlow<CatalogItem> = articles
        .map { entity ->
            val summary = entity.summaryTranslated ?: entity.summaryOriginal
            CatalogItem(entity.id, entity.title, summary)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CatalogItem(0, "", "")
        )

    init {
        println("CatalogItemViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
        translationRequests
            .onEach { id -> repo.translateArticle(id) }
            .launchIn(viewModelScope)

        articles
            .distinctUntilChanged { old, new ->
                old.id == new.id && old.summaryTranslated == new.summaryTranslated
            }
            .onEach { entity ->
                if (entity.summaryTranslated == null) {
                    translationRequests.emit(entity.id)
                }
            }
            .launchIn(viewModelScope)
    }
    override fun onCleared() {
        super.onCleared()
        println("CatalogItemViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun initOnce(params: Int) {
        articleIds.tryEmit(params)
    }

    override fun onClick() {
        val item = state.value
        bridge.onItemClick(item.id)
        screenBus.send("Item ${item.id} clicked at ${System.currentTimeMillis()}")
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
