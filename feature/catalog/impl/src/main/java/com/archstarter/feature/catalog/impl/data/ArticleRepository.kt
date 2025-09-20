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
  fun article(id: Int): Flow<ArticleEntity?>
  suspend fun refresh()
  suspend fun translateArticle(id: Int): ArticleEntity?
  suspend fun translate(word: String): String?
}

@Singleton
class ArticleRepository @Inject constructor(
  private val wiki: WikipediaService,
  private val summarizer: SummarizerService,
  private val translator: TranslatorService,
  private val dictionary: DictionaryService,
  private val settings: SettingsStateProvider,
  private val dao: ArticleDao
) : ArticleRepo {
  override val articles: Flow<List<ArticleEntity>> = dao.getArticles()

  override fun article(id: Int): Flow<ArticleEntity?> = dao.observeArticle(id)

  override suspend fun refresh() {
    val summary = runCatching { wiki.randomSummary() }.getOrElse { return }

    val prompt = "Summarize this in 3 bullets:\n\n${summary.extract}"
    val summaryText = runCatching { retry { summarizer.summarize(prompt) } }
      .getOrElse { return }
      .takeIf { it.isNotBlank() } ?: return

    val entity = ArticleEntity(
      id = summary.pageid,
      title = summary.title,
      summaryOriginal = summaryText,
      summaryTranslated = null,
      contentOriginal = summary.extract,
      contentTranslated = null,
      originalWord = null,
      translatedWord = null,
      ipa = null,
      sourceUrl = summary.contentUrls.desktop.page,
      createdAt = System.currentTimeMillis()
    )
    dao.insert(entity)
  }

  override suspend fun translateArticle(id: Int): ArticleEntity? {
    val current = dao.getArticle(id) ?: return null

    if (current.summaryTranslated != null && current.contentTranslated != null) {
      return current
    }

    val summaryText = current.summaryOriginal
    val contentText = current.contentOriginal

    val state = settings.state.value
    val nativeLanguage = state.nativeLanguage
    val learningLanguage = state.learningLanguage
    val nativeCode = languageCodes[nativeLanguage] ?: return current
    val learningCode = languageCodes[learningLanguage] ?: return current

    val summaryLanguageCode = detectLanguageCode(summaryText)
    val contentLanguageCode = detectLanguageCode(contentText)

    val summarySourceCode = summaryLanguageCode ?: contentLanguageCode ?: nativeCode
    val contentSourceCode = contentLanguageCode ?: summaryLanguageCode ?: nativeCode

    val summarySourceLanguage = languageDisplayName(summarySourceCode)
    val contentSourceLanguage = languageDisplayName(contentSourceCode)

    val translatedSummary = if (summarySourceCode == nativeCode) {
      summaryText
    } else {
      translateWithFallback(
        word = summaryText,
        langPair = "$summarySourceCode|$nativeCode",
        sourceLanguage = summarySourceLanguage,
        targetLanguage = nativeLanguage
      ) ?: return current
    }

    val translatedContent = if (contentSourceCode == nativeCode) {
      contentText
    } else {
      translateWithFallback(
        word = contentText,
        langPair = "$contentSourceCode|$nativeCode",
        sourceLanguage = contentSourceLanguage,
        targetLanguage = nativeLanguage
      ) ?: return current
    }

    val words = translatedContent.split("\\W+".toRegex()).filter { it.length > 3 }
    val nativeWord = words.randomOrNull()

    val learningTranslation = if (nativeWord == null || nativeCode == learningCode) {
      nativeWord
    } else {
      translateWithFallback(
        word = nativeWord,
        langPair = "$nativeCode|$learningCode",
        sourceLanguage = nativeLanguage,
        targetLanguage = learningLanguage
      ) ?: nativeWord
    }

    val ipa = if (nativeCode == "en" && nativeWord != null) {
      runCatching {
        dictionary.lookup(nativeWord).firstOrNull()?.phonetics?.firstOrNull()?.text
      }.getOrNull()
    } else {
      null
    }

    val replaced = if (nativeWord != null && learningTranslation != null && learningTranslation != nativeWord) {
      translatedContent.replaceFirst(nativeWord, "$nativeWord ($learningTranslation)")
    } else {
      translatedContent
    }

    val updated = current.copy(
      summaryTranslated = translatedSummary,
      contentTranslated = replaced,
      originalWord = nativeWord,
      translatedWord = learningTranslation,
      ipa = ipa
    )
    dao.insert(updated)
    return updated
  }

  override suspend fun translate(word: String): String? {
    val state = settings.state.value
    val nativeLanguage = state.nativeLanguage
    val learningLanguage = state.learningLanguage
    val nativeCode = languageCodes[nativeLanguage] ?: return null
    val learningCode = languageCodes[learningLanguage] ?: return null
    if (nativeCode == learningCode) return word
    val langPair = "$nativeCode|$learningCode"
    return translateWithFallback(
      word = word,
      langPair = langPair,
      sourceLanguage = nativeLanguage,
      targetLanguage = learningLanguage
    )
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
      ?.takeIf { it.isNotBlank() }

    if (translation != null) return translation

    val prompt = buildString {
      appendLine("Translate the following text from $sourceLanguage to $targetLanguage.")
      appendLine("Respond ONLY with valid JSON using this schema: {\"translatedText\":\"<translation>\"}.")
      append("Text: $word")
    }

    val fallback = runCatching {
      val response = retry { summarizer.summarize(prompt) }
      fallbackJson.decodeFromString<AiTranslation>(response).translatedText
    }.getOrNull()

    return fallback?.takeIf { it.isNotBlank() }
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
      .addMigrations(AppDatabase.MIGRATION_2_3)
      .fallbackToDestructiveMigration(dropAllTables = true)
      .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
      .build()

  @Provides
  fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

  @Provides
  @Singleton
  fun provideArticleRepo(
    wiki: WikipediaService,
    summarizer: SummarizerService,
    translator: TranslatorService,
    dictionary: DictionaryService,
    settings: SettingsRepository,
    dao: ArticleDao
  ): ArticleRepo = ArticleRepository(wiki, summarizer, translator, dictionary, settings, dao)
}
