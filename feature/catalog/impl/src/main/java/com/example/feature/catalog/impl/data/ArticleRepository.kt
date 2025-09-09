package com.example.feature.catalog.impl.data

import androidx.room.Room
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.feature.catalog.impl.CatalogViewModel

interface ArticleRepo {
  val articles: Flow<List<ArticleEntity>>
  suspend fun refresh()
  suspend fun article(id: Int): ArticleEntity?
}

class ArticleRepository(
  private val wiki: WikipediaService,
  private val summarizer: SummarizerService,
  private val translator: TranslatorService,
  private val dictionary: DictionaryService,
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

    val translation = runCatching {
      translator.translate(original, "en|sr").responseData.translatedText
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
      ipa = ipa
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

private val json = Json { ignoreUnknownKeys = true }

val catalogModule = module {
  single {
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
      .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", "android-compose-arch-starter").build())
      }
      .build()
  }
  single {
    Retrofit.Builder()
      .baseUrl("https://en.wikipedia.org/api/rest_v1/")
      .client(get())
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(WikipediaService::class.java)
  }
  single {
    Retrofit.Builder()
      .baseUrl("https://text.pollinations.ai/")
      .client(get())
      .addConverterFactory(ScalarsConverterFactory.create())
      .build()
      .create(SummarizerService::class.java)
  }
  single {
    Retrofit.Builder()
      .baseUrl("https://api.mymemory.translated.net/")
      .client(get())
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(TranslatorService::class.java)
  }
  single {
    Retrofit.Builder()
      .baseUrl("https://api.dictionaryapi.dev/")
      .client(get())
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(DictionaryService::class.java)
  }
  single {
    Room.databaseBuilder(androidContext(), AppDatabase::class.java, "articles.db").build()
  }
  single { get<AppDatabase>().articleDao() }
  single<ArticleRepo> { ArticleRepository(get(), get(), get(), get(), get()) }
  viewModel { CatalogViewModel(get(), get()) }
}
