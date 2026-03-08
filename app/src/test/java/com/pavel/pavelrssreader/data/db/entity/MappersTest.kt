package com.pavel.pavelrssreader.data.db.entity

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun `FeedEntity toDomain maps all fields`() {
        val entity = FeedEntity(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L)
        val domain = entity.toDomain()
        assertEquals(Feed(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L), domain)
    }

    @Test
    fun `Feed toEntity maps all fields`() {
        val domain = Feed(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L)
        val entity = domain.toEntity()
        assertEquals(FeedEntity(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L), entity)
    }

    @Test
    fun `ArticleEntity toDomain maps all fields`() {
        val now = 999L
        val entity = ArticleEntity(
            id = 2L, feedId = 1L, guid = "g1", title = "T", link = "L",
            description = "D", publishedAt = now, fetchedAt = now,
            isRead = true, isFavorite = false
        )
        val domain = entity.toDomain()
        assertEquals("g1", domain.guid)
        assertEquals(true, domain.isRead)
    }
}
