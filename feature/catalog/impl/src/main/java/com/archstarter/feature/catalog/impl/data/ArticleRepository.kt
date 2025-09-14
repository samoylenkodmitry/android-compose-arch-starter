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
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject
import com.archstarter.feature.settings.api.languageCodes
import com.archstarter.feature.settings.impl.data.SettingsRepository

interface ArticleRepo {
  val articles: Flow<List<ArticleEntity>>
  suspend fun refresh()
  suspend fun article(id: Int): ArticleEntity?
}

@Singleton
class ArticleRepository @Inject constructor(
  private val wiki: WikipediaService,
  private val summarizer: SummarizerService,
  private val translator: TranslatorService,
  private val dictionary: DictionaryService,
  private val settings: SettingsRepository,
  private val dao: ArticleDao
) : ArticleRepo {
  override val articles: Flow<List<ArticleEntity>> = dao.getArticles()

  override suspend fun refresh() {
    val summary = runCatching { wiki.randomSummary() }.getOrElse { return }

    val prompt = "Summarize this in 3 bullets:\n\n${summary.extract}"
    val summaryText = runCatching { retry { summarizer.summarize(prompt) } }
      .getOrElse { return }
      .takeIf { it.isNotBlank() } ?: return

    val words = summary.extract.split("\\W+".toRegex()).filter { it.length > 3 }
    val original = words.randomOrNull() ?: return

    val state = settings.state.value
    val langPair = "${languageCodes[state.learningLanguage]}|${languageCodes[state.nativeLanguage]}"
    val translation = runCatching {
      translator.translate(original, langPair).responseData.translatedText
    }.getOrElse { return }.takeIf { it.isNotBlank() } ?: return

    val ipa = runCatching {
      dictionary.lookup(original).firstOrNull()?.phonetics?.firstOrNull()?.text
    }.getOrNull()

    val replaced = summary.extract.replaceFirst(original, "$translation ($original)")
    val entity = ArticleEntity(
      id = summary.pageid,
      title = summary.title,
      summary = summaryText,
      content = replaced,
      sourceUrl = summary.contentUrls.desktop.page,
      originalWord = original,
      translatedWord = translation,
      ipa = ipa,
      createdAt = System.currentTimeMillis()
    )
    dao.insert(entity)
  }

  override suspend fun article(id: Int): ArticleEntity? = dao.getArticle(id)

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
    Room.databaseBuilder(context, AppDatabase::class.java, "articles.db").build()

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
