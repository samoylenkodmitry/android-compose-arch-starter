package com.example.app

import androidx.compose.runtime.Composable
import com.example.core.common.presenter.ParamInit
import com.example.core.common.presenter.PresenterResolver
import com.example.feature.catalog.impl.CatalogViewModel
import com.example.feature.detail.impl.DetailViewModel
import com.example.feature.settings.impl.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.KClass

class KoinPresenterResolver : PresenterResolver {
  @Composable
  override fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T {
    val vm: Any = when (klass) {
      CatalogViewModel::class -> koinViewModel<CatalogViewModel>(key = key)
      DetailViewModel::class -> koinViewModel<DetailViewModel>(key = key)
      SettingsViewModel::class -> koinViewModel<SettingsViewModel>(key = key)
      else -> error("No presenter binding for ${klass.simpleName}")
    }
    @Suppress("UNCHECKED_CAST")
    return vm as T
  }
}
