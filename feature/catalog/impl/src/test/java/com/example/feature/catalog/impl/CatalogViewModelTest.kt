package com.example.feature.catalog.impl

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val repo = object : CatalogRepo {
      override suspend fun items(): List<String> = listOf("One", "Two")
    }
    val vm = CatalogViewModel(repo, SavedStateHandle())

    vm.onRefresh()
    advanceUntilIdle()

    assertEquals(listOf("One", "Two"), vm.state.value.items)
  }
}
