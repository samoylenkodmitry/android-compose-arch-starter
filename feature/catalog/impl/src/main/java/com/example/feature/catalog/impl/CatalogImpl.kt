package com.example.feature.catalog.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.app.App
import com.example.feature.catalog.api.CatalogPresenter
import com.example.feature.catalog.api.CatalogState
import com.example.feature.catalog.impl.data.ArticleRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
  private val repo: ArticleRepo,
  private val app: App,
  private val bridge: CatalogBridge,
) : ViewModel(), CatalogPresenter {
  private val _state = MutableStateFlow(CatalogState())
  override val state: StateFlow<CatalogState> = _state

  init {
    bridge.setDelegate(this)
    viewModelScope.launch {
      repo.articles.collect { list ->
        _state.value = CatalogState(list.map { it.id })
      }
    }
  }

  override fun onRefresh() {
    viewModelScope.launch { repo.refresh() }
  }

  override fun onSettingsClick() {
    app.navigation.openSettings()
  }

  override fun onItemClick(id: Int) {
    app.navigation.openDetail(id)
  }

  override fun initOnce(params: Unit) {
  }
}

