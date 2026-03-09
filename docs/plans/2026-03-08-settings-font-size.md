# Settings Font Size Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a persistent Settings tab where the user sets title and body font sizes via sliders with a live preview; sizes survive app restarts via DataStore.

**Architecture:** `SettingsRepository` wraps `DataStore<Preferences>` and exposes two `Flow<Float>`. A `SettingsViewModel` collects those flows for `SettingsScreen`. `WebViewViewModel` also injects `SettingsRepository` so `WebViewScreen` can drop its local font size state and the A−/A+ buttons.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, DataStore Preferences (`androidx.datastore:datastore-preferences:1.1.1`)

---

### Task 1: Add DataStore dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Step 1: Add version + library entry to `libs.versions.toml`**

In `[versions]` add after `coil`:
```toml
datastore = "1.1.1"
```

In `[libraries]` add after `coil`:
```toml
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

**Step 2: Add to `app/build.gradle.kts`**

In the `// Network` section add:
```kotlin
implementation(libs.datastore.preferences)
```

**Step 3: Sync and verify**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Create SettingsRepository

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/repository/SettingsRepository.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/repository/SettingsRepositoryImpl.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/di/DataStoreModule.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/di/RepositoryModule.kt`

**Step 1: Create the domain interface**

```kotlin
package com.pavel.pavelrssreader.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val titleFontSize: Flow<Float>
    val bodyFontSize: Flow<Float>
    suspend fun setTitleFontSize(sp: Float)
    suspend fun setBodyFontSize(sp: Float)
}
```

**Step 2: Create the DataStore implementation**

```kotlin
package com.pavel.pavelrssreader.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val titleFontSize: Flow<Float> =
        dataStore.data.map { it[KEY_TITLE_FONT_SIZE] ?: DEFAULT_TITLE_FONT_SIZE }

    override val bodyFontSize: Flow<Float> =
        dataStore.data.map { it[KEY_BODY_FONT_SIZE] ?: DEFAULT_BODY_FONT_SIZE }

    override suspend fun setTitleFontSize(sp: Float) {
        dataStore.edit { it[KEY_TITLE_FONT_SIZE] = sp }
    }

    override suspend fun setBodyFontSize(sp: Float) {
        dataStore.edit { it[KEY_BODY_FONT_SIZE] = sp }
    }

    companion object {
        private val KEY_TITLE_FONT_SIZE = floatPreferencesKey("title_font_size")
        private val KEY_BODY_FONT_SIZE = floatPreferencesKey("body_font_size")
        const val DEFAULT_TITLE_FONT_SIZE = 14f
        const val DEFAULT_BODY_FONT_SIZE = 17f
    }
}
```

**Step 3: Create the Hilt DataStore provider**

```kotlin
package com.pavel.pavelrssreader.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
```

**Step 4: Bind SettingsRepository in RepositoryModule**

Add to the existing abstract class in `RepositoryModule.kt`:
```kotlin
@Binds
@Singleton
abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
```

Also add the required imports:
```kotlin
import com.pavel.pavelrssreader.data.repository.SettingsRepositoryImpl
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
```

**Step 5: Verify it compiles**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Create SettingsViewModel and SettingsScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsScreen.kt`

**Step 1: Create SettingsViewModel**

```kotlin
package com.pavel.pavelrssreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val titleFontSize: Float = 14f,
    val bodyFontSize: Float = 17f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize
    ) { title, body -> SettingsUiState(title, body) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTitleFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setTitleFontSize(sp) }
    }

    fun setBodyFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setBodyFontSize(sp) }
    }
}
```

**Step 2: Create SettingsScreen**

```kotlin
package com.pavel.pavelrssreader.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            FontSizeSection(
                label = "Article title",
                currentSize = state.titleFontSize,
                min = 10f,
                max = 22f,
                previewText = "Article title example",
                onSizeChange = viewModel::setTitleFontSize
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            FontSizeSection(
                label = "Article body",
                currentSize = state.bodyFontSize,
                min = 12f,
                max = 28f,
                previewText = "This is how the article body text will look at this size. " +
                        "Adjust until reading feels comfortable.",
                onSizeChange = viewModel::setBodyFontSize
            )
        }
    }
}

@Composable
private fun FontSizeSection(
    label: String,
    currentSize: Float,
    min: Float,
    max: Float,
    previewText: String,
    onSizeChange: (Float) -> Unit
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = previewText,
        style = TextStyle(fontSize = currentSize.sp),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Slider(
        value = currentSize,
        onValueChange = { onSizeChange(it.roundToInt().toFloat()) },
        valueRange = min..max,
        steps = (max - min).toInt() - 1,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        "${currentSize.roundToInt()} sp",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

**Step 3: Verify it compiles**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Wire Settings into navigation

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavGraph.kt`

**Step 1: Add Settings route to NavRoutes.kt**

Add the new data object and update `bottomNavItems`:
```kotlin
import androidx.compose.material.icons.filled.Settings   // add this import

data object Settings : NavRoutes("settings", "Settings", Icons.Default.Settings)

val bottomNavItems = listOf(NavRoutes.Articles, NavRoutes.Favourites, NavRoutes.Feeds, NavRoutes.Settings)
```

**Step 2: Add Settings composable to NavGraph.kt**

Add import and composable:
```kotlin
import com.pavel.pavelrssreader.presentation.settings.SettingsScreen

// inside NavHost:
composable(NavRoutes.Settings.route) {
    SettingsScreen()
}
```

**Step 3: Verify it compiles**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Wire font sizes into WebViewViewModel and WebViewScreen

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewViewModel.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt`

**Step 1: Inject SettingsRepository into WebViewViewModel**

Add `settingsRepository: SettingsRepository` to the constructor and expose both sizes in `uiState`. Replace `WebViewUiState` and `uiState` with:

```kotlin
import com.pavel.pavelrssreader.data.repository.SettingsRepositoryImpl
import com.pavel.pavelrssreader.domain.repository.SettingsRepository

data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val fullContent: String? = null,
    val titleFontSize: Float = SettingsRepositoryImpl.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepositoryImpl.DEFAULT_BODY_FONT_SIZE
)

// In the constructor:
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val articleContentFetcher: ArticleContentFetcher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
```

Replace the `uiState` `combine` to include both font sizes (4 flows → use nested combine):

```kotlin
val uiState: StateFlow<WebViewUiState> = combine(
    combine(_articleId, _articleFlow, _fullContent) { id, article, content ->
        Triple(id, article, content)
    },
    settingsRepository.titleFontSize,
    settingsRepository.bodyFontSize
) { (id, article, fullContent), titleSize, bodySize ->
    when {
        id == null -> WebViewUiState(titleFontSize = titleSize, bodyFontSize = bodySize)
        article == null -> WebViewUiState(isLoading = true, titleFontSize = titleSize, bodyFontSize = bodySize)
        else -> WebViewUiState(
            article = article,
            isLoading = false,
            fullContent = fullContent as String?,
            titleFontSize = titleSize,
            bodyFontSize = bodySize
        )
    }
}.stateIn(viewModelScope, SharingStarted.Eagerly, WebViewUiState(isLoading = true))
```

**Step 2: Update WebViewScreen**

Remove the local font size state and A−/A+ buttons. Replace the entire file with:

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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
                title = {
                    Text(
                        text = state.article?.title ?: "",
                        style = TextStyle(fontSize = state.titleFontSize.sp)
                    )
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
                        AndroidView(
                            factory = { ctx ->
                                android.widget.TextView(ctx).apply {
                                    val density = ctx.resources.displayMetrics.density
                                    setLineSpacing(0f, 1.5f)
                                    setPadding(
                                        (48 * density).toInt(),
                                        (32 * density).toInt(),
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
                                if (article != null && textView.tag != article.id) {
                                    textView.tag = article.id
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
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
```

**Step 3: Build and verify**

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 6: Run all tests

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` — all existing tests pass (no logic was changed)
