package com.example.feature.catalog.impl.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "articles")
data class ArticleEntity(
  @PrimaryKey val id: Int,
  val title: String,
  val summary: String,
  val content: String,
  val sourceUrl: String,
  val originalWord: String,
  val translatedWord: String,
  val ipa: String?
)

@Dao
interface ArticleDao {
  @Query("SELECT * FROM articles ORDER BY id DESC")
  fun getArticles(): Flow<List<ArticleEntity>>

  @Query("SELECT * FROM articles WHERE id = :id")
  suspend fun getArticle(id: Int): ArticleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(article: ArticleEntity)
}

@Database(entities = [ArticleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun articleDao(): ArticleDao
}
