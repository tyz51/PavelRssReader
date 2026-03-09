package com.pavel.pavelrssreader.presentation.webview

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    articleId: Long,
    onBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val article = state.article
                    if (article != null) {
                        IconButton(onClick = { viewModel.toggleFavourite() }) {
                            if (article.isFavorite) {
                                Icon(Icons.Default.Favorite, contentDescription = "Remove from favourites")
                            } else {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Add to favourites")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                key(state.article?.id) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                with(settings) {
                                    javaScriptEnabled = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(false)
                                    builtInZoomControls = false
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        val url = request.url.toString()
                                        if (url.startsWith("http")) {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            )
                                        }
                                        return true
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            val article = state.article ?: return@AndroidView
                            val contentTag = "${article.id}:${state.fullContent != null}" +
                                    ":${state.titleFontSize.toInt()}:${state.bodyFontSize.toInt()}"
                            if (webView.tag == contentTag) return@AndroidView
                            webView.tag = contentTag

                            val content = state.fullContent ?: (article.description ?: "")
                            val host = Uri.parse(article.link).host ?: article.link
                            // Show RSS thumbnail during preview; full content has inline images
                            val previewImg = if (state.fullContent == null && article.imageUrl != null)
                                """<img src="${article.imageUrl}">""" else ""

                            // CSS px ≈ dp in WebView with the viewport meta tag
                            val titlePx = state.titleFontSize.toInt()
                            val bodyPx  = state.bodyFontSize.toInt()

                            val html = buildString {
                                append("<html><head>")
                                append("""<meta name="viewport" content="width=device-width,initial-scale=1">""")
                                append("<style>")
                                append("body{margin:0;padding:24px 48px 64px;word-wrap:break-word;}")
                                append("h1.t{font-size:${titlePx}px;font-weight:bold;margin:0 0 6px;line-height:1.3;}")
                                append("a.s{font-size:${bodyPx - 4}px;color:#1976D2;text-decoration:none;}")
                                append("p,li,td,div{font-size:${bodyPx}px;line-height:1.6;}")
                                append("img{max-width:100%!important;height:auto!important;display:block;margin:12px 0;}")
                                append("figure{margin:0;}figcaption{font-size:${bodyPx - 4}px;color:#666;}")
                                append("</style></head><body>")
                                append("""<h1 class="t">${Html.escapeHtml(article.title)}</h1>""")
                                append("""<p><a class="s" href="${article.link}">$host</a></p>""")
                                append(previewImg)
                                append(content)
                                append("</body></html>")
                            }
                            webView.loadDataWithBaseURL(
                                article.link, html, "text/html", "UTF-8", null
                            )
                        },
                        onRelease = { it.destroy() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}
