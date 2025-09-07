package com.example.app

import androidx.navigation.NavHostController
import com.example.core.common.app.NavigationActions as NavigationActionsApi
import com.example.feature.detail.api.Detail

class NavigationActions(
    private val navController: NavHostController
) : NavigationActionsApi {
    override fun openDetail(id: Int) {
        navController.navigate(Detail(id))
    }
}
