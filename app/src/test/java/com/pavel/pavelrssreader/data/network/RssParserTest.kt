package com.pavel.pavelrssreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RssParserTest {

    private val parser = RssParser()

    private val rss2Xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <item>
              <title>Article One</title>
              <link>https://example.com/1</link>
              <description>Body of article one</description>
              <guid>unique-guid-1</guid>
              <pubDate>Mon, 06 Mar 2026 12:00:00 +0000</pubDate>
            </item>
            <item>
              <title>Article Two</title>
              <link>https://example.com/2</link>
              <description>Body of article two</description>
              <guid>unique-guid-2</guid>
              <pubDate>Sun, 05 Mar 2026 09:00:00 +0000</pubDate>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val atomXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Atom Feed</title>
          <entry>
            <title>Atom Article</title>
            <link href="https://example.com/atom/1"/>
            <summary>Atom summary</summary>
            <id>atom-id-1</id>
            <updated>2026-03-06T12:00:00Z</updated>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun `parse RSS 2 returns feed title and articles`() {
        val result = parser.parse(rss2Xml, feedId = 1L)
        assertEquals("Test Feed", result.feedTitle)
        assertEquals(2, result.articles.size)
        assertEquals("Article One", result.articles[0].title)
        assertEquals("https://example.com/1", result.articles[0].link)
        assertEquals("unique-guid-1", result.articles[0].guid)
    }

    @Test
    fun `parse Atom returns feed title and articles`() {
        val result = parser.parse(atomXml, feedId = 1L)
        assertEquals("Atom Feed", result.feedTitle)
        assertEquals(1, result.articles.size)
        assertEquals("Atom Article", result.articles[0].title)
        assertEquals("https://example.com/atom/1", result.articles[0].link)
        assertEquals("atom-id-1", result.articles[0].guid)
    }

    @Test
    fun `parse assigns correct feedId to articles`() {
        val result = parser.parse(rss2Xml, feedId = 42L)
        result.articles.forEach { assertEquals(42L, it.feedId) }
    }

    @Test
    fun `parse malformed XML returns empty result without crashing`() {
        val result = parser.parse("<not-rss/>", feedId = 1L)
        assertEquals(0, result.articles.size)
    }
}
