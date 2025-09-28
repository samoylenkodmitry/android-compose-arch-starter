package com.archstarter.feature.catalog.impl.data

import android.content.Context
import androidx.room.Room
import com.archstarter.core.common.network.Urls
import com.archstarter.feature.settings.api.SettingsStateProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

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
  private val dao: ArticleDao,
  private val settings: SettingsStateProvider,
  private val translator: TranslatorService,
) : ArticleRepo {
  override val articles: Flow<List<ArticleEntity>> = dao.getArticles()

  override suspend fun refresh() {
    val summary = runCatching { wiki.randomSummary() }.getOrElse { return }
    val trimmedSummary = summary.extract.trim().ifBlank { summary.title }

    val entity = ArticleEntity(
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
    dao.insert(entity)
  }

  override suspend fun article(id: Int): ArticleEntity? = dao.getArticle(id)

  override fun articleFlow(id: Int): Flow<ArticleEntity?> = dao.observeArticle(id)

  override suspend fun translateSummary(article: ArticleEntity): String? {
    val settingsState = settings.state.first()
    val from = article.summaryLanguage ?: settingsState.nativeLanguageCode
    val to = settingsState.learningLanguageCode
    return translate(article.summary, from, to)
  }

  override suspend fun translate(word: String): String? {
    val settingsState = settings.state.first()
    val from = settingsState.nativeLanguageCode
    val to = settingsState.learningLanguageCode
    return translate(word, from, to)
  }

  private suspend fun translate(
    text: String,
    from: String,
    to: String
  ): String? {
    if (from == to) return text
    val langPair = "$from|$to"
    return runCatching {
      translator.translate(text, langPair).responseData.translatedText
    }.getOrNull()
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
      .baseUrl(Urls.WIKIPEDIA_API_URL)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(WikipediaService::class.java)

  @Provides
  @Singleton
  fun provideTranslationService(client: OkHttpClient): TranslatorService =
    Retrofit.Builder()
      .baseUrl(Urls.MYMEMORY_API_URL)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(TranslatorService::class.java)

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
  @Singleton
  fun provideArticleRepo(
    wiki: WikipediaService,
    dao: ArticleDao,
    settings: SettingsStateProvider,
    translator: TranslatorService,
  ): ArticleRepo =
    ArticleRepository(wiki, dao, settings, translator)
}