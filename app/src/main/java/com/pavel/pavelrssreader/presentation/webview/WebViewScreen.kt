package com.pavel.pavelrssreader.presentation.webview

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.Coil

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val titleStyle = remember(state.titleFontSize) {
                            TextStyle(fontSize = state.titleFontSize.sp, fontWeight = FontWeight.Bold)
                        }
                        val context = LocalContext.current
                        Text(
                            text = state.article?.title ?: "",
                            style = titleStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, end = 48.dp, top = 24.dp)
                        )
                        val link = state.article?.link
                        if (link != null) {
                            TextButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                },
                                modifier = Modifier.padding(start = 36.dp)
                            ) {
                                Text(
                                    text = Uri.parse(link).host ?: link,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height((state.bodyFontSize * 3).dp))
                        AndroidView(
                            factory = { ctx ->
                                android.widget.TextView(ctx).apply {
                                    val density = ctx.resources.displayMetrics.density
                                    setLineSpacing(0f, 1.5f)
                                    setPadding(
                                        (48 * density).toInt(),
                                        0,
                                        (48 * density).toInt(),
                                        (64 * density).toInt()
                                    )
                                    movementMethod = LinkMovementMethod.getInstance()
                                    isClickable = true
                                    isFocusable = true
                                    setTextIsSelectable(true)
                                }
                            },
                            update = { textView ->
                                textView.textSize = state.bodyFontSize
                                val article = state.article
                                if (article != null) {
                                    // Tag encodes both the article and whether full content has arrived.
                                    // This ensures Html.fromHtml is re-run when fullContent
                                    // transitions from null (description) to the fetched article.
                                    val contentTag = "${article.id}:${state.fullContent != null}"
                                    if (textView.tag != contentTag) {
                                        textView.tag = contentTag
                                        val imageGetter = CoilImageGetter(
                                            textView = textView,
                                            imageLoader = Coil.imageLoader(textView.context),
                                            baseUrl = article.link
                                        )
                                        val content = state.fullContent ?: (article.description ?: "")
                                        textView.text = Html.fromHtml(
                                            content,
                                            Html.FROM_HTML_MODE_COMPACT,
                                            imageGetter,
                                            null
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
