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
import java.net.UnknownHostException
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
        val normalizedUrl = normalizeUrl(url)
        if (!isValidUrl(normalizedUrl)) return Result.Error("Invalid URL: $normalizedUrl")
        return try {
            val rawXml = apiService.fetchFeed(normalizedUrl)
            val parsed = parser.parse(rawXml, feedId = 0L)
            val feedTitle = parsed.feedTitle.ifBlank { normalizedUrl }
            val entity = FeedEntity(url = normalizedUrl, title = feedTitle, addedAt = System.currentTimeMillis())
            val id = feedDao.insertFeed(entity)
            val feed = Feed(id = id, url = url, title = feedTitle, addedAt = entity.addedAt)
            val resolvedTitle = parsed.feedTitle.ifBlank { normalizedUrl }
            val articles = parsed.articles.map { it.copy(feedId = id, sourceName = resolvedTitle) }
            articleDao.insertArticles(articles)
            Result.Success(feed)
        } catch (e: UnknownHostException) {
            val host = e.message?.substringBefore(':')?.trim() ?: normalizedUrl
            Result.Error("Cannot reach $host. Check the URL and your internet connection.", e)
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
            val resolvedTitle = parsed.feedTitle.ifBlank { feed.title }
            val articlesWithSource = parsed.articles.map { it.copy(sourceName = resolvedTitle) }
            articleDao.insertArticles(articlesWithSource)
            parsed.articles.forEach { article ->
                articleDao.updateContent(
                    feedId = article.feedId,
                    guid = article.guid,
                    title = article.title,
                    link = article.link,
                    description = article.description,
                    publishedAt = article.publishedAt,
                    fetchedAt = article.fetchedAt
                )
            }
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

    private fun normalizeUrl(url: String): String {
        var trimmed = url.trim()
        // Add https:// if the user omitted the scheme entirely (e.g. "pm.gc.ca/en/news.rss")
        if (!trimmed.contains("://")) trimmed = "https://$trimmed"
        // Commas are never valid in hostnames — replace with dots to handle common input typos
        // (e.g. "https://reform,news/feed" → "https://reform.news/feed")
        // Only the authority segment (between "://" and the first "/") is affected.
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd < 0) return trimmed
        val authorityStart = schemeEnd + 3
        val authorityEnd = trimmed.indexOf('/', authorityStart).takeIf { it >= 0 } ?: trimmed.length
        return trimmed.substring(0, authorityStart) +
            trimmed.substring(authorityStart, authorityEnd).replace(',', '.') +
            trimmed.substring(authorityEnd)
    }

    private fun isValidUrl(url: String): Boolean = try {
        val u = java.net.URL(url)
        u.protocol in listOf("http", "https")
    } catch (e: Exception) { false }
}
