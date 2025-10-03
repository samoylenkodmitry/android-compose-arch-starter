package com.archstarter.feature.catalog.impl.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject

@Serializable
data class WikipediaSummary(
    val pageid: Int,
    val title: String,
    val extract: String,
    @SerialName("content_urls") val contentUrls: ContentUrls
) {
    @Serializable
    data class ContentUrls(@SerialName("desktop") val desktop: Desktop) {
        @Serializable
        data class Desktop(val page: String)
    }
}

@Inject
class WikipediaService(private val httpClient: HttpClient) {
    suspend fun randomSummary(): WikipediaSummary {
        return httpClient.get("https://en.wikipedia.org/api/rest_v1/page/random/summary").body()
    }
}

@Serializable
data class TranslationResponse(
    @SerialName("responseData") val responseData: TranslationData,
    @SerialName("responseStatus") val responseStatus: Int = 200,
    @SerialName("responseDetails") val responseDetails: String? = null
)

@Serializable
data class TranslationData(@SerialName("translatedText") val translatedText: String)

@Inject
class TranslatorService(private val httpClient: HttpClient) {
    suspend fun translate(word: String, langPair: String): TranslationResponse {
        return httpClient.get("https://api.mymemory.translated.net/get") {
            parameter("q", word)
            parameter("langpair", langPair)
        }.body()
    }
}