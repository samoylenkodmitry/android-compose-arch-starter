package com.example.app

import com.example.core.common.app.NavigationActions as NavigationActionsApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds abstract fun bindNavigationActions(actions: NavigationActions): NavigationActionsApi
}
