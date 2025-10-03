package com.archstarter.feature.catalog.impl

import com.archstarter.core.common.navigation.Navigation
import com.archstarter.core.common.presenter.ParamInit
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.feature.catalog.api.CatalogPresenter
import com.archstarter.feature.catalog.api.CatalogState
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@ScreenScope
@Inject
class CatalogPresenterImpl(
    private val repo: ArticleRepo,
    private val navigator: Navigation,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus,
) : CatalogPresenter, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main) {

    private val _state = MutableStateFlow(CatalogState())
    override val state: StateFlow<CatalogState> = _state

    override fun initOnce(params: Unit?) {
        bridge.setDelegate(this)
        val articles = repo.articles
        articles
            .onEach { list ->
                _state.update { current ->
                    current.copy(items = list.map { it.id })
                }
            }
            .launchIn(this)
        launch {
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

    override fun onRefresh() {
        if (_state.value.isRefreshing) return
        launch {
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
        navigator.openSettings()
    }

    override fun onItemClick(id: Int) {
        navigator.openDetail(id)
    }
}