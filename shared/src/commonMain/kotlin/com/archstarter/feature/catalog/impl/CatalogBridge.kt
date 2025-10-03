package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.api.CatalogItemBridge
import com.archstarter.core.common.scope.ScreenScope
import me.tatarka.inject.annotations.Inject

@ScreenScope
@Inject
class CatalogBridge : CatalogItemBridge {
    private var delegate: CatalogItemBridge? = null

    fun setDelegate(d: CatalogItemBridge) {
        delegate = d
    }

    override fun onItemClick(id: Int) {
        delegate?.onItemClick(id)
    }
}