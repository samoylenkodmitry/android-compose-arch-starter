package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepository
import com.archstarter.feature.catalog.impl.data.ArticleDao
import com.archstarter.feature.catalog.impl.data.DictionaryEntry
import com.archstarter.feature.catalog.impl.data.DictionaryService
import com.archstarter.feature.catalog.impl.data.SummarizerService
import com.archstarter.feature.catalog.impl.data.TranslationDao
import com.archstarter.feature.catalog.impl.data.TranslationData
import com.archstarter.feature.catalog.impl.data.TranslationEntity
import com.archstarter.feature.catalog.impl.data.TranslationResponse
import com.archstarter.feature.catalog.impl.data.TranslatorService
import com.archstarter.feature.catalog.impl.data.WikipediaService
import com.archstarter.feature.catalog.impl.data.WikipediaSummary
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    extract = "Bonjour bonjour bonjour",
    contentUrls = WikipediaSummary.ContentUrls(
      WikipediaSummary.ContentUrls.Desktop("url")
    )
  )

  private class FakeArticleDao : ArticleDao {
    private val data = MutableStateFlow<List<ArticleEntity>>(emptyList())
    override fun getArticles(): Flow<List<ArticleEntity>> = data
    override fun observeArticle(id: Int): Flow<ArticleEntity?> = data.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getArticle(id: Int): ArticleEntity? = data.value.firstOrNull { it.id == id }
    override suspend fun insert(article: ArticleEntity) { data.value = data.value + article }
    val inserted: List<ArticleEntity> get() = data.value
  }

  private class FakeTranslationDao : TranslationDao {
    private val data = mutableMapOf<Pair<String, String>, TranslationEntity>()
    val lookups = mutableListOf<Pair<String, String>>()

    override suspend fun translation(langPair: String, normalized: String): TranslationEntity? {
      lookups += langPair to normalized
      return data[langPair to normalized]
    }

    override suspend fun insert(entity: TranslationEntity) {
      data[entity.langPair to entity.normalizedText] = entity
    }

    val inserted: List<TranslationEntity> get() = data.values.toList()
  }

  private class FakeSettingsRepository(
    native: String = "English",
    learning: String = "Spanish"
  ) : SettingsStateProvider {
    private val stateFlow = MutableStateFlow(
      SettingsState(nativeLanguage = native, learningLanguage = learning)
    )
    override val state: StateFlow<SettingsState> = stateFlow

    suspend fun updateNative(language: String) {
      stateFlow.value = stateFlow.value.copy(nativeLanguage = language)
    }

    suspend fun updateLearning(language: String) {
      stateFlow.value = stateFlow.value.copy(learningLanguage = language)
    }
  }

  @Test
  fun translateSummaryUsesStoredLanguageCode() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
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
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = translations,
    )

    val article = ArticleEntity(
      id = 1,
      title = "Title",
      summary = "Bonjour",
      summaryLanguage = "fr",
      content = "Content",
      sourceUrl = "url",
      originalWord = "Orig",
      translatedWord = "Trans",
      ipa = null,
      createdAt = 0L,
    )

    val translation = repo.translateSummary(article)

    assertEquals("fr|en", usedLangPair)
    assertEquals("hola", translation)
  }

  @Test
  fun translateUsesNativeToLearningLangPair() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
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
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = translations,
    )

    val result = repo.translate("word")

    assertEquals("en|es", usedLangPair)
    assertEquals("hola", result)
  }

  @Test
  fun translateUsesUpdatedNativeLanguageForSource() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
    var usedLangPair: String? = null
    val settings = FakeSettingsRepository()
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
      dao = dao,
      translationDao = translations,
    )

    repo.translate("word")

    assertEquals("sr|de", usedLangPair)
  }

  @Test
  fun translateFallsBackToAiOnErrorStatus() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
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
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = translations,
    )

    val translation = repo.translate("word")

    assertEquals("hola", translation)
  }

  @Test
  fun translateReturnsCachedTranslation() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
    translations.insert(
      TranslationEntity(
        langPair = "en|es",
        normalizedText = "word",
        translation = "hola",
        updatedAt = 0L,
      )
    )
    var invoked = false
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          invoked = true
          return TranslationResponse(TranslationData("api"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = translations,
    )

    val result = repo.translate("Word")

    assertEquals("hola", result)
    assertTrue(translations.lookups.contains("en|es" to "word"))
    assertTrue(!invoked)
  }

  @Test
  fun translateSummaryReturnsCachedTranslation() = runTest {
    val dao = FakeArticleDao()
    val translations = FakeTranslationDao()
    translations.insert(
      TranslationEntity(
        langPair = "fr|en",
        normalizedText = "bonjour",
        translation = "hello",
        updatedAt = 0L,
      )
    )
    var invoked = false
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary() },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String): TranslationResponse {
          invoked = true
          return TranslationResponse(TranslationData("ignored"))
        }
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = translations,
    )

    val article = ArticleEntity(
      id = 1,
      title = "Title",
      summary = "Bonjour",
      summaryLanguage = "fr",
      content = "Content",
      sourceUrl = "url",
      originalWord = "Orig",
      translatedWord = "Trans",
      ipa = null,
      createdAt = 0L,
    )

    val translation = repo.translateSummary(article)

    assertEquals("hello", translation)
    assertTrue(!invoked)
  }

  @Test
  fun refreshFetchesAndSavesArticle() = runTest {
    val dao = FakeArticleDao()
    val summary = summary()
    val repo = ArticleRepository(
      wiki = object : WikipediaService { override suspend fun randomSummary() = summary },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String) = TranslationResponse(TranslationData(""))
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = FakeTranslationDao(),
    )

    repo.refresh()

    assertEquals(1, dao.inserted.size)
    val entity = dao.inserted.first()
    assertEquals(summary.pageid, entity.id)
    assertEquals(summary.title, entity.title)
    assertEquals(summary.extract, entity.content)
    assertEquals(summary.contentUrls.desktop.page, entity.sourceUrl)
    assertEquals("", entity.summary)
    assertEquals(null, entity.summaryLanguage)
    assertEquals("", entity.originalWord)
    assertEquals("", entity.translatedWord)
    assertEquals(null, entity.ipa)
  }

  @Test
  fun refreshSkipsOnNetworkError() = runTest {
    val dao = FakeArticleDao()
    val repo = ArticleRepository(
      wiki = object : WikipediaService {
          override suspend fun randomSummary(): WikipediaSummary {
              throw IOException("network")
          }
      },
      summarizer = object : SummarizerService { override suspend fun summarize(prompt: String) = "ok" },
      translator = object : TranslatorService {
        override suspend fun translate(word: String, langPair: String) = TranslationResponse(TranslationData(""))
      },
      dictionary = object : DictionaryService { override suspend fun lookup(word: String) = emptyList<DictionaryEntry>() },
      settings = FakeSettingsRepository(),
      dao = dao,
      translationDao = FakeTranslationDao(),
    )

    repo.refresh()

    assertTrue(dao.inserted.isEmpty())
  }
}

