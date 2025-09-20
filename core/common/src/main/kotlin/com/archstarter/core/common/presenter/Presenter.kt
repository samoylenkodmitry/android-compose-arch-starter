package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.reflect.KClass

interface ParamInit<P> {
    fun initOnce(params: P?)
}

interface PresenterResolver {
    @Composable
    fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T
}

val LocalPresenterResolver = staticCompositionLocalOf<PresenterResolver?> { null }

val LocalCurrentPresenter = compositionLocalOf<Any?> { null }

@Composable
inline fun <reified P : ParamInit<Params>, Params> rememberPresenter(
    key: String? = null,
    params: Params? = null,
): P {
    val presenter = LocalCurrentPresenter.current as? P
        ?: LocalPresenterResolver.current?.resolve(P::class, key)
        ?: findPresenterMock(P::class, key) as? P
        ?: error("No presenter for ${P::class} with key=$key")

    LaunchedEffect(presenter, params) {
        (presenter as? ParamInit<Params>)?.initOnce(params)
    }
    return presenter
}

@Composable
inline fun <reified P : Any> ProvidePresenter(
    presenter: P,
    crossinline content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCurrentPresenter provides presenter) {
        content()
    }
}
