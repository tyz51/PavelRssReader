package com.pavel.pavelrssreader.data.network

import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedFeed(val feedTitle: String, val articles: List<ArticleEntity>)

class RssParser {

    private val rfc822Format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

    fun parse(xml: String, feedId: Long): ParsedFeed {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            when {
                xml.contains("<rss") -> parseRss2(parser, feedId)
                xml.contains("<feed") -> parseAtom(parser, feedId)
                else -> ParsedFeed("", emptyList())
            }
        } catch (e: Exception) {
            ParsedFeed("", emptyList())
        }
    }

    private fun parseRss2(parser: XmlPullParser, feedId: Long): ParsedFeed {
        var feedTitle = ""
        val articles = mutableListOf<ArticleEntity>()
        var inItem = false
        var title = ""; var link = ""; var description = ""; var guid = ""; var pubDate = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { inItem = true; title = ""; link = ""; description = ""; guid = ""; pubDate = "" }
                    "title" -> if (!inItem) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> link = parser.nextText()
                    "description" -> description = parser.nextText()
                    "guid" -> guid = parser.nextText()
                    "pubDate" -> pubDate = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = guid.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = description,
                            publishedAt = parseRfc822(pubDate),
                            fetchedAt = System.currentTimeMillis()
                        )
                    )
                    inItem = false
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed(feedTitle, articles)
    }

    private fun parseAtom(parser: XmlPullParser, feedId: Long): ParsedFeed {
        var feedTitle = ""
        val articles = mutableListOf<ArticleEntity>()
        var inEntry = false
        var title = ""; var link = ""; var summary = ""; var id = ""; var updated = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> { inEntry = true; title = ""; link = ""; summary = ""; id = ""; updated = "" }
                    "title" -> if (!inEntry) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> link = parser.getAttributeValue(null, "href") ?: ""
                    "summary", "content" -> summary = parser.nextText()
                    "id" -> id = parser.nextText()
                    "updated" -> updated = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry" && inEntry) {
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = id.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = summary,
                            publishedAt = parseIso8601(updated),
                            fetchedAt = System.currentTimeMillis()
                        )
                    )
                    inEntry = false
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed(feedTitle, articles)
    }

    private fun parseRfc822(date: String): Long =
        runCatching { rfc822Format.parse(date.trim())?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())

    private fun parseIso8601(date: String): Long =
        runCatching { iso8601Format.parse(date.replace("Z", "+0000").trim())?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())
}
