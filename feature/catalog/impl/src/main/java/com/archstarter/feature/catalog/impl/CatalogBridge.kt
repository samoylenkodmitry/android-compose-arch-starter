package com.archstarter.feature.catalog.impl

import com.archstarter.core.common.app.AppScope
import com.archstarter.feature.catalog.api.CatalogItemBridge
import me.tatarka.inject.annotations.Inject

@AppScope
class CatalogBridge @Inject constructor() : CatalogItemBridge {
  private var delegate: CatalogItemBridge? = null

  fun setDelegate(d: CatalogItemBridge) { delegate = d }

  override fun onItemClick(id: Int) {
    delegate?.onItemClick(id)
  }
}
