package com.example.core.common.app

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class App @Inject constructor(
    private val navigation: NavigationActions
) {
    fun openDetail(id: String) {
        navigation.openDetail(id)
    }
}
