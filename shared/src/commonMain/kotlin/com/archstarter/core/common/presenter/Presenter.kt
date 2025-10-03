package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.archstarter.core.common.scope.HasPresenterFactories
import com.archstarter.core.common.scope.LocalScreenComponent

interface ParamInit<P> {
    fun initOnce(params: P?)
}

@Composable
inline fun <reified P : ParamInit<Params>, Params> rememberPresenter(
    params: Params? = null,
): P {
    val component = LocalScreenComponent.current as HasPresenterFactories
    val presenterFactory = remember(component) {
        component.presenterFactories[P::class]
            ?: error("No presenter factory for ${P::class.simpleName}")
    }

    val presenter = remember(params) {
        @Suppress("UNCHECKED_CAST")
        (presenterFactory(params) as P)
    }

    LaunchedEffect(presenter, params) {
        presenter.initOnce(params)
    }

    return presenter
}