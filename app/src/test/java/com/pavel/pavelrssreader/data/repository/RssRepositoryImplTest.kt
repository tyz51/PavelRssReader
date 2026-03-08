package com.pavel.pavelrssreader.data.repository

import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import com.pavel.pavelrssreader.data.network.ParsedFeed
import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import com.pavel.pavelrssreader.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RssRepositoryImplTest {

    private val feedDao = mockk<FeedDao>(relaxed = true)
    private val articleDao = mockk<ArticleDao>(relaxed = true)
    private val apiService = mockk<RssApiService>()
    private val parser = mockk<RssParser>()

    private val repo = RssRepositoryImpl(feedDao, articleDao, apiService, parser)

    @Test
    fun `refreshFeed fetches XML, parses it, inserts articles, and cleans expired`() = runTest {
        val feed = com.pavel.pavelrssreader.domain.model.Feed(id = 1L, url = "https://feed.com/rss", title = "Feed", addedAt = 0L)
        val rawXml = "<rss/>"
        val parsedArticle = ArticleEntity(feedId = 1L, guid = "g1", title = "T", link = "L",
            description = "D", publishedAt = 0L, fetchedAt = 0L)
        coEvery { apiService.fetchFeed("https://feed.com/rss") } returns rawXml
        every { parser.parse(rawXml, 1L) } returns ParsedFeed("Feed", listOf(parsedArticle))

        val result = repo.refreshFeed(feed)

        assertTrue(result is Result.Success)
        coVerify { articleDao.insertArticles(listOf(parsedArticle)) }
        coVerify { articleDao.deleteExpiredArticles(any()) }
    }

    @Test
    fun `refreshFeed returns Error when network throws`() = runTest {
        val feed = com.pavel.pavelrssreader.domain.model.Feed(id = 1L, url = "https://bad.url/rss", title = "Bad", addedAt = 0L)
        coEvery { apiService.fetchFeed(any()) } throws Exception("Connection refused")

        val result = repo.refreshFeed(feed)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `addFeed validates URL format before fetching`() = runTest {
        val result = repo.addFeed("not-a-url")
        assertTrue(result is Result.Error)
    }
}
