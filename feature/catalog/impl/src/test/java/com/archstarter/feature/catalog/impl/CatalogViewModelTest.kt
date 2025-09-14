package com.archstarter.feature.catalog.impl

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.NavigationActions
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
      override suspend fun refresh() { data.value = listOf(ArticleEntity(1,"One","S","C","u","o","t",null), ArticleEntity(2,"Two","S","C","u","o","t",null)) }
      override suspend fun article(id: Int): ArticleEntity? = data.value.firstOrNull { it.id == id }
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
}
