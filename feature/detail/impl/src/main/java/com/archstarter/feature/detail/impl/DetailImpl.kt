package com.archstarter.feature.detail.impl

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
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.detail.api.DetailPresenter
import com.archstarter.feature.detail.api.DetailState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class DetailViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), DetailPresenter {

    init {
        println("DetailsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onCleared() {
        super.onCleared()
        println("DetailsViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }
    private val _state = MutableStateFlow(DetailState())
    override val state: StateFlow<DetailState> = _state
    private var initialized = false
    private val translationCache = mutableMapOf<String, String>()

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
                cacheTranslation(it.originalWord, it.translatedWord)
                screenBus.send("Detail loaded for article $params: ${it.title}")
            }
        }
    }

    override fun translate(word: String) {
        viewModelScope.launch {
            val normalizedWord = normalizeWord(word)
            if (normalizedWord.isEmpty()) return@launch

            translationCache[cacheKey(normalizedWord)]?.let { cached ->
                updateHighlighted(normalizedWord, cached)
                return@launch
            }

            val translation = repo.translate(normalizedWord)?.let(::normalizeTranslation)
            if (translation.isNullOrEmpty()) return@launch

            cacheTranslation(normalizedWord, translation)
            updateHighlighted(normalizedWord, translation)
        }
    }

    private fun updateHighlighted(word: String, translation: String) {
        _state.value = _state.value.copy(
            highlightedWord = word,
            highlightedTranslation = translation
        )
    }

    private fun cacheTranslation(word: String, translation: String) {
        val normalizedWord = normalizeWord(word)
        val normalizedTranslation = normalizeTranslation(translation)
        if (normalizedWord.isEmpty() || normalizedTranslation.isEmpty()) return
        translationCache[cacheKey(normalizedWord)] = normalizedTranslation
    }

    private fun normalizeWord(word: String): String = word.trim()

    private fun normalizeTranslation(translation: String): String = translation.trim()

    private fun cacheKey(word: String): String = word.lowercase(Locale.ROOT)

    @AssistedFactory
    interface Factory : AssistedVmFactory<DetailViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object DetailPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(DetailPresenter::class)
    fun provideDetailPresenterProvider(): PresenterProvider<*> {
        return object : PresenterProvider<DetailPresenter> {
            @Composable
            override fun provide(key: String?): DetailPresenter {
                return scopedViewModel<DetailViewModel>(key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class DetailVmBindingModule {

    @Binds
    @IntoMap
    @VmKey(DetailViewModel::class)
    abstract fun detailFactory(f: DetailViewModel.Factory): AssistedVmFactory<out ViewModel>
}
