package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleDao
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.catalog.impl.data.ArticleRepository
import com.archstarter.feature.catalog.impl.data.TranslationData
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryTest {

  @get:Rule
  val dispatcherRule = MainDispatcherRule()

  private fun summary(
    id: Int = 1,
    title: String = "Title",
    extract: String = " Summary \n ",
    url: String = "url",
  ) = WikipediaSummary(
    pageid = id,
    title = title,
    extract = extract,
    contentUrls = WikipediaSummary.ContentUrls(
      WikipediaSummary.ContentUrls.Desktop(url)
    ),
  )

  private class FakeArticleDao : ArticleDao {
    private val state = MutableStateFlow<List<ArticleEntity>>(emptyList())
    override fun getArticles(): Flow<List<ArticleEntity>> = state
    override fun observeArticle(id: Int): Flow<ArticleEntity?> = state.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getArticle(id: Int): ArticleEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun insert(article: ArticleEntity) {
      state.value = state.value + article
    }

    val inserted: List<ArticleEntity> get() = state.value
  }

  private class FakeTranslatorService(
    private val translatedText: String = "Translated"
  ) : TranslatorService {
    var lastQuery: String? = null
    var lastLangPair: String? = null
    override suspend fun translate(word: String, langPair: String): TranslationResponse {
      lastQuery = word
      lastLangPair = langPair
      return TranslationResponse(TranslationData(translatedText))
    }
  }

  private class FakeSettingsStateProvider(
    initialState: SettingsState = SettingsState()
  ) : SettingsStateProvider {
    override val state: StateFlow<SettingsState> = MutableStateFlow(initialState)
  }

  private val fakeWiki = object : WikipediaService {
    override suspend fun randomSummary(): WikipediaSummary = summary()
  }

  @Test
  fun refreshStoresTrimmedSummaryFromWikipedia() = runTest {
    val dao = FakeArticleDao()
    val repo: ArticleRepo = ArticleRepository(
      wiki = fakeWiki,
      dao = dao,
      settings = FakeSettingsStateProvider(),
      translator = FakeTranslatorService(),
    )

    repo.refresh()

    val stored = dao.inserted.single()
    assertEquals(1, stored.id)
    assertEquals("Title", stored.title)
    assertEquals("Summary", stored.summary)
    assertEquals("Summary", stored.content)
    assertEquals("url", stored.sourceUrl)
  }

  @Test
  fun translateSummaryReturnsStoredSummary() = runTest {
    val dao = FakeArticleDao()
    val article = ArticleEntity(
      id = 2,
      title = "Other",
      summary = "Original",
      summaryLanguage = "en",
      content = "Original",
      sourceUrl = "url",
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = 0L,
    )
    dao.insert(article)

    val translator = FakeTranslatorService("Translated")
    val repo: ArticleRepo = ArticleRepository(
      wiki = fakeWiki,
      dao = dao,
      settings = FakeSettingsStateProvider(SettingsState(learningLanguageCode = "es")),
      translator = translator,
    )

    val translated = repo.translateSummary(article)
    assertEquals("Translated", translated)
    assertEquals("Original", translator.lastQuery)
    assertEquals("en|es", translator.lastLangPair)
  }

  @Test
  fun translateReturnsInputWord() = runTest {
    val translator = FakeTranslatorService("Correcto")
    val repo: ArticleRepo = ArticleRepository(
      wiki = fakeWiki,
      dao = FakeArticleDao(),
      settings = FakeSettingsStateProvider(SettingsState(learningLanguageCode = "es")),
      translator = translator,
    )

    val result = repo.translate("Correct")
    assertEquals("Correcto", result)
    assertEquals("Correct", translator.lastQuery)
    assertEquals("en|es", translator.lastLangPair)
  }
}
