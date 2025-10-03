package com.archstarter.feature.catalog.impl.data

import com.archstarter.shared.cache.AppDatabase
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import java.util.Locale

interface ArticleRepo {
    val articles: Flow<List<com.archstarter.shared.cache.ArticleEntity>>
    suspend fun refresh()
    suspend fun article(id: Int): com.archstarter.shared.cache.ArticleEntity?
    fun articleFlow(id: Int): Flow<com.archstarter.shared.cache.ArticleEntity?>
    suspend fun translateSummary(
        article: com.archstarter.shared.cache.ArticleEntity,
        fromLanguage: String,
        toLanguage: String,
    ): String?

    suspend fun translateContent(
        content: String,
        fromLanguage: String,
        toLanguage: String,
    ): String?

    suspend fun translate(
        word: String,
        fromLanguage: String,
        toLanguage: String,
    ): String?
}

@Inject
class ArticleRepository(
    private val wiki: WikipediaService,
    private val db: AppDatabase,
    private val translator: TranslatorService,
) : ArticleRepo {
    private val articleQueries = db.appDatabaseQueries

    override val articles: Flow<List<com.archstarter.shared.cache.ArticleEntity>> =
        articleQueries.getArticles().asFlow().mapToList()

    override suspend fun refresh() {
        val summary = runCatching { wiki.randomSummary() }.getOrElse { return }
        val trimmedSummary = summary.extract.trim().ifBlank { summary.title }

        val entity = com.archstarter.shared.cache.ArticleEntity(
            id = summary.pageid,
            title = summary.title,
            summary = trimmedSummary,
            summaryLanguage = null,
            content = trimmedSummary,
            sourceUrl = summary.contentUrls.desktop.page,
            originalWord = "",
            translatedWord = "",
            ipa = null,
            createdAt = System.currentTimeMillis(),
        )
        articleQueries.insertArticle(entity)
    }

    override suspend fun article(id: Int): com.archstarter.shared.cache.ArticleEntity? =
        articleQueries.getArticle(id).executeAsOneOrNull()

    override fun articleFlow(id: Int): Flow<com.archstarter.shared.cache.ArticleEntity?> =
        articleQueries.observeArticle(id).asFlow().mapToOneOrNull()

    override suspend fun translateSummary(
        article: com.archstarter.shared.cache.ArticleEntity,
        fromLanguage: String,
        toLanguage: String,
    ): String? = translateText(article.summary, fromLanguage, toLanguage)

    override suspend fun translateContent(
        content: String,
        fromLanguage: String,
        toLanguage: String,
    ): String? = translateText(content, fromLanguage, toLanguage)

    override suspend fun translate(
        word: String,
        fromLanguage: String,
        toLanguage: String,
    ): String? = translateText(word, fromLanguage, toLanguage)

    private suspend fun translateText(
        text: String,
        fromLanguage: String,
        toLanguage: String,
    ): String? {
        val normalized = text.trim()
        if (normalized.isEmpty()) return null
        val langPair = buildLangPair(fromLanguage, toLanguage) ?: return normalized
        val cacheKey = normalized.lowercase(Locale.ROOT)
        articleQueries.getTranslation(langPair, cacheKey).executeAsOneOrNull()?.let { return it.translation }

        val response = runCatching { translator.translate(normalized, langPair) }.getOrNull()
        val translation = response
            ?.takeIf { it.responseStatus == 200 }
            ?.responseData
            ?.translatedText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return normalized

        articleQueries.insertTranslation(
            langPair = langPair,
            normalizedText = cacheKey,
            translation = translation,
            updatedAt = System.currentTimeMillis(),
        )
        return translation
    }

    private fun buildLangPair(fromLanguage: String, toLanguage: String): String? {
        val from = fromLanguage.trim().lowercase(Locale.ROOT)
        val to = toLanguage.trim().lowercase(Locale.ROOT)
        if (from.isEmpty() || to.isEmpty()) return null
        if (from == to) return null
        return "$from|$to"
    }
}