# Article Text View Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the WebView article reader with a native `TextView` that renders the RSS `description` HTML including inline images loaded via Coil.

**Architecture:** `Html.fromHtml()` parses the HTML string into a `Spanned`; a custom `CoilImageGetter` (implementing `Html.ImageGetter`) loads each `<img src>` asynchronously with Coil and triggers a TextView re-layout when each image arrives. The `TextView` lives inside a `Column` with `verticalScroll` so the full article is scrollable.

**Tech Stack:** Kotlin, Jetpack Compose, `android.text.Html`, Coil 2.7.0, `AndroidView(TextView)`

---

### Task 1: Add Coil dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Step 1: Add Coil version and library entry to `libs.versions.toml`**

In the `[versions]` block, add after `okhttp`:
```toml
coil = "2.7.0"
```

In the `[libraries]` block, add after `okhttp`:
```toml
coil = { group = "io.coil-kt", name = "coil", version.ref = "coil" }
```

**Step 2: Add Coil to `app/build.gradle.kts`**

In the `// Network` section, add:
```kotlin
implementation(libs.coil)
```

**Step 3: Sync and verify the build compiles**

Run:
```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Create `CoilImageGetter`

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/CoilImageGetter.kt`

**Step 1: Create the file with this exact content**

```kotlin
package com.pavel.pavelrssreader.presentation.webview

import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.text.Html
import android.widget.TextView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.Target

/**
 * Html.ImageGetter that loads <img> tags asynchronously using Coil.
 * After each image loads it triggers a TextView re-layout by reassigning textView.text.
 */
class CoilImageGetter(
    private val textView: TextView,
    private val imageLoader: ImageLoader,
    private val baseUrl: String
) : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        val resolvedUrl = resolveUrl(source)
        val container = LevelListDrawable()
        container.setBounds(0, 0, 1, 1) // tiny placeholder so parsing doesn't stall

        val request = ImageRequest.Builder(textView.context)
            .data(resolvedUrl)
            .target(object : Target {
                override fun onSuccess(result: Drawable) {
                    val availableWidth = textView.width
                        .takeIf { it > 0 }
                        ?: (textView.context.resources.displayMetrics.widthPixels - 64)

                    val scaledWidth: Int
                    val scaledHeight: Int
                    if (result.intrinsicWidth > 0 && result.intrinsicHeight > 0) {
                        scaledWidth = minOf(availableWidth, result.intrinsicWidth)
                        scaledHeight = (scaledWidth * result.intrinsicHeight.toFloat() /
                                result.intrinsicWidth).toInt()
                    } else {
                        scaledWidth = availableWidth
                        scaledHeight = availableWidth / 2
                    }

                    result.setBounds(0, 0, scaledWidth, scaledHeight)
                    container.addLevel(1, 1, result)
                    container.level = 1
                    container.setBounds(0, 0, scaledWidth, scaledHeight)

                    // Reassigning text forces the TextView to re-measure with the new drawable
                    textView.text = textView.text
                }
            })
            .build()

        imageLoader.enqueue(request)
        return container
    }

    private fun resolveUrl(source: String): String {
        if (source.startsWith("http://") || source.startsWith("https://")) return source
        if (source.startsWith("//")) return "https:$source"
        return try {
            java.net.URL(java.net.URL(baseUrl), source).toString()
        } catch (_: Exception) {
            source
        }
    }
}
```

**Step 2: Verify it compiles**

Run:
```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL` (no errors in `CoilImageGetter.kt`)

---

### Task 3: Replace WebView with TextView in `WebViewScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt`

**What to do:** Replace the entire file with the content below. Key changes:
- Remove `buildReaderHtml` function
- Remove `AndroidView(WebView)` block
- Add `Column` with `verticalScroll` containing `AndroidView(TextView)`
- `loadedUrl` renamed `loadedArticleId` and typed `Long?` to guard re-rendering

```kotlin
package com.pavel.pavelrssreader.presentation.webview

import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                title = { Text(state.article?.title ?: "") },
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
        var loadedArticleId by remember { mutableStateOf<Long?>(null) }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.widget.TextView(ctx).apply {
                                textSize = 17f
                                setLineSpacing(0f, 1.5f)
                                setPadding(48, 32, 48, 64)
                                movementMethod = LinkMovementMethod.getInstance()
                                isClickable = true
                                isFocusable = true
                                setTextIsSelectable(true)
                            }
                        },
                        update = { textView ->
                            val article = state.article
                            if (article != null && article.id != loadedArticleId) {
                                loadedArticleId = article.id
                                val imageGetter = CoilImageGetter(
                                    textView = textView,
                                    imageLoader = Coil.imageLoader(textView.context),
                                    baseUrl = article.link
                                )
                                textView.text = Html.fromHtml(
                                    article.description,
                                    Html.FROM_HTML_MODE_COMPACT,
                                    imageGetter,
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
```

**Step 2: Verify it compiles**

Run:
```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Run all tests and full build

**Step 1: Run unit tests**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` — all tests pass (no logic was changed)

**Step 2: Build debug APK**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

## Manual Verification Checklist

After installing the APK:
1. Open any article — should show full text with paragraphs, bold, italic
2. Tap a link — should open in system browser (not navigate within the view)
3. Articles with images — images should appear inline, scaled to screen width
4. Scroll — full article scrolls vertically
5. Long articles — no truncation
