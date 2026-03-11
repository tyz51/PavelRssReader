# Source Filter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a filter-by-source dropdown to the News screen so users can show articles from a single feed, with "All" as the default.

**Architecture:** A `FilterList` icon in the `TopAppBar` opens a `DropdownMenu` with radio-button items (All + one per feed). Filtering is pure in-memory inside `ArticleListViewModel` via a `combine` of the articles flow, feeds flow, and a `_selectedFeedId` state flow — no new DB queries needed.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (`DropdownMenu`, `DropdownMenuItem`, `RadioButton`), Coroutines `combine`, MockK, Turbine.

---

### Task 1: Update `ArticleListUiState` and `ArticleListViewModel`

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModel.kt`

**Context:**
Current `ArticleListUiState` has `articles`, `isRefreshing`, `errorMessage`.
Current `init` block uses a single `getArticlesUseCase()` flow with `.onEach`.
`GetFeedsUseCase` already exists at `domain/usecase/GetFeedsUseCase.kt` — returns `Flow<List<Feed>>`.
`Feed` domain model has fields: `id: Long`, `url: String`, `title: String`, `addedAt: Long`.
`Article` domain model has field `feedId: Long`.

**Step 1: Replace the entire file with the updated version**

```kotlin
package com.pavel.pavelrssreader.presentation.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.RefreshFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val feeds: List<Feed> = emptyList(),
    val selectedFeedId: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val getFeedsUseCase: GetFeedsUseCase,
    private val refreshFeedsUseCase: RefreshFeedsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private val _selectedFeedId = MutableStateFlow<Long?>(null)

    init {
        combine(
            getArticlesUseCase(),
            getFeedsUseCase(),
            _selectedFeedId
        ) { articles, feeds, selectedFeedId ->
            val unread = articles.filter { !it.isRead }
            val filtered = if (selectedFeedId == null) unread
                           else unread.filter { it.feedId == selectedFeedId }
            Triple(filtered, feeds, selectedFeedId)
        }
        .onEach { (articles, feeds, selectedFeedId) ->
            _uiState.update { it.copy(
                articles = articles,
                feeds = feeds,
                selectedFeedId = selectedFeedId
            )}
        }
        .launchIn(viewModelScope)
    }

    fun selectFeed(feedId: Long?) {
        _selectedFeedId.value = feedId
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshFeedsUseCase()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    errorMessage = (result as? Result.Error)?.message
                )
            }
        }
    }

    fun toggleFavourite(articleId: Long, isFavorite: Boolean) {
        viewModelScope.launch { toggleFavouriteUseCase(articleId, isFavorite) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

**Step 2: Build to verify it compiles**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Update `ArticleListViewModelTest`

**Files:**
- Modify: `app/src/test/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModelTest.kt`

**Context:**
The test constructs `ArticleListViewModel` directly (not via Hilt). Every call site must be updated to pass `getFeedsUseCase` as the second argument. Two new tests cover `selectFeed`.

**Step 1: Write the two new failing tests**

Open `ArticleListViewModelTest.kt` and:

1. Add `getFeedsUseCase` mock at the top of the class:
```kotlin
private val getFeedsUseCase = mockk<GetFeedsUseCase>()
```

2. Add `sampleFeed`:
```kotlin
private val sampleFeed = Feed(id = 1L, url = "https://example.com/rss", title = "Example", addedAt = 0L)
```

3. Add the import at the top:
```kotlin
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import org.junit.Assert.assertEquals
```

4. In every existing test, change the constructor call from:
```kotlin
ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
```
to:
```kotlin
ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
```
and add `every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))` to each test's setup.

5. Add two new tests at the bottom:

```kotlin
@Test
fun `selectFeed filters articles to chosen feedId`() = runTest {
    val article1 = sampleArticle.copy(id = 1L, feedId = 1L)
    val article2 = sampleArticle.copy(id = 2L, feedId = 2L)
    every { getArticlesUseCase() } returns flowOf(listOf(article1, article2))
    every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

    val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
    advanceUntilIdle()
    vm.selectFeed(1L)
    advanceUntilIdle()

    vm.uiState.test {
        val state = awaitItem()
        assertEquals(1, state.articles.size)
        assertEquals(1L, state.articles.first().feedId)
        cancelAndIgnoreRemainingEvents()
    }
}

@Test
fun `selectFeed null restores all articles`() = runTest {
    val article1 = sampleArticle.copy(id = 1L, feedId = 1L)
    val article2 = sampleArticle.copy(id = 2L, feedId = 2L)
    every { getArticlesUseCase() } returns flowOf(listOf(article1, article2))
    every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

    val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
    vm.selectFeed(1L)
    advanceUntilIdle()
    vm.selectFeed(null)
    advanceUntilIdle()

    vm.uiState.test {
        val state = awaitItem()
        assertEquals(2, state.articles.size)
        cancelAndIgnoreRemainingEvents()
    }
}
```

**Step 2: Run the new tests to verify they fail (before ViewModel is done)**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --tests "*.ArticleListViewModelTest" 2>&1 | tail -20`

Expected: FAIL (compile error because ViewModel constructor has changed)

**Step 3: Run all tests after the ViewModel change from Task 1 is in place**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --tests "*.ArticleListViewModelTest" 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModel.kt \
        app/src/test/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModelTest.kt
git commit -m "feat: add source filter state and selectFeed() to ArticleListViewModel"
```

---

### Task 3: Update `ArticleListScreen` with the dropdown UI

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt`

**Context:**
The `TopAppBar` currently has one action: a Search `IconButton`. We add a `FilterList` icon button to its left that opens a `DropdownMenu`.

The `DropdownMenu` must:
- Anchor to the filter icon button (place both inside a `Box`)
- Show "All" as the first item, then one item per `state.feeds`
- Show a `RadioButton` as the leading icon for each item
- Close on any selection and call `viewModel.selectFeed()`
- Tint the `FilterList` icon `MaterialTheme.colorScheme.primary` when `state.selectedFeedId != null`

**Step 1: Replace the entire file**

```kotlin
package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.news_title)) },
                actions = {
                    // Source filter dropdown
                    var filterMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter by source",
                                tint = if (state.selectedFeedId != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    LocalContentColor.current
                            )
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    viewModel.selectFeed(null)
                                    filterMenuExpanded = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = state.selectedFeedId == null,
                                        onClick = null
                                    )
                                }
                            )
                            state.feeds.forEach { feed ->
                                DropdownMenuItem(
                                    text = { Text(feed.title) },
                                    onClick = {
                                        viewModel.selectFeed(feed.id)
                                        filterMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = state.selectedFeedId == feed.id,
                                            onClick = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                    // Search (existing placeholder)
                    IconButton(onClick = { /* search placeholder */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.articles.isEmpty() && !state.isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No articles. Add a feed and pull to refresh.")
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            ) {
                LazyColumn {
                    items(state.articles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onArticleClick(article.id) },
                            onToggleFavourite = { isFav ->
                                viewModel.toggleFavourite(article.id, isFav)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
```

**Step 2: Build and run all tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 3: Build the APK**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt
git commit -m "feat: add source filter dropdown to News screen top bar"
```
