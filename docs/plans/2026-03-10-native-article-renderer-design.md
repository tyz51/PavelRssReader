# Native Article Renderer Design — 2026-03-10

## Goal

Replace the `WebView`-based article reader with a native Compose renderer that shows only text and images. Iframes, social embeds, subscribe buttons, and any other parasitic elements are structurally impossible to render — if the parser doesn't know the node type, it drops it silently.

---

## Content Block Model

A sealed class hierarchy representing every node type the renderer supports. Lives in `domain/model/ContentBlock.kt`.

```kotlin
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

- `Heading`: h2–h6 (h1 is already shown as the article title above the content)
- `Paragraph`: p tag; child nodes walk to build `TextSpan` list
- `Image`: img src + optional figcaption text
- `Quote`: blockquote text
- `ul`/`ol` list items are rendered as `Paragraph` with bullet/number prefix

---

## HTML Parser

New class: `data/parser/HtmlToBlocks.kt`

Pure function: `fun parse(html: String): List<ContentBlock>`

- Uses Jsoup (already a dependency) to walk the element tree
- Maps supported elements to `ContentBlock`s
- Everything else (iframe, form, script, social embeds, subscribe widgets) is silently dropped
- No Android dependencies — unit testable in isolation

### Inline span parsing (for Paragraph)
Walk Jsoup child nodes recursively:
- `#text` → `TextSpan.Plain`
- `strong`, `b` → `TextSpan.Bold`
- `em`, `i` → `TextSpan.Italic`
- `a[href]` → `TextSpan.Link`
- Other elements → recurse into children, flatten as Plain

---

## ViewModel & State

`WebViewUiState` replaces `fullContent: String?` with `contentBlocks: List<ContentBlock>`:

```kotlin
data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val contentBlocks: List<ContentBlock> = emptyList(),
    val titleFontSize: Float = SettingsRepository.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepository.DEFAULT_BODY_FONT_SIZE
)
```

In `WebViewViewModel.init`:
- After `ArticleContentFetcher.fetch()` returns cleaned HTML → call `HtmlToBlocks.parse()` → store blocks
- If fetch returns blank → parse `article.description` as fallback via the same `HtmlToBlocks.parse()`
- Raw HTML string never reaches the UI layer

---

## Compose Renderer

`WebViewScreen.kt`: the `AndroidView(WebView)` block is replaced with a `LazyColumn`.

Each `ContentBlock` maps to a composable item:

| Block | Composable |
|-------|-----------|
| `Heading(2)` | `Text`, `titleLarge`, `onSurface` |
| `Heading(3–4)` | `Text`, `titleMedium`, `onSurface` |
| `Heading(5–6)` | `Text`, `titleSmall`, `onSurface` |
| `Paragraph` | `Text` with `AnnotatedString` (bold/italic/link spans) |
| `Image` | `AsyncImage` (Coil), full width, caption in `onSurfaceVariant` |
| `Quote` | 4dp left border (`primary`), italic `Text`, indented |

- Link spans use `primary` color; tap opens system browser via `Intent.ACTION_VIEW`
- Font sizes (title/body) from `settingsState` sliders — same as before
- Dark/light theme automatic via `MaterialTheme.colorScheme` — no CSS needed
- `WebView`, CSS string-building, `setBackgroundColor()`, `javaScriptEnabled` all removed

---

## Files Changed

| Action | File |
|--------|------|
| Create | `domain/model/ContentBlock.kt` |
| Create | `data/parser/HtmlToBlocks.kt` |
| Modify | `presentation/webview/WebViewViewModel.kt` |
| Modify | `presentation/webview/WebViewScreen.kt` |
