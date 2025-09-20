package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.PublishedApi
import kotlin.reflect.KClass

interface ParamInit<P> {
    fun initOnce(params: P?)
}

interface PresenterResolver {
    @Composable
    fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T
}

val LocalPresenterResolver = staticCompositionLocalOf<PresenterResolver?> { null }

data class PresenterOverrideKey(
    val klass: KClass<*>,
    val key: String?,
)

@PublishedApi
internal val LocalPresenterOverrides = compositionLocalOf<Map<PresenterOverrideKey, Any>> { emptyMap() }

data class PresenterMockEntry(
    val overrideKey: PresenterOverrideKey,
    val factory: () -> ParamInit<*>,
)

class PresenterMocks internal constructor(
    internal val entries: List<PresenterMockEntry>,
)

@PublishedApi
internal object PresenterPreviewRegistry {
    private val factories = LinkedHashMap<PresenterOverrideKey, () -> ParamInit<*>>()
    private val active = LinkedHashMap<PresenterOverrideKey, ParamInit<*>>()

    @Synchronized
    fun register(entries: List<PresenterMockEntry>) {
        entries.forEach { entry ->
            factories[entry.overrideKey] = entry.factory
            removeActiveFor(entry)
        }
    }

    @Synchronized
    fun unregister(entries: List<PresenterMockEntry>) {
        entries.forEach { entry ->
            factories.remove(entry.overrideKey)
            removeActiveFor(entry)
        }
    }

    @Synchronized
    fun resolve(klass: KClass<*>, key: String?): ParamInit<*>? {
        val directKey = PresenterOverrideKey(klass, key)
        resolveDirect(directKey)?.let { return it }
        if (key != null) {
            val fallbackKey = PresenterOverrideKey(klass, null)
            val fallbackFactory = factories[fallbackKey] ?: return null
            val presenter = fallbackFactory()
            active[directKey] = presenter
            return presenter
        }
        return null
    }

    private fun resolveDirect(key: PresenterOverrideKey): ParamInit<*>? {
        active[key]?.let { return it }
        val factory = factories[key] ?: return null
        val presenter = factory()
        active[key] = presenter
        return presenter
    }

    private fun removeActiveFor(entry: PresenterMockEntry) {
        val overrideKey = entry.overrideKey
        if (overrideKey.key == null) {
            val iterator = active.entries.iterator()
            while (iterator.hasNext()) {
                val existing = iterator.next()
                if (existing.key.klass == overrideKey.klass) {
                    iterator.remove()
                }
            }
        } else {
            active.remove(overrideKey)
        }
    }
}

/******** Helpers for preview mocks ********/

inline fun <reified P : ParamInit<*>> presenterMock(
    key: String? = null,
    noinline factory: () -> P,
): PresenterMockEntry = PresenterMockEntry(PresenterOverrideKey(P::class, key), factory)

fun presenterMocksOf(vararg entries: PresenterMockEntry): PresenterMocks =
    PresenterMocks(entries.asList())

/*******************************************/

/** Resolve presenter and auto-init params via ParamInit if present. */
@Composable
inline fun <reified P : ParamInit<Params>, Params> rememberPresenter(
    key: String? = null,
    params: Params? = null
): P {
    val overrides = LocalPresenterOverrides.current
    val overrideKey = PresenterOverrideKey(P::class, key)
    val fallbackKey = PresenterOverrideKey(P::class, null)
    val presenter = overrides[overrideKey] as? P
        ?: overrides[fallbackKey] as? P
        ?: PresenterPreviewRegistry.resolve(P::class, key) as? P
        ?: LocalPresenterResolver.current?.resolve(P::class, key)
        ?: error("No presenter for ${P::class} with key=$key")

    LaunchedEffect(presenter, params) {
        (presenter as? ParamInit<Params>)?.initOnce(params)
    }
    return presenter
}

@Composable
inline fun <reified P : Any> ProvidePresenter(
    presenter: P?,
    key: String? = null,
    crossinline content: @Composable () -> Unit
) {
    val overrides = LocalPresenterOverrides.current
    val overrideKey = PresenterOverrideKey(P::class, key)
    val updated = if (presenter == null) {
        overrides - overrideKey
    } else {
        overrides + (overrideKey to presenter)
    }
    CompositionLocalProvider(LocalPresenterOverrides provides updated) {
        content()
    }
}

@Composable
fun ProvidePresenterMocks(
    mocks: PresenterMocks,
    content: @Composable () -> Unit,
) {
    DisposableEffect(mocks) {
        PresenterPreviewRegistry.register(mocks.entries)
        onDispose { PresenterPreviewRegistry.unregister(mocks.entries) }
    }
    content()
}
