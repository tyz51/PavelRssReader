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
        // Strip noise before extracting content
        doc.select(
            "script, style, nav, header, footer, aside, " +
            "[class*=ad-], [id*=ad-], [class*=banner], [class*=cookie], " +
            "[class*=popup], [class*=modal], [class*=sidebar], [class*=menu], " +
            "[id*=sidebar], [id*=menu], [id*=nav]"
        ).remove()
        // Try selectors from most to least specific
        for (selector in ARTICLE_SELECTORS) {
            val el = doc.selectFirst(selector) ?: continue
            if (el.text().length > 200) return el.html()
        }
        return doc.body()?.html() ?: ""
    }

    companion object {
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
            "main"
        )
    }
}
