package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavouriteArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isFavorite = :isFavorite WHERE id = :articleId")
    suspend fun setFavourite(articleId: Long, isFavorite: Boolean)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :articleId")
    suspend fun markAsRead(articleId: Long)

    @Query("DELETE FROM articles WHERE isFavorite = 0 AND fetchedAt < :cutoffTime")
    suspend fun deleteExpiredArticles(cutoffTime: Long)
}
