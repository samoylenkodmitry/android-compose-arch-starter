package com.example.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.detail.api.DetailPresenter
import com.example.feature.detail.api.DetailState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface DetailRepo { suspend fun detail(id: String): String }

class FakeDetailRepo @Inject constructor() : DetailRepo {
  override suspend fun detail(id: String): String {
    delay(100)
    return "Details for $id"
  }
}

@HiltViewModel
class DetailViewModel @Inject constructor(
  private val repo: DetailRepo,
  private val saved: SavedStateHandle
) : ViewModel(), DetailPresenter {
  private val _state = MutableStateFlow(DetailState())
  override val state: StateFlow<DetailState> = _state
  private var initialized = false
  override fun initOnce(params: String) {
    if (initialized) return
    initialized = true
    viewModelScope.launch {
      _state.value = DetailState(repo.detail(params))
    }
  }
}

@Module
@InstallIn(SingletonComponent::class)
object DetailBindings {
  @Provides fun provideDetailRepo(): DetailRepo = FakeDetailRepo()

  @Provides @IntoMap @ClassKey(DetailPresenter::class)
  fun bindDetailPresenter(): Class<out ViewModel> = DetailViewModel::class.java
}
