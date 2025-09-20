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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogItemViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), CatalogItemPresenter {
    private val params = MutableSharedFlow<Int>(replay = 1)
    private val articles = params.flatMapLatest { id -> repo.articleFlow(id) }
    private val translations = articles.flatMapLatest { article ->
        if (article == null) {
            flowOf<String?>(null)
        } else {
            flow {
                emit(null)
                val translated = runCatching { repo.translateSummary(article) }.getOrNull()
                if (!translated.isNullOrBlank() && translated != article.summary) {
                    emit(translated)
                }
            }
        }
    }
    override val state: StateFlow<CatalogItem> =
        combine(articles, translations) { article, translated ->
            if (article == null) {
                CatalogItem(0, "", "")
            } else {
                CatalogItem(article.id, article.title, translated ?: article.summary)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogItem(0, "", ""))

    init {
        println("CatalogItemViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }
    override fun onCleared() {
        super.onCleared()
        println("CatalogItemViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun initOnce(params: Int) {
        viewModelScope.launch {
            this@CatalogItemViewModel.params.emit(params)
        }
    }

    override fun onClick() {
        val current = state.value
        bridge.onItemClick(current.id)
        screenBus.send("Item ${current.id} clicked at ${System.currentTimeMillis()}")
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
