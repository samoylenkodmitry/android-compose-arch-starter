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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.LocalPresenterResolver
import com.archstarter.core.common.scope.LocalScreenComponentFactory
import com.archstarter.core.common.scope.LocalSubscreenComponentFactory
import com.archstarter.core.common.scope.ScreenComponentNode
import com.archstarter.core.common.scope.SubscreenComponentNode
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.catalog.api.Catalog
import com.archstarter.feature.catalog.ui.CatalogScreen
import com.archstarter.feature.detail.api.Detail
import com.archstarter.feature.detail.ui.DetailScreen
import com.archstarter.feature.onboarding.api.Onboarding
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import com.archstarter.feature.onboarding.ui.OnboardingScreen
import com.archstarter.feature.settings.api.Settings
import com.archstarter.feature.settings.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext
        setContent {
            AppTheme {
                val nav = rememberNavController()
                val app = remember(nav) { App(NavigationActions(nav)) }
                val appComponent = remember(app) { AppComponent::class.create(context, app) }
                val presenterResolver = remember(appComponent) { appComponent.presenterResolver }
                val screenFactory = remember(appComponent) {
                    { ScreenComponent::class.create(appComponent) as ScreenComponentNode }
                }
                val subscreenFactory = remember(appComponent) {
                    { parent: ScreenComponentNode ->
                        val screenParent = parent as? ScreenComponent
                            ?: error("Cannot create subscreen from $parent")
                        SubscreenComponent::class.create(screenParent) as SubscreenComponentNode
                    }
                }
                val onboardingStatus = appComponent.onboardingStatusProvider
                CompositionLocalProvider(
                    LocalPresenterResolver provides presenterResolver,
                    LocalScreenComponentFactory provides screenFactory,
                    LocalSubscreenComponentFactory provides subscreenFactory,
                ) {
                    AppContent(nav, onboardingStatus)
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    nav: NavHostController,
    onboardingStatus: OnboardingStatusProvider,
) {
    val onboardingCompleted by onboardingStatus.hasCompleted.collectAsStateWithLifecycle(initialValue = null)
    val startDestinationState = remember { mutableStateOf<Any?>(null) }
    if (startDestinationState.value == null && onboardingCompleted != null) {
        startDestinationState.value = if (onboardingCompleted == true) Catalog else Onboarding
    }
    Box(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding()
            .systemBarsPadding()
            .safeContentPadding()
            .fillMaxSize(),
    ) {
        val startDestination = startDestinationState.value
        if (startDestination != null) {
            AppNavHost(
                nav = nav,
                startDestination = startDestination,
                onOnboardingFinished = {
                    nav.navigate(Catalog) {
                        popUpTo(Onboarding) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(
    nav: NavHostController,
    startDestination: Any,
    onOnboardingFinished: () -> Unit,
) {
    NavHost(nav, startDestination = startDestination) {
        composable<Onboarding> {
            ScreenScope {
                OnboardingScreen(onFinished = onOnboardingFinished)
            }
        }
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
                SettingsScreen(onExit = { nav.popBackStack() })
            }
        }
    }
}
