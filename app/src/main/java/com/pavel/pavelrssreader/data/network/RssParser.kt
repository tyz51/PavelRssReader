package com.pavel.pavelrssreader.data.network

import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedFeed(val feedTitle: String, val articles: List<ArticleEntity>)

class RssParser {

    fun parse(xml: String, feedId: Long): ParsedFeed {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            // Advance to first start tag to detect format
            var eventType = parser.eventType
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next()
            }

            when (parser.name) {
                "rss" -> parseRss2(parser, feedId)
                "feed" -> parseAtom(parser, feedId)
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
        var title = ""; var link = ""; var description = ""; var contentEncoded = ""
        var guid = ""; var pubDate = ""; var imageUrl = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        inItem = true
                        title = ""; link = ""; description = ""; contentEncoded = ""
                        guid = ""; pubDate = ""; imageUrl = ""
                    }
                    "title" -> if (!inItem) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> link = parser.nextText()
                    "description" -> description = parser.nextText()
                    "content:encoded" -> contentEncoded = parser.nextText()
                    "guid" -> guid = parser.nextText()
                    "pubDate" -> pubDate = parser.nextText()
                    // Image tags — first non-blank value wins
                    "media:thumbnail" -> {
                        if (inItem && imageUrl.isBlank())
                            imageUrl = parser.getAttributeValue(null, "url") ?: ""
                    }
                    "media:content" -> {
                        if (inItem && imageUrl.isBlank()) {
                            val medium = parser.getAttributeValue(null, "medium") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (medium == "image" || type.startsWith("image/"))
                                imageUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                    }
                    "enclosure" -> {
                        if (inItem && imageUrl.isBlank()) {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image/"))
                                imageUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    val body = contentEncoded.ifBlank { description }
                    val resolvedImage = imageUrl.ifBlank { extractFirstImageUrl(body) }
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = guid.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = body,
                            publishedAt = parseRfc822(pubDate),
                            fetchedAt = System.currentTimeMillis(),
                            imageUrl = resolvedImage.ifBlank { null },
                            sourceName = feedTitle
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
        var title = ""; var link = ""; var summary = ""; var id = ""; var updated = ""; var imageUrl = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> {
                        inEntry = true
                        title = ""; link = ""; summary = ""; id = ""; updated = ""; imageUrl = ""
                    }
                    "title" -> if (!inEntry) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> {
                        val rel = parser.getAttributeValue(null, "rel") ?: "alternate"
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        when {
                            type.startsWith("image/") && inEntry && imageUrl.isBlank() -> imageUrl = href
                            rel == "alternate" || rel == "enclosure" && !type.startsWith("image/") -> if (link.isBlank()) link = href
                            else -> if (link.isBlank()) link = href
                        }
                    }
                    "summary", "content" -> summary = parser.nextText()
                    "id" -> id = parser.nextText()
                    "updated" -> updated = parser.nextText()
                    "media:thumbnail" -> {
                        if (inEntry && imageUrl.isBlank())
                            imageUrl = parser.getAttributeValue(null, "url") ?: ""
                    }
                    "media:content" -> {
                        if (inEntry && imageUrl.isBlank()) {
                            val medium = parser.getAttributeValue(null, "medium") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (medium == "image" || type.startsWith("image/"))
                                imageUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry" && inEntry) {
                    val resolvedImage = imageUrl.ifBlank { extractFirstImageUrl(summary) }
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = id.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = summary,
                            publishedAt = parseIso8601(updated),
                            fetchedAt = System.currentTimeMillis(),
                            imageUrl = resolvedImage.ifBlank { null },
                            sourceName = feedTitle
                        )
                    )
                    inEntry = false
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed(feedTitle, articles)
    }

    /** Extracts the src of the first <img> tag from an HTML string, or empty string if none. */
    private fun extractFirstImageUrl(html: String): String =
        Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1) ?: ""

    private fun parseRfc822(date: String): Long {
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        return runCatching { fmt.parse(date.trim())?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())
    }

    private fun parseIso8601(date: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH)
        val normalized = date.replace("Z", "+0000").trim()
        // Try with milliseconds first, then without
        return runCatching { fmt.parse(normalized)?.time }
            .getOrNull()
            ?: runCatching {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).parse(normalized)?.time
            }.getOrNull()
            ?: System.currentTimeMillis()
    }
}
