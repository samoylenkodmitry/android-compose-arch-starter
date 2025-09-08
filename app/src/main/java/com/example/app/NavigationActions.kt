package com.example.app

import androidx.navigation.NavHostController
import com.example.core.common.app.NavigationActions as NavigationActionsApi
import com.example.feature.detail.api.Detail
import com.example.feature.settings.api.Settings

class NavigationActions(
    private val navController: NavHostController
) : NavigationActionsApi {
    override fun openDetail(id: Int) {
        navController.navigate(Detail(id))
    }
    override fun openSettings() {
        navController.navigate(Settings)
    }
}
