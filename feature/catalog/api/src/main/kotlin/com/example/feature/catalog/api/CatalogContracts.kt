package com.example.feature.catalog.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Catalog

// Presenter & state
data class CatalogState(val items: List<String> = emptyList())

interface CatalogPresenter {
  val state: StateFlow<CatalogState>
  fun onRefresh()
}
