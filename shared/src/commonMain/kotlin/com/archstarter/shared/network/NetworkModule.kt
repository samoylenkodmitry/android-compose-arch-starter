package com.archstarter.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides

expect class NetworkModule {
    @Provides
    fun provideHttpClientEngine(): HttpClientEngine
}

fun createHttpClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}