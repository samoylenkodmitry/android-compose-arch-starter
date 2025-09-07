package com.example.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.common.presenter.LocalPresenterResolver
import com.example.core.designsystem.AppTheme
import com.example.feature.catalog.api.Catalog
import com.example.feature.catalog.ui.CatalogScreen
import com.example.feature.detail.api.Detail
import com.example.feature.detail.ui.DetailScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var resolver: HiltPresenterResolver
  @Inject lateinit var navigation: NavigationActions

  override fun onCreate(savedInstanceState: android.os.Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        androidx.compose.runtime.CompositionLocalProvider(
          LocalPresenterResolver provides resolver
        ) {
          AppNavHost(navigation)
        }
      }
    }
  }
}

@Composable
fun AppNavHost(navigation: NavigationActions) {
  val nav = rememberNavController()
  LaunchedEffect(nav) { navigation.bind(nav) }
  NavHost(nav, startDestination = Catalog) {
    composable<Catalog> { CatalogScreen() }
    composable<Detail> {
      val args = it.toRoute<Detail>()
      DetailScreen(args.id)
    }
  }
}
