package com.example.feature.catalog.impl

import com.example.feature.catalog.api.CatalogItemBridge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogBridge @Inject constructor() : CatalogItemBridge {
  private var delegate: CatalogItemBridge? = null

  fun setDelegate(d: CatalogItemBridge) { delegate = d }

  override fun onItemClick(id: Int) {
    delegate?.onItemClick(id)
  }
}
