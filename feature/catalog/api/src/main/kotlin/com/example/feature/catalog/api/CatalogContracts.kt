package com.example.feature.catalog.api

import kotlinx.coroutines.flow.StateFlow

// Presenter & state
data class CatalogState(val items: List<String> = emptyList())

interface CatalogPresenter {
  val state: StateFlow<CatalogState>
  fun onRefresh()
}
