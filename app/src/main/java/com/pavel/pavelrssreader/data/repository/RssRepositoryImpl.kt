package com.pavel.pavelrssreader.data.repository

import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import com.pavel.pavelrssreader.data.db.entity.toDomain
import com.pavel.pavelrssreader.data.db.entity.toEntity
import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.repository.RssRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RssRepositoryImpl @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val apiService: RssApiService,
    private val parser: RssParser
) : RssRepository {

    override fun getAllArticles(): Flow<List<Article>> =
        articleDao.getAllArticles().map { it.map { entity -> entity.toDomain() } }

    override fun getFavouriteArticles(): Flow<List<Article>> =
        articleDao.getFavouriteArticles().map { it.map { entity -> entity.toDomain() } }

    override fun getAllFeeds(): Flow<List<Feed>> =
        feedDao.getAllFeeds().map { it.map { entity -> entity.toDomain() } }

    override suspend fun addFeed(url: String): Result<Feed> {
        if (!isValidUrl(url)) return Result.Error("Invalid URL: $url")
        return try {
            val rawXml = apiService.fetchFeed(url)
            val parsed = parser.parse(rawXml, feedId = 0L)
            val feedTitle = parsed.feedTitle.ifBlank { url }
            val entity = FeedEntity(url = url, title = feedTitle, addedAt = System.currentTimeMillis())
            val id = feedDao.insertFeed(entity)
            val feed = Feed(id = id, url = url, title = feedTitle, addedAt = entity.addedAt)
            val articles = parsed.articles.map { it.copy(feedId = id) }
            articleDao.insertArticles(articles)
            articleDao.deleteExpiredArticles(System.currentTimeMillis() - 86_400_000L)
            Result.Success(feed)
        } catch (e: Exception) {
            Result.Error("Failed to add feed: ${e.message}", e)
        }
    }

    override suspend fun deleteFeed(feedId: Long) {
        feedDao.deleteFeed(feedId)
    }

    override suspend fun refreshFeed(feed: Feed): Result<Unit> {
        return try {
            val rawXml = apiService.fetchFeed(feed.url)
            val parsed = parser.parse(rawXml, feed.id)
            articleDao.insertArticles(parsed.articles)
            articleDao.deleteExpiredArticles(System.currentTimeMillis() - 86_400_000L)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh ${feed.title}: ${e.message}", e)
        }
    }

    override suspend fun refreshAllFeeds(): Result<Unit> {
        val feeds = feedDao.getAllFeedsOneShot().map { it.toDomain() }
        val errors = feeds.mapNotNull { feed ->
            (refreshFeed(feed) as? Result.Error)?.message
        }
        return if (errors.isEmpty()) Result.Success(Unit)
        else Result.Error(errors.joinToString("\n"))
    }

    override suspend fun setFavourite(articleId: Long, isFavorite: Boolean) {
        articleDao.setFavourite(articleId, isFavorite)
    }

    override suspend fun markAsRead(articleId: Long) {
        articleDao.markAsRead(articleId)
    }

    private fun isValidUrl(url: String): Boolean = try {
        val u = java.net.URL(url)
        u.protocol in listOf("http", "https")
    } catch (e: Exception) { false }
}
