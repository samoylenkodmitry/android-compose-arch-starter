package com.archstarter.feature.catalog.impl

import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.feature.catalog.api.CatalogItem
import com.archstarter.feature.catalog.api.CatalogItemPresenter
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import com.archstarter.feature.settings.api.languageCodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ScreenScope
@Inject
class CatalogItemPresenterImpl(
    private val repo: ArticleRepo,
    private val bridge: CatalogBridge,
    private val screenBus: ScreenBus,
    private val settingsStateProvider: SettingsStateProvider,
) : CatalogItemPresenter, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main) {

    private val params = MutableSharedFlow<Int>(replay = 1)
    private val articles = params.flatMapLatest { id -> repo.articleFlow(id) }
    private val languagePairs = settingsStateProvider.state
        .map { it.toLanguagePairOrNull() }
        .distinctUntilChanged()

    private val translations = combine(articles, languagePairs) { article, languages ->
        article to languages
    }.flatMapLatest { (article, languages) ->
        val currentArticle = article
        val currentLanguages = languages
        if (currentArticle == null || currentLanguages == null) {
            flowOf<String?>(null)
        } else {
            flow {
                emit(null)
                val translated = runCatching {
                    repo.translateSummary(
                        currentArticle,
                        currentLanguages.learning,
                        currentLanguages.native,
                    )
                }.getOrNull()
                if (!translated.isNullOrBlank() && translated != currentArticle.summary) {
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
        }.stateIn(this, SharingStarted.WhileSubscribed(5_000), CatalogItem(0, "", ""))

    override fun initOnce(params: Int?) {
        val actual = params ?: return
        launch {
            this@CatalogItemPresenterImpl.params.emit(actual)
        }
    }

    override fun onClick() {
        val current = state.value
        bridge.onItemClick(current.id)
        screenBus.send("Item ${current.id} clicked at ${System.currentTimeMillis()}")
    }
}

private data class LanguagePair(val native: String, val learning: String)

private fun SettingsState.toLanguagePairOrNull(): LanguagePair? {
    val native = languageCodes[nativeLanguage]
    val learning = languageCodes[learningLanguage]
    if (native.isNullOrBlank() || learning.isNullOrBlank()) return null
    return LanguagePair(native, learning)
}