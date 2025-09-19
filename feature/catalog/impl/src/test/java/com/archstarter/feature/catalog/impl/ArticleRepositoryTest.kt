package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepository
import com.archstarter.feature.catalog.impl.data.ArticleDao
import com.archstarter.feature.catalog.impl.data.DictionaryEntry
import com.archstarter.feature.catalog.impl.data.DictionaryService
import com.archstarter.feature.catalog.impl.data.SummarizerService
import com.archstarter.feature.catalog.impl.data.TranslationData
import com.archstarter.feature.catalog.impl.data.TranslationResponse
import com.archstarter.feature.catalog.impl.data.TranslatorService
import com.archstarter.feature.catalog.impl.data.WikipediaService
import com.archstarter.feature.catalog.impl.data.WikipediaSummary
import com.archstarter.feature.settings.impl.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryTest {

  @get:Rule
  val dispatcherRule = MainDispatcherRule()

  private fun summary() = WikipediaSummary(
    pageid = 1,
    title = "Title",
    extract = "Some example extract with random words",
    contentUrls = WikipediaSummary.ContentUrls(
      WikipediaSummary.ContentUrls.Desktop("url")
    )
  )

  private class FakeArticleDao : ArticleDao {
    private val data = MutableStateFlow<List<ArticleEntity>>(emptyList())
    override fun getArticles(): Flow<List<ArticleEntity>> = data
    override suspend fun getArticle(id: Int): ArticleEntity? = data.value.firstOrNull { it.id == id }
    override suspend fun insert(article: ArticleEntity) { data.value = data.value + article }
    val inserted: List<ArticleEntity> get() = data.value
  }

  @Test
  fun translateUsesEnglishToNativeLangPair() = runTest {
    val dao = FakeArticleDao()
    var usedLangPair: String? = null
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          usedLangPair = langPair
          return TranslationResponse(TranslationData("hola"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    val result = repo.translate("word")

    assertEquals("en|en", usedLangPair)
    assertEquals("hola", result)
  }

  @Test
  fun translateUsesNativeLanguageForTarget() = runTest {
    val dao = FakeArticleDao()
    var usedLangPair: String? = null
    val settings = SettingsRepository()
    settings.updateNative("Serbian")
    settings.updateLearning("German")
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          usedLangPair = langPair
          return TranslationResponse(TranslationData("guten tag"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = settings,
      dao = dao
    )

    repo.translate("word")

    assertEquals("en|sr", usedLangPair)
  }

  @Test
  fun translateFallsBackToAiOnErrorStatus() = runTest {
    val dao = FakeArticleDao()
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService {
        override suspend fun summarize(prompt: String) = "{\"translatedText\":\"hola\"}"
      },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse =
          TranslationResponse(TranslationData("api"), responseStatus = 403)
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    val translation = repo.translate("word")

    assertEquals("hola", translation)
  }

  @Test
  fun refreshUsesEnglishToNativeLangPair() = runTest {
    val dao = FakeArticleDao()
    var usedLangPair: String? = null
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "ok" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          usedLangPair = langPair
          return TranslationResponse(TranslationData("hola"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    repo.refresh()

    assertEquals("en|en", usedLangPair)
  }

  @Test
  fun refreshUsesNativeLanguageForTarget() = runTest {
    val dao = FakeArticleDao()
    var usedLangPair: String? = null
    val settings = SettingsRepository()
    settings.updateNative("French")
    settings.updateLearning("German")
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "ok" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          usedLangPair = langPair
          return TranslationResponse(TranslationData("guten tag"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = settings,
      dao = dao
    )

    repo.refresh()

    assertEquals("en|fr", usedLangPair)
  }

  @Test
  fun refreshSkipsOnNetworkError() = runTest {
    val dao = FakeArticleDao()
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "ok" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          throw IOException("network")
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    repo.refresh()

    assertTrue(dao.inserted.isEmpty())
  }

  @Test
  fun refreshSkipsOnEmptyTranslation() = runTest {
    val dao = FakeArticleDao()
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "ok" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String) = TranslationResponse(TranslationData(""))
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    repo.refresh()

    assertTrue(dao.inserted.isEmpty())
  }

  @Test
  fun refreshFallsBackToAiOnErrorStatus() = runTest {
    val dao = FakeArticleDao()
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService {
        override suspend fun summarize(prompt: String): String =
          if (prompt.startsWith("Summarize this")) "ok" else "{\"translatedText\":\"hola\"}"
      },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse =
          TranslationResponse(TranslationData("api"), responseStatus = 403)
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = SettingsRepository(),
      dao = dao
    )

    repo.refresh()

    assertEquals(1, dao.inserted.size)
    assertEquals("hola", dao.inserted.first().translatedWord)
  }
}

