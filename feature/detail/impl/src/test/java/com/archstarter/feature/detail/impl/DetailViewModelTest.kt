package com.archstarter.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  @Test
  fun translateReusesCachedTranslation() = runTest {
    val article = sampleArticle()
    val repo = FakeArticleRepo(article) { word, call -> "$word-$call" }
    val vm = DetailViewModel(repo, ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    vm.translate("Hover")
    advanceUntilIdle()

    assertEquals("Hover-1", vm.state.value.highlightedTranslation)
    assertEquals(1, repo.translateCalls)

    vm.translate("Hover")
    advanceUntilIdle()

    assertEquals("Hover-1", vm.state.value.highlightedTranslation)
    assertEquals(1, repo.translateCalls)
  }

  @Test
  fun translateUsesCachedArticleTranslation() = runTest {
    val article = sampleArticle(original = "Original", translated = "Translated")
    val repo = FakeArticleRepo(article) { word, call -> "$word-$call" }
    val vm = DetailViewModel(repo, ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    vm.translate("Original")
    advanceUntilIdle()

    assertEquals(0, repo.translateCalls)
    assertEquals("Original", vm.state.value.highlightedWord)
    assertEquals("Translated", vm.state.value.highlightedTranslation)
  }

  @Test
  fun translateCachesAcrossWordCaseDifferences() = runTest {
    val article = sampleArticle()
    val repo = FakeArticleRepo(article) { word, call -> "$word-$call" }
    val vm = DetailViewModel(repo, ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    vm.translate("Word")
    advanceUntilIdle()

    assertEquals(1, repo.translateCalls)
    assertEquals("Word-1", vm.state.value.highlightedTranslation)

    vm.translate("word")
    advanceUntilIdle()

    assertEquals(1, repo.translateCalls)
    assertEquals("Word-1", vm.state.value.highlightedTranslation)
  }

  @Test
  fun initRequestsTranslationAndUpdatesWhenAvailable() = runTest {
    val untranslated = sampleArticle(summary = "Summary", content = "Content")
      .copy(
        summaryTranslated = null,
        contentTranslated = null,
        originalWord = null,
        translatedWord = null
      )
    val translated = untranslated.copy(
      summaryTranslated = "Translated summary",
      contentTranslated = "Translated content",
      originalWord = "Word",
      translatedWord = "Palabra"
    )
    val repo = FakeArticleRepo(untranslated) { _, _ -> null }
    repo.publishOnTranslate = false
    repo.translateArticleResult = translated
    val vm = DetailViewModel(repo, ScreenBus(), SavedStateHandle())

    vm.initOnce(untranslated.id)
    advanceUntilIdle()

    assertEquals("Content", vm.state.value.content)
    assertEquals(1, repo.translateArticleCalls)

    repo.publishTranslation()
    advanceUntilIdle()

    assertEquals("Translated content", vm.state.value.content)
    assertEquals("Word", vm.state.value.originalWord)
    assertEquals("Palabra", vm.state.value.translatedWord)
  }

  private fun sampleArticle(
    id: Int = 1,
    title: String = "Title",
    summary: String = "Summary",
    content: String = "Content",
    sourceUrl: String = "https://example.com",
    original: String = "Original",
    translated: String = "Translated",
    ipa: String? = null,
    createdAt: Long = 0L,
  ) = ArticleEntity(
    id = id,
    title = title,
    summaryOriginal = summary,
    summaryTranslated = summary,
    contentOriginal = content,
    contentTranslated = content,
    originalWord = original,
    translatedWord = translated,
    ipa = ipa,
    sourceUrl = sourceUrl,
    createdAt = createdAt,
  )

  private class FakeArticleRepo(
    initialArticle: ArticleEntity?,
    private val translationProvider: (String, Int) -> String?,
  ) : ArticleRepo {
    override val articles: StateFlow<List<ArticleEntity>> = MutableStateFlow(emptyList())
    private val articleState = MutableStateFlow(initialArticle)
    var translateCalls: Int = 0
    var translateArticleCalls: Int = 0
    var translateArticleResult: ArticleEntity? = initialArticle
    var publishOnTranslate: Boolean = true

    override suspend fun refresh() {}

    override fun article(id: Int): Flow<ArticleEntity?> = articleState

    override suspend fun translateArticle(id: Int): ArticleEntity? {
      translateArticleCalls += 1
      if (publishOnTranslate) {
        translateArticleResult?.let { articleState.value = it }
      }
      return translateArticleResult
    }

    override suspend fun translate(word: String): String? {
      translateCalls += 1
      return translationProvider(word, translateCalls)
    }

    fun publishTranslation() {
      translateArticleResult?.let { articleState.value = it }
    }
  }
}
