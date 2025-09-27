package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleDao
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.catalog.impl.data.ArticleRepository
import com.archstarter.feature.catalog.impl.data.WikipediaService
import com.archstarter.feature.catalog.impl.data.WikipediaSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    override suspend fun insert(article: ArticleEntity) { state.value = state.value + article }
    val inserted: List<ArticleEntity> get() = state.value
  }

  @Test
  fun refreshStoresTrimmedSummaryFromWikipedia() = runTest {
    val dao = FakeArticleDao()
    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary()
      },
      dao = dao,
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
      summaryLanguage = null,
      content = "Original",
      sourceUrl = "url",
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = 0L,
    )
    dao.insert(article)

    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary(id = 2)
      },
      dao = dao,
    )

    val translated = repo.translateSummary(article)
    assertEquals("Original", translated)
  }

  @Test
  fun translateReturnsInputWord() = runTest {
    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary()
      },
      dao = FakeArticleDao(),
    )

    val result = repo.translate("Hola")
    assertEquals("Hola", result)
  }
}
