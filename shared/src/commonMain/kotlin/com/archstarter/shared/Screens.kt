package com.archstarter.shared

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.feature.catalog.ui.CatalogScreen

object CatalogScreenDestination : Screen {
    @Composable
    override fun Content() {
        ScreenScope {
            CatalogScreen()
        }
    }
}

data class DetailScreenDestination(val id: Int) : Screen {
    @Composable
    override fun Content() {
        ScreenScope {
            Text("Detail Screen for $id")
        }
    }
}

object SettingsScreenDestination : Screen {
    @Composable
    override fun Content() {
        ScreenScope {
            Text("Settings Screen")
        }
    }
}