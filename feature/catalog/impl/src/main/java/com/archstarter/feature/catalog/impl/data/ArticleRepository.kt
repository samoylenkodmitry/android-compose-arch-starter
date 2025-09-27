package com.archstarter.feature.catalog.impl.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject
import com.archstarter.feature.settings.api.SettingsStateProvider
import com.archstarter.feature.settings.api.languageCodes
import com.archstarter.feature.settings.impl.data.SettingsRepository
import java.util.Locale

private const val HTTP_OK = 200

@Serializable
private data class AiTranslation(val translatedText: String)

@Serializable
private data class AiLanguageDetection(val languageCode: String)

private val fallbackJson = Json { ignoreUnknownKeys = true }

interface ArticleRepo {
  val articles: Flow<List<ArticleEntity>>
  suspend fun refresh()
  suspend fun article(id: Int): ArticleEntity?
  fun articleFlow(id: Int): Flow<ArticleEntity?>
  suspend fun translateSummary(article: ArticleEntity): String?
  suspend fun translate(word: String): String?
}

@Singleton
class ArticleRepository @Inject constructor(
  private val wiki: WikipediaService,
  private val summarizer: SummarizerService,
  private val translator: TranslatorService,
  private val dictionary: DictionaryService,
  private val settings: SettingsStateProvider,
  private val dao: ArticleDao,
  private val translationDao: TranslationDao,
) : ArticleRepo {
  override val articles: Flow<List<ArticleEntity>> = dao.getArticles()

  override suspend fun refresh() {
    val summary = runCatching { wiki.randomSummary() }.getOrElse { return }
    val entity = ArticleEntity(
      id = summary.pageid,
      title = summary.title,
      summary = "",
      summaryLanguage = null,
      content = summary.extract,
      sourceUrl = summary.contentUrls.desktop.page,
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = System.currentTimeMillis()
    )
    dao.insert(entity)
  }

  override suspend fun article(id: Int): ArticleEntity? = dao.getArticle(id)

  override fun articleFlow(id: Int): Flow<ArticleEntity?> = dao.observeArticle(id)

  override suspend fun translateSummary(article: ArticleEntity): String? {
    val text = article.summary
    if (text.isBlank()) return text

    val state = settings.state.value
    val nativeLanguage = state.nativeLanguage
    val nativeCode = languageCodes[nativeLanguage] ?: return null
    val sourceCode = article.summaryLanguage?.takeIf { it.isNotBlank() }
      ?: detectLanguageCode(text)
      ?: nativeCode
    if (sourceCode == nativeCode) return text
    val sourceLanguage = languageDisplayName(sourceCode)
    val langPair = "$sourceCode|$nativeCode"
    cachedTranslation(langPair, text)?.let { return it }
    val translation = translateWithFallback(
      word = text,
      langPair = langPair,
      sourceLanguage = sourceLanguage,
      targetLanguage = nativeLanguage
    )
    if (translation != null) {
      storeTranslation(langPair, text, translation)
    }
    return translation
  }

  override suspend fun translate(word: String): String? {
    val state = settings.state.value
    val nativeLanguage = state.nativeLanguage
    val learningLanguage = state.learningLanguage
    val nativeCode = languageCodes[nativeLanguage] ?: return null
    val learningCode = languageCodes[learningLanguage] ?: return null
    if (nativeCode == learningCode) return word
    val langPair = "$nativeCode|$learningCode"
    cachedTranslation(langPair, word)?.let { return it }
    val translation = translateWithFallback(
      word = word,
      langPair = langPair,
      sourceLanguage = nativeLanguage,
      targetLanguage = learningLanguage
    )
    if (translation != null) {
      storeTranslation(langPair, word, translation)
    }
    return translation
  }

  private suspend fun translateWithFallback(
    word: String,
    langPair: String,
    sourceLanguage: String,
    targetLanguage: String
  ): String? {
    val translation = runCatching { translator.translate(word, langPair) }
      .getOrNull()
      ?.takeIf { it.responseStatus == HTTP_OK }
      ?.responseData
      ?.translatedText
      ?.trim()
      ?.takeIf { it.isNotEmpty() }

    if (translation != null) return translation

    val prompt = buildString {
      appendLine("Translate the following text from $sourceLanguage to $targetLanguage.")
      appendLine("Respond ONLY with valid JSON using this schema: {\"translatedText\":\"<translation>\"}.")
      append("Text: $word")
    }

    val fallback = runCatching {
      val response = retry { summarizer.summarize(prompt) }
      fallbackJson.decodeFromString<AiTranslation>(response).translatedText.trim()
    }.getOrNull()

    return fallback?.takeIf { it.isNotEmpty() }
  }

  private suspend fun cachedTranslation(langPair: String, input: String): String? {
    val key = normalizedInput(input) ?: return null
    return translationDao.translation(langPair, key)?.translation
  }

  private suspend fun storeTranslation(langPair: String, input: String, translation: String) {
    val key = normalizedInput(input) ?: return
    val normalizedTranslation = translation.trim()
    if (normalizedTranslation.isEmpty()) return
    translationDao.insert(
      TranslationEntity(
        langPair = langPair,
        normalizedText = key,
        translation = normalizedTranslation,
        updatedAt = System.currentTimeMillis()
      )
    )
  }

  private fun normalizedInput(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.lowercase(Locale.ROOT)
  }

  private suspend fun detectLanguageCode(text: String): String? {
    val sample = text.trim().take(1000)
    if (sample.isBlank()) return null
    val prompt = buildString {
      appendLine("Identify the ISO 639-1 language code (two letters) for the following text.")
      appendLine("Respond ONLY with valid JSON using this schema: {\"languageCode\":\"<code>\"}.")
      appendLine("Text:")
      append(sample)
    }

    return runCatching {
      val response = retry { summarizer.summarize(prompt) }
      fallbackJson.decodeFromString<AiLanguageDetection>(response).languageCode
        .lowercase(Locale.ENGLISH)
    }.getOrNull()?.takeIf { it.length == 2 }
  }

  private fun languageDisplayName(code: String): String {
    val mapped = languageCodes.entries.firstOrNull { it.value.equals(code, ignoreCase = true) }?.key
    if (mapped != null) return mapped
    val localeName = Locale(code).getDisplayLanguage(Locale.ENGLISH)
    return if (localeName.isBlank()) code else localeName
  }

  private suspend fun <T> retry(times: Int = 3, block: suspend () -> T): T {
    repeat(times - 1) { attempt ->
      try { return block() } catch (e: Exception) { delay(500L * (attempt + 1)) }
    }
    return block()
  }
}

@Module
@InstallIn(SingletonComponent::class)
object ArticleDataModule {
  private val json = Json { ignoreUnknownKeys = true }

  @Provides
  @Singleton
  fun provideOkHttp(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
      .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", "android-compose-arch-starter").build())
      }
      .build()

  @Provides
  @Singleton
  fun provideWikipediaService(client: OkHttpClient): WikipediaService =
    Retrofit.Builder()
      .baseUrl("https://en.wikipedia.org/api/rest_v1/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(WikipediaService::class.java)

  @Provides
  @Singleton
  fun provideSummarizerService(client: OkHttpClient): SummarizerService =
    Retrofit.Builder()
      .baseUrl("https://text.pollinations.ai/")
      .client(client)
      .addConverterFactory(ScalarsConverterFactory.create())
      .build()
      .create(SummarizerService::class.java)

  @Provides
  @Singleton
  fun provideTranslatorService(client: OkHttpClient): TranslatorService =
    Retrofit.Builder()
      .baseUrl("https://api.mymemory.translated.net/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(TranslatorService::class.java)

  @Provides
  @Singleton
  fun provideDictionaryService(client: OkHttpClient): DictionaryService =
    Retrofit.Builder()
      .baseUrl("https://api.dictionaryapi.dev/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(DictionaryService::class.java)

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "articles.db")
      .fallbackToDestructiveMigration(dropAllTables = true)
      .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
      .build()

  @Provides
  fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

  @Provides
  fun provideTranslationDao(db: AppDatabase): TranslationDao = db.translationDao()

  @Provides
  @Singleton
  fun provideArticleRepo(
    wiki: WikipediaService,
    summarizer: SummarizerService,
    translator: TranslatorService,
    dictionary: DictionaryService,
    settings: SettingsRepository,
    dao: ArticleDao,
    translationDao: TranslationDao,
  ): ArticleRepo =
    ArticleRepository(wiki, summarizer, translator, dictionary, settings, dao, translationDao)
}
