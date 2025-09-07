package com.example.core.common.app

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class App @Inject constructor() {
    private var navigation: NavigationActions? = null

    fun bindNavigation(actions: NavigationActions) {
        navigation = actions
    }

    fun openDetail(id: String) {
        navigation?.openDetail(id)
    }
}
