package com.example.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.catalog.api.PresenterKey
import com.example.feature.catalog.api.PresenterResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiltPresenterResolver @Inject constructor(
  private val map: Map<Class<*>, @JvmSuppressWildcards Class<out ViewModel>>
) : PresenterResolver {

  @Composable
  override fun <T : Any> resolve(klass: kotlin.reflect.KClass<T>, key: PresenterKey): T {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
      "No ViewModelStoreOwner in composition"
    }
    val vmClass = map[klass.java] ?: error("No binding for presenter ${klass.simpleName}")
    @Suppress("UNCHECKED_CAST")
    return viewModel(modelClass = vmClass, viewModelStoreOwner = owner, key = key) as T
  }
}
