package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleDataBindings

interface CatalogAppGraphBindings :
    ArticleDataBindings,
    CatalogAppBindings,
    CatalogItemAppBindings

interface CatalogScreenGraphBindings :
    CatalogScreenBindings,
    CatalogItemScreenBindings
