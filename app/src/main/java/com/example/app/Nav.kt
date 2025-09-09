package com.example.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.common.app.App
import com.example.core.common.presenter.LocalPresenterResolver
import com.example.core.designsystem.AppTheme
import com.example.feature.catalog.api.Catalog
import com.example.feature.catalog.ui.CatalogScreen
import com.example.feature.detail.api.Detail
import com.example.feature.detail.ui.DetailScreen
import com.example.feature.settings.api.Settings
import com.example.feature.settings.ui.SettingsScreen
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val nav = rememberNavController()
                val app = remember(nav) {
                    val actions = NavigationActions(nav)
                    App(actions)
                }
                val resolver = remember { KoinPresenterResolver() }
                DisposableEffect(app) {
                    val koin = GlobalContext.get()
                    val mod = module { single { app } }
                    koin.loadModules(listOf(mod), allowOverride = true)
                    onDispose { koin.unloadModules(listOf(mod)) }
                }
                CompositionLocalProvider(
                    LocalPresenterResolver provides resolver
                ) {
                    Box(
                        modifier = Modifier
                            .imePadding()
                            .navigationBarsPadding()
                            .systemBarsPadding()
                            .safeContentPadding()
                            .fillMaxSize()
                    ) {
                        AppNavHost(nav)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(nav: NavHostController) {
    NavHost(nav, startDestination = Catalog) {
        composable<Catalog> { CatalogScreen() }
        composable<Detail> {
            val args = it.toRoute<Detail>()
            DetailScreen(args.id)
        }
        composable<Settings> { SettingsScreen() }
    }
}
