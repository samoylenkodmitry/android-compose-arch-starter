package com.archstarter.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.NavigationActions
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Hover")
    advanceUntilIdle()

    val first = requireNotNull(vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)

    vm.translate("Hover")
    advanceUntilIdle()

    assertEquals(first, vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
  }

  @Test
  fun translateUsesCachedArticleTranslation() = runTest {
    val article = sampleArticle(original = "Original", translated = "Translated")
    val repo = FakeArticleRepo(article) { word, call -> "$word-$call" }
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Original")
    advanceUntilIdle()

    assertEquals(baselineCalls, repo.translateCalls)
    assertEquals("Original", vm.state.value.highlightedWord)
    assertEquals("Translated", vm.state.value.highlightedTranslation)
  }

  @Test
  fun translateCachesAcrossWordCaseDifferences() = runTest {
    val article = sampleArticle()
    val repo = FakeArticleRepo(article) { word, call -> "$word-$call" }
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Word")
    advanceUntilIdle()

    val first = requireNotNull(vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)

    vm.translate("word")
    advanceUntilIdle()

    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)
  }

  @Test
  fun prefetchPopulatesWordTranslations() = runTest {
    val article = sampleArticle(content = "Alpha beta alpha", original = "seed", translated = "sprout")
    val repo = FakeArticleRepo(article) { word, _ -> "${word.lowercase(Locale.ROOT)}-t" }
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    val translations = vm.state.value.wordTranslations
    assertEquals("sprout", translations["seed"])
    assertEquals("alpha-t", translations["alpha"])
    assertEquals("beta-t", translations["beta"])
  }

  @Test
  fun onSourceClickOpensLink() = runTest {
    val repo = FakeArticleRepo(null) { _, _ -> null }
    val nav = RecordingNavigationActions()
    val vm = DetailViewModel(repo, App(nav), ScreenBus(), SavedStateHandle())

    vm.onSourceClick("")
    vm.onSourceClick("https://example.com")

    assertEquals(listOf("https://example.com"), nav.openedLinks)
  }

  private class RecordingNavigationActions : NavigationActions {
    val openedLinks = mutableListOf<String>()

    override fun openDetail(id: Int) {}

    override fun openSettings() {}

    override fun openLink(url: String) {
      openedLinks += url
    }
  }

  private fun sampleArticle(
    id: Int = 1,
    title: String = "Title",
    summary: String = "Summary",
    summaryLanguage: String? = null,
    content: String = "Content",
    sourceUrl: String = "https://example.com",
    original: String = "Original",
    translated: String = "Translated",
    ipa: String? = null,
    createdAt: Long = 0L,
  ) = ArticleEntity(
    id = id,
    title = title,
    summary = summary,
    summaryLanguage = summaryLanguage,
    content = content,
    sourceUrl = sourceUrl,
    originalWord = original,
    translatedWord = translated,
    ipa = ipa,
    createdAt = createdAt,
  )

  private class FakeArticleRepo(
    private val article: ArticleEntity?,
    private val translationProvider: (String, Int) -> String?,
  ) : ArticleRepo {
    override val articles: StateFlow<List<ArticleEntity>> = MutableStateFlow(emptyList())
    var translateCalls: Int = 0

    override suspend fun refresh() {}

    override suspend fun article(id: Int): ArticleEntity? = article

    override fun articleFlow(id: Int): Flow<ArticleEntity?> = flowOf(article)

    override suspend fun translateSummary(article: ArticleEntity): String? = article.summary

    override suspend fun translate(word: String): String? {
      translateCalls += 1
      return translationProvider(word, translateCalls)
    }
  }
}
