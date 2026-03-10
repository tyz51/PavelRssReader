package com.pavel.pavelrssreader.presentation.webview

import android.net.Uri
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.model.TextSpan

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
            CenterAlignedTopAppBar(
                title = {
                    val article = state.article
                    if (article != null) {
                        Text(
                            text = article.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    val threshold = 80.dp.toPx()
                    var totalX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalX = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalX += dragAmount },
                        onDragEnd = { if (totalX < -threshold) viewModel.goToNextArticle() }
                    )
                }
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val article = state.article ?: return@Box
                    ArticleContent(
                        article = article,
                        blocks = state.contentBlocks,
                        titleFontSize = state.titleFontSize,
                        bodyFontSize = state.bodyFontSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleContent(
    article: Article,
    blocks: List<ContentBlock>,
    titleFontSize: Float,
    bodyFontSize: Float,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary

    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = article.title,
                fontSize = titleFontSize.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (titleFontSize * 1.3f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            val host = Uri.parse(article.link).host ?: article.link
            Text(
                text = buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            article.link,
                            TextLinkStyles(SpanStyle(color = linkColor))
                        )
                    ) { append(host) }
                },
                fontSize = (bodyFontSize - 4).sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
        }
        items(blocks) { block ->
            when (block) {
                is ContentBlock.Heading -> HeadingItem(block)
                is ContentBlock.Paragraph -> ParagraphItem(block, bodyFontSize.sp, linkColor)
                is ContentBlock.Image -> ImageItem(block, (bodyFontSize - 4).sp)
                is ContentBlock.Quote -> QuoteItem(block, bodyFontSize.sp)
            }
        }
        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun HeadingItem(block: ContentBlock.Heading) {
    val style = when (block.level) {
        2 -> MaterialTheme.typography.titleLarge
        3, 4 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = block.text,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun ParagraphItem(
    block: ContentBlock.Paragraph,
    bodyFontSize: TextUnit,
    linkColor: Color
) {
    Text(
        text = buildAnnotatedString {
            block.spans.forEach { span ->
                when (span) {
                    is TextSpan.Plain -> append(span.text)
                    is TextSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                    is TextSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(span.text) }
                    is TextSpan.Link -> withLink(
                        LinkAnnotation.Url(
                            span.url,
                            TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                        )
                    ) { append(span.text) }
                }
            }
        },
        fontSize = bodyFontSize,
        lineHeight = bodyFontSize * 1.6f,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun ImageItem(
    block: ContentBlock.Image,
    captionFontSize: TextUnit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        AsyncImage(
            model = block.url,
            contentDescription = block.caption,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        if (block.caption != null) {
            Text(
                text = block.caption,
                fontSize = captionFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun QuoteItem(
    block: ContentBlock.Quote,
    bodyFontSize: TextUnit
) {
    val borderColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .drawBehind {
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(4.dp.toPx(), size.height)
                )
            }
    ) {
        Text(
            text = block.text,
            fontSize = bodyFontSize,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
