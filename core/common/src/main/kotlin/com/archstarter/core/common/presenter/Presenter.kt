package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@PublishedApi
internal data class PresenterOverrideKey(
    val klass: KClass<*>,
    val key: String?,
)

@PublishedApi
internal val LocalPresenterOverrides = compositionLocalOf<Map<PresenterOverrideKey, Any>> { emptyMap() }

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
