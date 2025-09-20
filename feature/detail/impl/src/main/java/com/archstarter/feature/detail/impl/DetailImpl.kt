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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    @Assisted private val handle: SavedStateHandle,
) : ViewModel(), DetailPresenter {

    init {
        println("DetailsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
        println("DetailsViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    private val articleIds = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    private val translationRequests = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val highlightState = MutableStateFlow(Highlight())
    private val wordTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    private val translationCache = mutableMapOf<String, String>()
    private var announcedArticleId: Int? = null
    private var prefetchJob: Job? = null

    private val articleState = articleIds
        .flatMapLatest { id -> repo.article(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val articles = articleState.filterNotNull()

    override val state: StateFlow<DetailState> = combine(articles, highlightState, wordTranslations) { entity, highlight, translations ->
        val content = entity.contentTranslated ?: entity.contentOriginal
        DetailState(
            title = entity.title,
            content = content,
            sourceUrl = entity.sourceUrl,
            originalWord = entity.originalWord.orEmpty(),
            translatedWord = entity.translatedWord.orEmpty(),
            ipa = entity.ipa,
            highlightedWord = highlight.word,
            highlightedTranslation = highlight.translation,
            wordTranslations = translations,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DetailState(),
    )

    init {
        translationRequests
            .onEach { id -> repo.translateArticle(id) }
            .launchIn(viewModelScope)

        articles
            .onEach { entity ->
                cacheTranslation(entity.originalWord, entity.translatedWord)
                prefetchContentTranslations(entity.contentTranslated ?: entity.contentOriginal)
                if (announcedArticleId != entity.id) {
                    screenBus.send("Detail loaded for article ${entity.id}: ${entity.title}")
                    announcedArticleId = entity.id
                }
            }
            .launchIn(viewModelScope)

        articles
            .distinctUntilChanged { old, new ->
                old.id == new.id &&
                    old.summaryTranslated == new.summaryTranslated &&
                    old.contentTranslated == new.contentTranslated
            }
            .onEach { entity ->
                if (entity.summaryTranslated == null || entity.contentTranslated == null) {
                    translationRequests.emit(entity.id)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun initOnce(params: Int) {
        articleIds.tryEmit(params)
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
        highlightState.value = Highlight(word, translation)
    }

    private fun cacheTranslation(word: String?, translation: String?) {
        val normalizedWord = normalizeWord(word.orEmpty())
        val normalizedTranslation = normalizeTranslation(translation.orEmpty())
        if (normalizedWord.isEmpty() || normalizedTranslation.isEmpty()) return
        val key = cacheKey(normalizedWord)
        val previous = translationCache.put(key, normalizedTranslation)
        if (previous != normalizedTranslation) {
            wordTranslations.value = translationCache.toMap()
        }
    }

    private fun normalizeWord(word: String): String = word.trim()

    private fun normalizeTranslation(translation: String): String = translation.trim()

    private fun cacheKey(word: String): String = word.lowercase(Locale.ROOT)

    private fun prefetchContentTranslations(content: String) {
        if (content.isBlank()) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val words = extractWords(content)
            for ((key, candidate) in words) {
                if (translationCache.containsKey(key)) continue
                val translation = repo.translate(candidate)?.let(::normalizeTranslation)
                if (!translation.isNullOrEmpty()) {
                    cacheTranslation(candidate, translation)
                }
            }
        }
    }

    private fun extractWords(content: String): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        WORD_REGEX.findAll(content).forEach { match ->
            val rawWord = normalizeWord(match.value)
            if (rawWord.length <= 1) return@forEach
            val key = cacheKey(rawWord)
            if (key !in map) {
                map[key] = rawWord
            }
        }
        return map
    }

    private data class Highlight(
        val word: String? = null,
        val translation: String? = null,
    )

    companion object {
        private val WORD_REGEX = Regex("[\\p{L}\\p{Nd}']+")
    }

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
