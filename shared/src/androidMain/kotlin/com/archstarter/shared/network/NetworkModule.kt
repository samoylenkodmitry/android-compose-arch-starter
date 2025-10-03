package com.archstarter.shared.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import me.tatarka.inject.annotations.Provides

actual class NetworkModule {
    @Provides
    fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create()
}