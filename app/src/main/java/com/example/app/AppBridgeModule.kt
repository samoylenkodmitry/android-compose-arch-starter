package com.example.app

import com.example.core.common.app.App
import com.example.core.common.app.AppScope
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppBridgeModule {
    @Provides
    @AppScope
    fun provideApp(manager: AppScopeManager): App =
        EntryPoints.get(manager.getComponent(), AppEntryPoint::class.java).app()
}
