package com.example.feature.detail.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.catalog.impl.data.ArticleRepo
import com.example.feature.detail.api.DetailPresenter
import com.example.feature.detail.api.DetailState
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
  private val repo: ArticleRepo
) : ViewModel(), DetailPresenter {
  private val _state = MutableStateFlow(DetailState())
  override val state: StateFlow<DetailState> = _state
  private var initialized = false
  override fun initOnce(params: Int) {
    if (initialized) return
    initialized = true
    viewModelScope.launch {
      repo.article(params)?.let {
        _state.value = DetailState(
          title = it.title,
          content = it.content,
          sourceUrl = it.sourceUrl,
          originalWord = it.originalWord,
          translatedWord = it.translatedWord,
          ipa = it.ipa
        )
      }
    }
  }
}

@Module
@InstallIn(SingletonComponent::class)
object DetailBindings {
  @dagger.Provides @IntoMap @ClassKey(DetailPresenter::class)
  fun bindDetailPresenter(): Class<out ViewModel> = DetailViewModel::class.java
}
