package com.example.feature.catalog.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.catalog.api.CatalogItem
import com.example.feature.catalog.api.CatalogItemPresenter
import com.example.feature.catalog.impl.data.ArticleRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogItemViewModel @Inject constructor(
  private val repo: ArticleRepo,
  private val bridge: CatalogBridge,
) : ViewModel(), CatalogItemPresenter {
  private val _state = MutableStateFlow(CatalogItem(0, "", ""))
  override val state: StateFlow<CatalogItem> = _state

  override fun initOnce(params: Int) {
    viewModelScope.launch {
      repo.article(params)?.let {
        _state.value = CatalogItem(it.id, it.title, it.summary)
      }
    }
  }

  override fun onClick() {
    bridge.onItemClick(_state.value.id)
  }
}
