package com.example.feature.catalog.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.app.App
import com.example.feature.catalog.api.CatalogPresenter
import com.example.feature.catalog.api.CatalogState
import com.example.feature.catalog.api.CatalogItem
import com.example.feature.catalog.impl.data.ArticleRepo
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
  private val repo: ArticleRepo,
  private val app: App
) : ViewModel(), CatalogPresenter {
  private val _state = MutableStateFlow(CatalogState())
  override val state: StateFlow<CatalogState> = _state

  init {
    viewModelScope.launch {
      repo.articles.collect { list ->
        _state.value = CatalogState(list.map { CatalogItem(it.id, it.title, it.summary) })
      }
    }
  }

  override fun onRefresh() {
    viewModelScope.launch { repo.refresh() }
  }

  override fun onItemClick(id: Int) {
    app.navigation.openDetail(id)
  }
}

@Module
@InstallIn(SingletonComponent::class)
object CatalogBindings {
  @dagger.Provides @IntoMap @ClassKey(CatalogPresenter::class)
  fun bindCatalogPresenter(): Class<out ViewModel> = CatalogViewModel::class.java
}
