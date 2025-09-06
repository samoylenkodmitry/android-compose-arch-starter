package com.example.feature.catalog.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.catalog.api.CatalogPresenter
import com.example.feature.catalog.api.CatalogState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Simple fake repo kept local to the feature impl for the starter
interface CatalogRepo { suspend fun items(): List<String> }

class FakeCatalogRepo @Inject constructor() : CatalogRepo {
  override suspend fun items(): List<String> {
    delay(150) // simulate IO
    return listOf("One", "Two", "Three", "Four")
  }
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
  private val repo: CatalogRepo,
  private val saved: SavedStateHandle
) : ViewModel(), CatalogPresenter {
  private val _state = MutableStateFlow(CatalogState())
  override val state: StateFlow<CatalogState> = _state

  override fun onRefresh() {
    viewModelScope.launch {
      _state.value = _state.value.copy(items = repo.items())
    }
  }
}

@Module
@InstallIn(SingletonComponent::class)
object CatalogBindings {
  @Provides fun provideCatalogRepo(): CatalogRepo = FakeCatalogRepo()

  // Presenter -> VM class map entry
  @Provides @IntoMap @ClassKey(CatalogPresenter::class)
  fun bindCatalogPresenter(): Class<out ViewModel> = CatalogViewModel::class.java
}
