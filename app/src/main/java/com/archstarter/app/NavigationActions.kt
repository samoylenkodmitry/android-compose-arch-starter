package com.archstarter.app

import androidx.navigation.NavHostController
import com.archstarter.core.common.app.NavigationActions as NavigationActionsApi
import com.archstarter.feature.detail.api.Detail
import com.archstarter.feature.settings.api.Settings

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
