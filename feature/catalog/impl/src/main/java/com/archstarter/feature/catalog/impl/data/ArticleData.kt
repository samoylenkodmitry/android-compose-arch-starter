package com.archstarter.feature.catalog.impl.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "articles")
data class ArticleEntity(
  @PrimaryKey val id: Int,
  val title: String,
  val summaryOriginal: String,
  val summaryTranslated: String?,
  val contentOriginal: String,
  val contentTranslated: String?,
  val originalWord: String?,
  val translatedWord: String?,
  val ipa: String?,
  val sourceUrl: String,
  val createdAt: Long
)

@Dao
interface ArticleDao {
  @Query("SELECT * FROM articles ORDER BY createdAt DESC")
  fun getArticles(): Flow<List<ArticleEntity>>

  @Query("SELECT * FROM articles WHERE id = :id")
  suspend fun getArticle(id: Int): ArticleEntity?

  @Query("SELECT * FROM articles WHERE id = :id")
  fun observeArticle(id: Int): Flow<ArticleEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(article: ArticleEntity)
}

@Database(entities = [ArticleEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
  abstract fun articleDao(): ArticleDao

  companion object {
    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `articles_new` (
            `id` INTEGER NOT NULL,
            `title` TEXT NOT NULL,
            `summaryOriginal` TEXT NOT NULL,
            `summaryTranslated` TEXT,
            `contentOriginal` TEXT NOT NULL,
            `contentTranslated` TEXT,
            `originalWord` TEXT,
            `translatedWord` TEXT,
            `ipa` TEXT,
            `sourceUrl` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        database.execSQL(
          """
          INSERT INTO `articles_new` (
            `id`, `title`, `summaryOriginal`, `summaryTranslated`, `contentOriginal`, `contentTranslated`,
            `originalWord`, `translatedWord`, `ipa`, `sourceUrl`, `createdAt`
          )
          SELECT `id`, `title`, `summary`, `summary`, `content`, `content`, `originalWord`, `translatedWord`, `ipa`, `sourceUrl`, `createdAt`
          FROM `articles`
          """.trimIndent()
        )
        database.execSQL("DROP TABLE `articles`")
        database.execSQL("ALTER TABLE `articles_new` RENAME TO `articles`")
      }
    }
  }
}
