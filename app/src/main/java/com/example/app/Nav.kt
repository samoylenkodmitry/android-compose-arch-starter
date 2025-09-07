package com.example.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.feature.catalog.ui.CatalogScreen
import com.example.feature.detail.ui.DetailScreen
import com.example.feature.catalog.api.Catalog
import com.example.feature.detail.api.Detail
import com.example.core.common.presenter.LocalPresenterResolver
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.core.designsystem.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var resolver: HiltPresenterResolver

  override fun onCreate(savedInstanceState: android.os.Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        androidx.compose.runtime.CompositionLocalProvider(
          LocalPresenterResolver provides resolver
        ) {
          AppNavHost()
        }
      }
    }
  }
}

@Composable
fun AppNavHost() {
  val nav = rememberNavController()
  NavHost(nav, startDestination = Catalog) {
    composable<Catalog> { CatalogScreen(onItemClick = { id -> nav.navigate(Detail(id)) }) }
    composable<Detail> {
      val args = it.toRoute<Detail>()
      DetailScreen(args.id)
    }
  }
}
