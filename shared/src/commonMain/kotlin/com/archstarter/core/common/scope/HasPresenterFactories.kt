package com.archstarter.core.common.scope

import kotlin.reflect.KClass

typealias PresenterFactory = (Any?) -> Any

interface HasPresenterFactories {
    val presenterFactories: Map<KClass<*>, PresenterFactory>
}