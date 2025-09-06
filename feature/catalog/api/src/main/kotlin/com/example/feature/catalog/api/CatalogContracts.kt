package com.example.feature.catalog.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

// Presenter & state
data class CatalogState(val items: List<String> = emptyList())

interface CatalogPresenter {
  val state: StateFlow<CatalogState>
  fun onRefresh()
}

// Param init marker (for param'd presenters if needed)
interface ParamInit<P> { fun initOnce(params: P) }

// Resolver API (UI-friendly, no Hilt here)
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
