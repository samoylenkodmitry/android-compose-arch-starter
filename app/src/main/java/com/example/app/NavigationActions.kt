package com.example.app

import androidx.navigation.NavHostController
import com.example.core.common.app.NavigationActions as NavigationActionsApi
import com.example.feature.detail.api.Detail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationActions @Inject constructor() : NavigationActionsApi {
    private lateinit var navController: NavHostController

    fun bind(navController: NavHostController) {
        this.navController = navController
    }

    override fun openDetail(id: String) {
        navController.navigate(Detail(id))
    }
}
