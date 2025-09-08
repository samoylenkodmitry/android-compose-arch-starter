package com.example.app

import androidx.compose.runtime.Composable
import com.example.core.common.presenter.ParamInit
import com.example.core.common.presenter.PresenterProvider
import com.example.core.common.presenter.PresenterResolver
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HiltPresenterResolver @Inject constructor(
    private val presenterProviders: Map<Class<out ParamInit<*>>, @JvmSuppressWildcards PresenterProvider<*>>
) : PresenterResolver {

  @Composable
  override fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T {
    val provider = presenterProviders[klass.java]
        ?: error("No presenter binding for ${klass.simpleName}, map: $presenterProviders")
    
    @Suppress("UNCHECKED_CAST")
    return provider.provide(key) as T
  }
}
