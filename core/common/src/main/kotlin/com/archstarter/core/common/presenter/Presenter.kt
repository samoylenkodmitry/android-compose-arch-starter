package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.reflect.KClass

interface ParamInit<P> { fun initOnce(params: P) }

interface PresenterResolver {
  @Composable fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String??): T
}

val LocalPresenterResolver = staticCompositionLocalOf<PresenterResolver> {
  error("LocalPresenterResolver not provided")
}

/** Resolve presenter and auto-init params via ParamInit if present. */
@Composable
inline fun <reified P: ParamInit<Params>, Params> rememberPresenter(
  key: String? = null,
  params: Params? = null
): P {
  val p = LocalPresenterResolver.current.resolve(P::class, key)
  LaunchedEffect(p, params) {
    if (params != null) (p as? ParamInit<Params>)?.initOnce(params)
  }
  return p
}
