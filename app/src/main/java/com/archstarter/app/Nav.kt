package com.archstarter.app

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
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.LocalPresenterResolver
import com.archstarter.core.common.scope.LocalScreenBuilder
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.catalog.api.Catalog
import com.archstarter.feature.catalog.ui.CatalogScreen
import com.archstarter.feature.detail.api.Detail
import com.archstarter.feature.detail.ui.DetailScreen
import com.archstarter.feature.settings.api.Settings
import com.archstarter.feature.settings.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var resolver: HiltPresenterResolver

    @Inject
    lateinit var appManager: AppScopeManager

    @Inject
    lateinit var screenBuilder: ScreenComponent.Builder

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val nav = rememberNavController()
                val app = remember(nav) {
                    val actions = NavigationActions(nav)
                    App(actions).also { appManager.create(it) }
                }
                DisposableEffect(app) {
                    onDispose { appManager.clear() }
                }
                CompositionLocalProvider(
                    LocalPresenterResolver provides resolver,
                    LocalScreenBuilder provides screenBuilder
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
        composable<Catalog> { 
            ScreenScope {
                CatalogScreen() 
            }
        }
        composable<Detail> {
            val args = it.toRoute<Detail>()
            ScreenScope {
                DetailScreen(args.id)
            }
        }
        composable<Settings> { 
            ScreenScope {
                SettingsScreen() 
            }
        }
    }
}
