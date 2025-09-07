package com.example.app

import com.example.core.common.app.App
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(AppComponent::class)
interface AppEntryPoint {
    fun app(): App
}
