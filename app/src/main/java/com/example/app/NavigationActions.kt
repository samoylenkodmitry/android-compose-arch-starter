package com.example.app

import androidx.navigation.NavHostController
import javax.inject.Inject
import javax.inject.Singleton
import com.example.core.common.app.NavigationActions as NavigationActionsApi
import com.example.feature.detail.api.Detail

@Singleton
class NavigationActions @Inject constructor() : NavigationActionsApi {
    private lateinit var navController: NavHostController

    fun setNavController(controller: NavHostController) {
        navController = controller
    }

    override fun openDetail(id: String) {
        navController.navigate(Detail(id))
    }
}
