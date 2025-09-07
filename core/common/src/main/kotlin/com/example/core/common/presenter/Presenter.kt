package com.example.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.reflect.KClass

interface ParamInit<P> { fun initOnce(params: P) }

typealias PresenterKey = String?

interface PresenterResolver {
  @Composable fun <T : Any> resolve(klass: KClass<T>, key: PresenterKey?): T
}

val LocalPresenterResolver = staticCompositionLocalOf<PresenterResolver> {
  error("LocalPresenterResolver not provided")
}

/** Resolve presenter and auto-init params via ParamInit if present. */
@Composable
inline fun <reified P: Any, Params> rememberPresenter(
  key: PresenterKey = null,
  params: Params? = null
): P {
  val p = LocalPresenterResolver.current.resolve(P::class, key)
  LaunchedEffect(p, params) {
    if (params != null) (p as? ParamInit<Params>)?.initOnce(params)
  }
  return p
}
