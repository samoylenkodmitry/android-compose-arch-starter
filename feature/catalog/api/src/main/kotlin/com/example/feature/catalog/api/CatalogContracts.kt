package com.example.feature.catalog.api

import com.example.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Catalog

data class CatalogItem(val id: Int, val title: String, val summary: String)

// Presenter & state
data class CatalogState(val items: List<Int> = emptyList())

interface CatalogItemBridge {
  fun onItemClick(id: Int)
}

interface CatalogPresenter : ParamInit<Unit>, CatalogItemBridge {
  val state: StateFlow<CatalogState>
  fun onRefresh()
  fun onSettingsClick()
}

interface CatalogItemPresenter : ParamInit<Int> {
  val state: StateFlow<CatalogItem>
  fun onClick()
}
