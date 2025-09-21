package com.archstarter.core.common.presenter

import kotlin.collections.HashMap
import kotlin.reflect.KClass

data class PresenterMockKey(
    val klass: KClass<*>,
    val key: String?,
)

val MocksMap: MutableMap<PresenterMockKey, ParamInit<*>> = HashMap()

@PublishedApi
internal fun findPresenterMock(klass: KClass<*>, key: String?): ParamInit<*>? {
    val direct = MocksMap[PresenterMockKey(klass, key)]
    if (direct != null) {
        return direct
    }
    if (key != null) {
        return MocksMap[PresenterMockKey(klass, null)]
    }
    return null
}

internal fun clearPresenterMocks() {
    MocksMap.clear()
}
