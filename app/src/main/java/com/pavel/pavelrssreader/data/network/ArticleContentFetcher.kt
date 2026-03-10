package com.pavel.pavelrssreader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        return doc.body()?.html() ?: ""
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
