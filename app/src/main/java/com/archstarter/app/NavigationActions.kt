package com.archstarter.app

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
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

    override fun openLink(url: String) {
        if (url.isBlank()) return
        val context = navController.context
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ContextCompat.startActivity(context, intent, null)
    }
}
