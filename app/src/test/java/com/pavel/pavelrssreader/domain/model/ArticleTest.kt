package com.pavel.pavelrssreader.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleTest {

    @Test
    fun `isExpired returns true when fetchedAt older than 24h and not favourite`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 90_000_000L,
            fetchedAt = now - 90_000_000L, // 25 hours ago
            isRead = false,
            isFavorite = false
        )
        assertTrue(article.isExpired(now))
    }

    @Test
    fun `isExpired returns false when favourite regardless of age`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 90_000_000L,
            fetchedAt = now - 90_000_000L, // 25 hours ago
            isRead = false,
            isFavorite = true
        )
        assertFalse(article.isExpired(now))
    }

    @Test
    fun `isExpired returns false when fetchedAt within 24h`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 3_600_000L,
            fetchedAt = now - 3_600_000L, // 1 hour ago
            isRead = false,
            isFavorite = false
        )
        assertFalse(article.isExpired(now))
    }
}
