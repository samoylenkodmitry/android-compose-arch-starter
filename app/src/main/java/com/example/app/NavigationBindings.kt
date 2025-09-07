package com.example.app

import com.example.core.common.app.NavigationActions as NavigationActionsApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface NavigationBindings {
    @Binds
    fun bindNavigationActions(actions: NavigationActions): NavigationActionsApi
}

