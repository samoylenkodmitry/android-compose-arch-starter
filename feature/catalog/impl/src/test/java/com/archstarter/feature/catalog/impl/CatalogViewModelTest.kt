package com.archstarter.feature.catalog.impl

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.NavigationActions
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

  @get:Rule
  val dispatcherRule = MainDispatcherRule()

  @Test
  fun onRefreshUpdatesStateWithRepoItems() = runTest {
    val data = MutableStateFlow(listOf<ArticleEntity>())
    val repo = object : ArticleRepo {
      override val articles: StateFlow<List<ArticleEntity>> = data
      override fun article(id: Int): Flow<ArticleEntity?> =
        data.map { articles -> articles.firstOrNull { it.id == id } }
      override suspend fun refresh() {
        data.value = listOf(
          articleEntity(1, "One", 0L),
          articleEntity(2, "Two", 1L)
        )
      }
      override suspend fun translateArticle(id: Int): ArticleEntity? =
        data.value.firstOrNull { it.id == id }
      override suspend fun translate(word: String): String? = word
    }
    val nav = object : NavigationActions { override fun openDetail(id: Int) {}; override fun openSettings() {} }
    val bridge = CatalogBridge()
    val screenBus = ScreenBus()
    val handle = SavedStateHandle()
    val vm = CatalogViewModel(repo, App(nav), bridge, screenBus, handle)

    vm.onRefresh()
    advanceUntilIdle()

    assertEquals(listOf(1, 2), vm.state.value.items)
  }

  @Test
  fun initFetchesTenItems() = runTest {
    var refreshCount = 0
    val data = MutableStateFlow(listOf<ArticleEntity>())
    val repo = object : ArticleRepo {
      override val articles: StateFlow<List<ArticleEntity>> = data
      override fun article(id: Int): Flow<ArticleEntity?> =
        data.map { articles -> articles.firstOrNull { it.id == id } }
      override suspend fun refresh() {
        refreshCount++
        val id = refreshCount
        data.value = listOf(
          articleEntity(id, "Title$id", id.toLong())
        ) + data.value
      }
      override suspend fun translateArticle(id: Int): ArticleEntity? = null
      override suspend fun translate(word: String): String? = word
    }
    val nav = object : NavigationActions { override fun openDetail(id: Int) {}; override fun openSettings() {} }
    val bridge = CatalogBridge()
    val screenBus = ScreenBus()
    val handle = SavedStateHandle()
    val vm = CatalogViewModel(repo, App(nav), bridge, screenBus, handle)

    advanceUntilIdle()

    assertEquals(10, refreshCount)
    assertEquals((10 downTo 1).toList(), vm.state.value.items)
  }

  @Test
  fun initDoesNotFetchWhenArticlesAlreadyExist() = runTest {
    var refreshCount = 0
    val existing = articleEntity(1, "One", 0L)
    val data = MutableStateFlow(listOf(existing))
    val repo = object : ArticleRepo {
      override val articles: StateFlow<List<ArticleEntity>> = data
      override fun article(id: Int): Flow<ArticleEntity?> =
        data.map { articles -> articles.firstOrNull { it.id == id } }
      override suspend fun refresh() { refreshCount++ }
      override suspend fun translateArticle(id: Int): ArticleEntity? =
        data.value.firstOrNull { it.id == id }
      override suspend fun translate(word: String): String? = word
    }
    val nav = object : NavigationActions { override fun openDetail(id: Int) {}; override fun openSettings() {} }
    val bridge = CatalogBridge()
    val screenBus = ScreenBus()
    val handle = SavedStateHandle()
    val vm = CatalogViewModel(repo, App(nav), bridge, screenBus, handle)

    advanceUntilIdle()

    assertEquals(0, refreshCount)
    assertEquals(listOf(existing.id), vm.state.value.items)
  }
}

private fun articleEntity(id: Int, title: String, createdAt: Long) = ArticleEntity(
  id = id,
  title = title,
  summaryOriginal = "S",
  summaryTranslated = null,
  contentOriginal = "C",
  contentTranslated = null,
  originalWord = null,
  translatedWord = null,
  ipa = null,
  sourceUrl = "u",
  createdAt = createdAt
)
