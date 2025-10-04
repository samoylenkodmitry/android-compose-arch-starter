package com.archstarter.app

import androidx.compose.runtime.Composable
import com.archstarter.core.common.app.AppScope
import com.archstarter.core.common.presenter.ParamInit
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.presenter.PresenterResolver
import me.tatarka.inject.annotations.Inject
import kotlin.reflect.KClass

@AppScope
@Inject
class InjectPresenterResolver(
    private val presenterProviders: Map<Class<*>, @JvmSuppressWildcards PresenterProvider<*>>,
) : PresenterResolver {

    @Composable
    override fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T {
        val provider = presenterProviders[klass.java]
            ?: error("No presenter binding for ${klass.simpleName}, map: $presenterProviders")

        @Suppress("UNCHECKED_CAST")
        return provider.provide(key) as T
    }
}
