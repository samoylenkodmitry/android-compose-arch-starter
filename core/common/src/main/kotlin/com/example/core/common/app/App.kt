package com.example.core.common.app

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class App @Inject constructor() {
    private lateinit var navigation: NavigationActions

    fun bindNavigation(actions: NavigationActions) {
        navigation = actions
    }

    fun openDetail(id: String) {
        navigation.openDetail(id)
    }
}
