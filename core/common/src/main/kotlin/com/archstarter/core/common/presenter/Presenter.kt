package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LifecycleStartEffect
import com.archstarter.core.common.BuildConfig
import kotlin.reflect.KClass

interface ParamInit<P> {
  fun initOnce(params: P?)
}

/** Implement in a presenter to be notified when its screen leaves the foreground (ON_STOP). */
interface LifecycleStop {
  fun onStop()
}

interface PresenterResolver {
  @Composable
  fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T
}

val LocalPresenterResolver = staticCompositionLocalOf<PresenterResolver?> { null }

val LocalCurrentPresenter = compositionLocalOf<Any?> { null }

/** Resolve presenter and auto-init params via ParamInit if present. */
@Composable
inline fun <reified P : ParamInit<Params>, Params> rememberPresenter(
  key: String? = null,
  params: Params? = null,
): P {
  val presenter = LocalCurrentPresenter.current as? P
    ?: LocalPresenterResolver.current?.resolve(P::class, key)
    ?: presenterMock(P::class, key)
    ?: error("No presenter for ${P::class.simpleName} with key=$key")

  // LifecycleStartEffect (not LaunchedEffect) so initOnce re-runs each time the screen returns to
  // the foreground and LifecycleStop.onStop fires when it leaves — keeps presenter subscriptions in
  // step with visibility instead of running once and leaking while off-screen.
  LifecycleStartEffect(presenter, params) {
    (presenter as? ParamInit<Params>)?.initOnce(params)
    onStopOrDispose {
      (presenter as? LifecycleStop)?.onStop()
    }
  }
  return presenter
}

@PublishedApi
internal fun <P : ParamInit<*>> presenterMock(
  klass: KClass<P>,
  key: String?,
): P? {
  if (!BuildConfig.DEBUG) return null
  val keyed = MocksMap[PresenterMockKey(klass, key)]
  val fallback = MocksMap[PresenterMockKey(klass, null)]
  @Suppress("UNCHECKED_CAST")
  return (keyed ?: fallback) as? P
}
