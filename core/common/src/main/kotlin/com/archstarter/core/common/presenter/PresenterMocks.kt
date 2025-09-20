package com.archstarter.core.common.presenter

import kotlin.reflect.KClass

data class PresenterMockKey(
    val klass: KClass<*>,
    val key: String?,
)

data class PresenterMockEntry(
    val key: PresenterMockKey,
    val factory: () -> ParamInit<*>,
)

inline fun <reified P : ParamInit<*>> presenterMock(
    key: String? = null,
    noinline factory: () -> P,
): PresenterMockEntry = PresenterMockEntry(PresenterMockKey(P::class, key), factory)

fun registerPresenterMocks(vararg entries: PresenterMockEntry) {
    if (entries.isNotEmpty()) {
        PresenterMocksStore.register(entries.asList())
    }
}

@PublishedApi
internal fun findPresenterMock(klass: KClass<*>, key: String?): ParamInit<*>? =
    PresenterMocksStore.resolve(klass, key)

internal fun clearPresenterMocks() {
    PresenterMocksStore.clear()
}

@PublishedApi
internal object PresenterMocksStore {
    private val factories = LinkedHashMap<PresenterMockKey, () -> ParamInit<*>>()
    private val active = LinkedHashMap<PresenterMockKey, ParamInit<*>>()

    @Synchronized
    fun register(entries: Collection<PresenterMockEntry>) {
        entries.forEach { entry ->
            factories[entry.key] = entry.factory
            removeActiveFor(entry.key)
        }
    }

    @Synchronized
    fun resolve(klass: KClass<*>, key: String?): ParamInit<*>? {
        val directKey = PresenterMockKey(klass, key)
        resolveDirect(directKey)?.let { return it }
        if (key != null) {
            val fallbackKey = PresenterMockKey(klass, null)
            val fallbackFactory = factories[fallbackKey] ?: return null
            val presenter = fallbackFactory()
            active[directKey] = presenter
            return presenter
        }
        return null
    }

    private fun resolveDirect(key: PresenterMockKey): ParamInit<*>? {
        active[key]?.let { return it }
        val factory = factories[key] ?: return null
        val presenter = factory()
        active[key] = presenter
        return presenter
    }

    private fun removeActiveFor(key: PresenterMockKey) {
        if (key.key == null) {
            val iterator = active.entries.iterator()
            while (iterator.hasNext()) {
                val existing = iterator.next()
                if (existing.key.klass == key.klass) {
                    iterator.remove()
                }
            }
        } else {
            active.remove(key)
        }
    }

    @Synchronized
    fun clear() {
        factories.clear()
        active.clear()
    }
}
