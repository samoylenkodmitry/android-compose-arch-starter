package com.example.app

import com.example.core.common.app.App
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import com.example.app.InternalApp

@EntryPoint
@InstallIn(AppComponent::class)
interface AppEntryPoint {
    @InternalApp fun app(): App
}
