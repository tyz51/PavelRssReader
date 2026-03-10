package com.pavel.pavelrssreader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import javax.inject.Inject

class ArticleContentFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0")
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .build()
            val html = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                response.body?.string() ?: return@withContext ""
            }
            extractBody(html, url)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun extractBody(html: String, baseUrl: String): String {
        val doc = Jsoup.parse(html, baseUrl)
        // Strip structural chrome: navigation, headers, ads, sidebars
        doc.select(NOISE_SELECTOR).remove()
        // Walk selectors in priority order (article is more precise than main).
        // For each selector take the element with the most text, then return the first
        // selector group that yields substantial content (>200 chars).
        // This ensures a page with both <article> and <main> always prefers <article>.
        val best = ARTICLE_SELECTORS
            .asSequence()
            .mapNotNull { selector -> doc.select(selector).maxByOrNull { it.text().length } }
            .firstOrNull { it.text().length > 200 }
        if (best != null) {
            // Strip inline clutter (share buttons, donate banners, comments) before returning
            best.select(INLINE_NOISE_SELECTOR).remove()
            // Resolve lazy-loaded images: many sites ship <img src="placeholder" data-src="real-url">
            // Html.fromHtml only reads `src`, so swap in the real URL before serialising.
            best.select("img").forEach { img ->
                val src = img.attr("src")
                if (src.isBlank() || src.startsWith("data:")) {
                    val real = LAZY_SRC_ATTRS.firstNotNullOfOrNull { attr ->
                        img.attr(attr).takeIf { it.isNotBlank() }
                    } ?: run {
                        // Last resort: first URL from srcset
                        img.attr("srcset").split(",").firstOrNull()
                            ?.trim()?.split("\\s+".toRegex())?.firstOrNull()
                            ?.takeIf { it.isNotBlank() }
                    }
                    if (real != null) img.attr("src", real)
                }
            }
            // Drop any img that still has no usable src
            best.select("img[src=''], img:not([src])").remove()
            // Remove duplicate images — sites often place the same lead image both as a
            // hero figure and again inside the article body.
            val seenSrcs = mutableSetOf<String>()
            best.select("img[src]").forEach { img ->
                val s = img.attr("src")
                if (s.startsWith("data:") || !seenSrcs.add(s)) img.remove()
            }
            return best.html()
        }
        // Fallback for JS-rendered pages (e.g. React apps like CBC) that store article
        // content inside window.__INITIAL_STATE__ rather than server-rendered HTML.
        // Use raw html (not doc) because NOISE_SELECTOR removes <script> elements from doc.
        val stateHtml = extractFromInitialState(html)
        if (stateHtml.length > 200) return stateHtml
        return doc.body()?.html() ?: ""
    }

    /**
     * Extracts article body from window.__INITIAL_STATE__ JSON embedded in a <script> tag.
     * CBC and similar React/Next.js sites store article paragraphs as JSON objects with
     * {"type":"html","tag":"p","content":[{"type":"text","content":"text here"}]}.
     * Uses the raw HTML string because Jsoup's NOISE_SELECTOR removes <script> nodes.
     */
    private fun extractFromInitialState(html: String): String {
        val markerIdx = html.indexOf("window.__INITIAL_STATE__")
        if (markerIdx == -1) return ""
        val objStart = html.indexOf('{', markerIdx)
        if (objStart == -1) return ""
        val jsonStr = extractJsonObject(html, objStart)
        if (jsonStr.isEmpty()) return ""
        return try {
            val json = JSONObject(jsonStr)
            val body = json.getJSONObject("detail")
                .getJSONObject("content")
                .getJSONArray("body")
            buildArticleHtmlFromBodyArray(body)
        } catch (_: Exception) {
            ""
        }
    }

    /** Extracts a complete JSON object starting at [start] using brace counting. */
    private fun extractJsonObject(text: String, start: Int): String {
        var depth = 0
        var inString = false
        var escape = false
        val sb = StringBuilder()
        for (i in start until text.length) {
            val c = text[i]
            sb.append(c)
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return sb.toString()
                }
            }
        }
        return ""
    }

    private fun buildArticleHtmlFromBodyArray(body: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until body.length()) {
            val item = body.optJSONObject(i) ?: continue
            if (item.optString("type") != "html") continue
            val tag = item.optString("tag", "").lowercase()
            val text = extractNodeText(item)
            if (text.isBlank()) continue
            when (tag) {
                "p" -> sb.append("<p>").append(text).append("</p>\n")
                "h2" -> sb.append("<h2>").append(text).append("</h2>\n")
                "h3", "h4", "h5", "h6" -> sb.append("<h3>").append(text).append("</h3>\n")
                "blockquote" -> sb.append("<blockquote>").append(text).append("</blockquote>\n")
            }
        }
        return sb.toString()
    }

    private fun extractNodeText(node: JSONObject): String {
        val content = node.optJSONArray("content") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val child = content.optJSONObject(i) ?: continue
            when (child.optString("type")) {
                "text" -> sb.append(child.optString("content"))
                "html" -> sb.append(extractNodeText(child))
            }
        }
        return sb.toString()
    }

    companion object {
        private const val NOISE_SELECTOR =
            "script, style, nav, header, footer, aside, " +
            // Match ad- only at the START of a class word (^=) or after a space (*= " ad-")
            // to avoid false positives like "wallpaper-ad-wrap" that contain ad- mid-word
            "[class^=ad-], [class*= ad-], [id^=ad-], " +
            "[class*=banner], [class*=cookie], " +
            "[class*=popup], [class*=modal], " +
            "div[class*=sidebar], aside[class*=sidebar], section[class*=sidebar], " +
            "div[class*=menu], nav[class*=menu], ul[class*=menu], " +
            "[id*=sidebar], [id*=menu], [id*=nav]"

        private const val INLINE_NOISE_SELECTOR =
            // Duplicate headline — app already shows the title above the body
            "h1, " +
            // Author / byline blocks
            "[class*=byline], [class*=author-info], [class*=authorDetails], " +
            // Audio / text-to-speech players (SVG buttons that Html.fromHtml drops, leaving blank space)
            "[class*=tts], [class*=textToSpeech], [class*=text-to-speech], [class*=audio-player], " +
            // Deck / kicker subheadlines that repeat summary info
            "[class*=deck], [class*=kicker], " +
            // Social / share / comment clutter
            "[class*=social-button], [class*=social-share], .sharedaddy, " +
            "[class*=comment], " +
            "[class*=fb-quote], [class*=fb-root], " +
            ".b-r"

        // Common lazy-loading src attributes, in priority order
        private val LAZY_SRC_ATTRS = listOf(
            "data-src", "data-lazy-src", "data-original", "data-lazy",
            "data-hi-res-src", "data-full-src", "data-image-src"
        )

        private val ARTICLE_SELECTORS = listOf(
            "article",
            "[itemprop=articleBody]",
            "[class*=article-body]",
            "[class*=article__body]",
            "[class*=article-text]",
            "[class*=article__text]",
            "[class*=article-content]",
            "[class*=article__content]",
            "[class*=post-body]",
            "[class*=post-content]",
            "[class*=entry-content]",
            "[class*=story-body]",
            "[class*=story-content]",
            "[class*=detail-content]",
            "[id*=articleBody]",
            "[id*=article-body]",
            "[id*=story-body]",
            "[id*=storyContent]",
            "[id*=detailContent]",
            "main"
        )
    }
}
