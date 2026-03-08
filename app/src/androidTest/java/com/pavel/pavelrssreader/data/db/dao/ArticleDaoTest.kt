package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.pavelrssreader.data.db.AppDatabase
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var articleDao: ArticleDao
    private lateinit var feedDao: FeedDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        articleDao = db.articleDao()
        feedDao = db.feedDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveArticles() = runTest {
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        val articles = listOf(
            ArticleEntity(feedId = 1L, guid = "g1", title = "T1", link = "L1",
                description = "D1", publishedAt = 1000L, fetchedAt = 1000L)
        )
        articleDao.insertArticles(articles)
        val result = articleDao.getAllArticles().first()
        assertEquals(1, result.size)
        assertEquals("T1", result[0].title)
    }

    @Test
    fun deleteExpiredArticles_keepsNonExpiredAndFavourites() = runTest {
        val now = System.currentTimeMillis()
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        articleDao.insertArticles(listOf(
            ArticleEntity(id = 1L, feedId = 1L, guid = "old", title = "Old", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = now - 90_000_000L),
            ArticleEntity(id = 2L, feedId = 1L, guid = "fav", title = "Fav", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = now - 90_000_000L,
                isFavorite = true),
            ArticleEntity(id = 3L, feedId = 1L, guid = "new", title = "New", link = "L",
                description = "D", publishedAt = now, fetchedAt = now)
        ))
        articleDao.deleteExpiredArticles(now - 86_400_000L)
        val result = articleDao.getAllArticles().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.guid == "fav" })
        assertTrue(result.any { it.guid == "new" })
    }

    @Test
    fun toggleFavourite() = runTest {
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        articleDao.insertArticles(listOf(
            ArticleEntity(id = 1L, feedId = 1L, guid = "g1", title = "T", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = 0L)
        ))
        articleDao.setFavourite(1L, true)
        assertTrue(articleDao.getArticleById(1L)!!.isFavorite)

        articleDao.setFavourite(1L, false)
        assertFalse(articleDao.getArticleById(1L)!!.isFavorite)
    }
}
