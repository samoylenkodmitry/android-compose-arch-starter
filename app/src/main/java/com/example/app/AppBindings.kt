package com.example.app

import com.example.core.common.app.App
import com.example.core.common.app.AppScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppBindings {
    @AppScope
    @Provides
    fun provideApp(): App = AppHolder.app ?: error("App not initialized")
}
