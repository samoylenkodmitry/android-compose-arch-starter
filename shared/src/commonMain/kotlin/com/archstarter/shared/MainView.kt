package com.archstarter.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.archstarter.core.common.navigation.Navigation
import com.archstarter.core.common.scope.AppComponent
import com.archstarter.core.common.scope.LocalAppComponent
import com.archstarter.core.common.scope.create
import com.archstarter.core.designsystem.AppTheme

@Composable
fun MainView() {
    AppTheme {
        Navigator(CatalogScreenDestination) { navigator ->
            val navigation = remember<Navigation> {
                object : Navigation {
                    override fun openSettings() {
                        navigator.push(SettingsScreenDestination)
                    }

                    override fun openDetail(id: Int) {
                        navigator.push(DetailScreenDestination(id))
                    }
                }
            }

            val appComponent = remember {
                AppComponent::class.create(navigation)
            }

            CompositionLocalProvider(LocalAppComponent provides appComponent) {
                CurrentScreen()
            }
        }
    }
}