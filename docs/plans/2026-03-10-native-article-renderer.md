# Native Article Renderer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the WebView-based article reader with a native Compose renderer that displays only text and images — iframes, social embeds, subscribe buttons, and any other elements are structurally impossible to render.

**Architecture:** Jsoup (already a dependency) parses the pre-cleaned HTML into a `List<ContentBlock>` (sealed class: Heading, Paragraph, Image, Quote). `WebViewViewModel` stores blocks instead of raw HTML. `WebViewScreen` renders them in a `LazyColumn` using `Text` + `AsyncImage` (Coil, already a dependency).

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Jsoup 1.x, Coil 2.7.0 (`coil.compose.AsyncImage`), `LinkAnnotation` (Compose BOM 2025.05.00)

---

### Task 1: ContentBlock sealed class

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/model/ContentBlock.kt`

**Step 1: Create the file**

```kotlin
package com.pavel.pavelrssreader.domain.model

sealed class ContentBlock {
    data class Heading(val level: Int, val text: String) : ContentBlock()
    data class Paragraph(val spans: List<TextSpan>) : ContentBlock()
    data class Image(val url: String, val caption: String?) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
}

sealed class TextSpan {
    data class Plain(val text: String) : TextSpan()
    data class Bold(val text: String) : TextSpan()
    data class Italic(val text: String) : TextSpan()
    data class Link(val text: String, val url: String) : TextSpan()
}
```

**Step 2: Compile to verify**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/domain/model/ContentBlock.kt
git commit -m "feat: add ContentBlock and TextSpan sealed classes"
```

---

### Task 2: HtmlToBlocks parser

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocks.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocksTest.kt`

**Step 1: Write the failing test**

Create `app/src/test/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocksTest.kt`:

```kotlin
package com.pavel.pavelrssreader.data.parser

import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.model.TextSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlToBlocksTest {

    @Test
    fun `heading h2 parsed`() {
        assertEquals(listOf(ContentBlock.Heading(2, "Hello")), HtmlToBlocks.parse("<h2>Hello</h2>"))
    }

    @Test
    fun `heading level from tag`() {
        assertEquals(listOf(ContentBlock.Heading(3, "Sub")), HtmlToBlocks.parse("<h3>Sub</h3>"))
        assertEquals(listOf(ContentBlock.Heading(5, "Small")), HtmlToBlocks.parse("<h5>Small</h5>"))
    }

    @Test
    fun `plain paragraph`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello world")))),
            HtmlToBlocks.parse("<p>Hello world</p>")
        )
    }

    @Test
    fun `paragraph with bold`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello "), TextSpan.Bold("world")))),
            HtmlToBlocks.parse("<p>Hello <strong>world</strong></p>")
        )
    }

    @Test
    fun `paragraph with italic`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello "), TextSpan.Italic("world")))),
            HtmlToBlocks.parse("<p>Hello <em>world</em></p>")
        )
    }

    @Test
    fun `paragraph with link`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Link("click", "https://example.com")))),
            HtmlToBlocks.parse("""<p><a href="https://example.com">click</a></p>""")
        )
    }

    @Test
    fun `img parsed`() {
        assertEquals(
            listOf(ContentBlock.Image("https://example.com/img.jpg", null)),
            HtmlToBlocks.parse("""<img src="https://example.com/img.jpg">""")
        )
    }

    @Test
    fun `figure with caption`() {
        assertEquals(
            listOf(ContentBlock.Image("https://x.com/a.jpg", "Cap")),
            HtmlToBlocks.parse("""<figure><img src="https://x.com/a.jpg"><figcaption>Cap</figcaption></figure>""")
        )
    }

    @Test
    fun `blockquote parsed`() {
        assertEquals(
            listOf(ContentBlock.Quote("A quote")),
            HtmlToBlocks.parse("<blockquote>A quote</blockquote>")
        )
    }

    @Test
    fun `ul list items become paragraphs with bullet`() {
        assertEquals(
            listOf(
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• Item 1"))),
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• Item 2")))
            ),
            HtmlToBlocks.parse("<ul><li>Item 1</li><li>Item 2</li></ul>")
        )
    }

    @Test
    fun `ol list items become paragraphs with numbers`() {
        assertEquals(
            listOf(
                ContentBlock.Paragraph(listOf(TextSpan.Plain("1. First"))),
                ContentBlock.Paragraph(listOf(TextSpan.Plain("2. Second")))
            ),
            HtmlToBlocks.parse("<ol><li>First</li><li>Second</li></ol>")
        )
    }

    @Test
    fun `iframe is dropped`() {
        assertEquals(
            emptyList<ContentBlock>(),
            HtmlToBlocks.parse("""<iframe src="https://youtube.com/embed/abc"></iframe>""")
        )
    }

    @Test
    fun `form is dropped`() {
        assertEquals(
            emptyList<ContentBlock>(),
            HtmlToBlocks.parse("""<form><input type="email"><button>Subscribe</button></form>""")
        )
    }

    @Test
    fun `blank paragraph filtered out`() {
        assertEquals(emptyList<ContentBlock>(), HtmlToBlocks.parse("<p>   </p>"))
    }

    @Test
    fun `img without src filtered out`() {
        assertEquals(emptyList<ContentBlock>(), HtmlToBlocks.parse("<img>"))
    }
}
```

**Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --tests "*.HtmlToBlocksTest" 2>&1 | tail -10
```

Expected: FAIL with `Unresolved reference: HtmlToBlocks`

**Step 3: Create the parser**

Create `app/src/main/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocks.kt`:

```kotlin
package com.pavel.pavelrssreader.data.parser

import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.model.TextSpan
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlToBlocks {

    fun parse(html: String): List<ContentBlock> =
        Jsoup.parseBodyFragment(html).body().children()
            .flatMap { parseElement(it) }
            .filter { isNonEmpty(it) }

    private fun isNonEmpty(block: ContentBlock): Boolean = when (block) {
        is ContentBlock.Heading -> block.text.isNotBlank()
        is ContentBlock.Paragraph -> block.spans.any { span ->
            when (span) {
                is TextSpan.Plain -> span.text.isNotBlank()
                is TextSpan.Bold -> span.text.isNotBlank()
                is TextSpan.Italic -> span.text.isNotBlank()
                is TextSpan.Link -> span.text.isNotBlank()
            }
        }
        is ContentBlock.Image -> block.url.isNotBlank()
        is ContentBlock.Quote -> block.text.isNotBlank()
    }

    private fun parseElement(element: Element): List<ContentBlock> =
        when (element.tagName().lowercase()) {
            "h2", "h3", "h4", "h5", "h6" ->
                listOf(ContentBlock.Heading(element.tagName()[1].digitToInt(), element.text()))
            "p" -> {
                val spans = parseSpans(element)
                if (spans.isEmpty()) emptyList() else listOf(ContentBlock.Paragraph(spans))
            }
            "blockquote" -> {
                val text = element.text()
                if (text.isBlank()) emptyList() else listOf(ContentBlock.Quote(text))
            }
            "figure" -> {
                val img = element.selectFirst("img") ?: return emptyList()
                val src = img.attr("src").takeIf { it.isNotBlank() } ?: return emptyList()
                val caption = element.selectFirst("figcaption")?.text()?.takeIf { it.isNotBlank() }
                listOf(ContentBlock.Image(src, caption))
            }
            "img" -> {
                val src = element.attr("src").takeIf { it.isNotBlank() } ?: return emptyList()
                listOf(ContentBlock.Image(src, null))
            }
            "ul" -> element.select("> li").map { li ->
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• ${li.text()}")))
            }
            "ol" -> element.select("> li").mapIndexed { i, li ->
                ContentBlock.Paragraph(listOf(TextSpan.Plain("${i + 1}. ${li.text()}")))
            }
            "div", "section", "article" -> element.children().flatMap { parseElement(it) }
            else -> emptyList()
        }

    private fun parseSpans(element: Element): List<TextSpan> =
        element.childNodes().flatMap { parseNode(it) }

    private fun parseNode(node: Node): List<TextSpan> = when {
        node is TextNode -> {
            val text = node.text()
            if (text.isBlank()) emptyList() else listOf(TextSpan.Plain(text))
        }
        node is Element -> when (node.tagName().lowercase()) {
            "strong", "b" -> {
                val text = node.text()
                if (text.isBlank()) emptyList() else listOf(TextSpan.Bold(text))
            }
            "em", "i" -> {
                val text = node.text()
                if (text.isBlank()) emptyList() else listOf(TextSpan.Italic(text))
            }
            "a" -> {
                val href = node.attr("href").takeIf { it.isNotBlank() }
                val text = node.text()
                when {
                    href != null && text.isNotBlank() -> listOf(TextSpan.Link(text, href))
                    text.isNotBlank() -> listOf(TextSpan.Plain(text))
                    else -> emptyList()
                }
            }
            "br" -> listOf(TextSpan.Plain("\n"))
            else -> node.childNodes().flatMap { parseNode(it) }
        }
        else -> emptyList()
    }
}
```

**Step 4: Run tests to verify they pass**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --tests "*.HtmlToBlocksTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with all tests passing

**Step 5: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocks.kt \
        app/src/test/java/com/pavel/pavelrssreader/data/parser/HtmlToBlocksTest.kt
git commit -m "feat: HtmlToBlocks parser — converts article HTML to ContentBlock list"
```

---

### Task 3: Update WebViewViewModel

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewViewModel.kt`

Replace the entire file content:

```kotlin
package com.pavel.pavelrssreader.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.data.network.ArticleContentFetcher
import com.pavel.pavelrssreader.data.parser.HtmlToBlocks
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val contentBlocks: List<ContentBlock> = emptyList(),
    val titleFontSize: Float = SettingsRepository.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepository.DEFAULT_BODY_FONT_SIZE
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val articleContentFetcher: ArticleContentFetcher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _articleId = MutableStateFlow<Long?>(null)
    private val _contentBlocks = MutableStateFlow<List<ContentBlock>>(emptyList())

    private val _articleFlow = _articleId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else getArticlesUseCase().map { articles -> articles.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<WebViewUiState> = combine(
        combine(_articleId, _articleFlow, _contentBlocks) { id, article, blocks ->
            Triple(id, article, blocks)
        },
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize
    ) { (id, article, contentBlocks), titleSize, bodySize ->
        when {
            id == null -> WebViewUiState(titleFontSize = titleSize, bodyFontSize = bodySize)
            article == null -> WebViewUiState(isLoading = true, titleFontSize = titleSize, bodyFontSize = bodySize)
            else -> WebViewUiState(
                article = article,
                isLoading = false,
                contentBlocks = contentBlocks,
                titleFontSize = titleSize,
                bodyFontSize = bodySize
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WebViewUiState(isLoading = true))

    init {
        viewModelScope.launch {
            _articleFlow
                .mapNotNull { it }
                .distinctUntilChanged { old, new -> old.link == new.link }
                .collect { article ->
                    _contentBlocks.value = emptyList()
                    val fetched = articleContentFetcher.fetch(article.link)
                    val html = fetched.ifBlank { article.description ?: "" }
                    _contentBlocks.value = HtmlToBlocks.parse(html)
                }
        }
    }

    fun loadArticle(articleId: Long) {
        if (_articleId.value != articleId) {
            _contentBlocks.value = emptyList()
            _articleId.value = articleId
            viewModelScope.launch { markAsReadUseCase(articleId) }
        }
    }

    fun toggleFavourite() {
        val current = uiState.value.article ?: return
        viewModelScope.launch {
            toggleFavouriteUseCase(current.id, !current.isFavorite)
        }
    }
}
```

**Step 1: Compile to verify**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 2: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewViewModel.kt
git commit -m "feat: WebViewViewModel uses ContentBlock list instead of raw HTML string"
```

---

### Task 4: Replace WebViewScreen with native Compose renderer

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt`
- Delete: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/CoilImageGetter.kt` (unused dead code)

**Step 1: Replace WebViewScreen.kt entirely**

```kotlin
package com.pavel.pavelrssreader.presentation.webview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val article = state.article ?: return@Scaffold
                ArticleContent(
                    article = article,
                    blocks = state.contentBlocks,
                    titleFontSize = state.titleFontSize,
                    bodyFontSize = state.bodyFontSize,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
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
    bodyFontSize: androidx.compose.ui.unit.TextUnit,
    linkColor: androidx.compose.ui.graphics.Color
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
    captionFontSize: androidx.compose.ui.unit.TextUnit
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
    bodyFontSize: androidx.compose.ui.unit.TextUnit
) {
    val borderColor = MaterialTheme.colorScheme.primary
    Row(
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
```

**Step 2: Delete CoilImageGetter.kt (dead code)**

```bash
rm app/src/main/java/com/pavel/pavelrssreader/presentation/webview/CoilImageGetter.kt
```

**Step 3: Build to verify**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug 2>&1 | tail -8
```

Expected: `BUILD SUCCESSFUL`

If you get `Unresolved reference: LinkAnnotation` or `withLink`, the Compose BOM version doesn't include it. In that case replace all `withLink(LinkAnnotation.Url(...)) { append(...) }` with:

```kotlin
pushStringAnnotation("URL", url)
withStyle(SpanStyle(color = linkColor)) { append(text) }
pop()
```

And handle clicks via `ClickableText` instead of `Text`.

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt
git rm app/src/main/java/com/pavel/pavelrssreader/presentation/webview/CoilImageGetter.kt
git commit -m "feat: replace WebView with native Compose article renderer"
```

---

### Task 5: Final build and APK

**Step 1: Run unit tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

**Step 2: Build APK**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug 2>&1 | tail -8
```

Expected: `BUILD SUCCESSFUL`

APK at: `app/build/outputs/apk/debug/app-debug.apk`
