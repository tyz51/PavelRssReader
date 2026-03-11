# Swipe-to-Read Design

**Goal:** Let users swipe an article card left on the News screen to mark it as read, with haptic feedback and an undo Snackbar.

## UI

A left swipe (`endToStart`) on any `ArticleCard` in the `LazyColumn`:

1. Reveals a `MaterialTheme.colorScheme.primaryContainer` background with a `Icons.Default.DoneAll` icon aligned to the right edge.
2. At full threshold, the card dismisses with the standard `SwipeToDismissBox` animation.
3. A `HapticFeedbackType.LongPress` fires at the moment of confirmation.
4. A Snackbar "Marked as read" appears with an **Undo** action (short duration).

## Data Flow

```
swipe endToStart confirmed
  → viewModel.dismissArticle(articleId)   // hides from list in-memory
  → haptic.performHapticFeedback(LongPress)
  → snackbarHostState.showSnackbar("Marked as read", actionLabel = "Undo", Short)
      SnackbarResult.ActionPerformed → viewModel.undoDismiss(articleId)
      SnackbarResult.Dismissed       → viewModel.confirmDismiss(articleId)
```

## State

`ArticleListViewModel` gains:
```kotlin
private val _hiddenArticleIds = MutableStateFlow<Set<Long>>(emptySet())
```

The existing `combine(articles, feeds, _selectedFeedId)` is extended to a 4-way combine that also filters out `_hiddenArticleIds`:
```kotlin
combine(getArticlesUseCase(), getFeedsUseCase(), _selectedFeedId, _hiddenArticleIds)
{ articles, feeds, selectedFeedId, hiddenIds ->
    val unread = articles.filter { !it.isRead && it.id !in hiddenIds }
    ...
}
```

## ViewModel Methods

```kotlin
fun dismissArticle(articleId: Long) {
    _hiddenArticleIds.update { it + articleId }
}

fun undoDismiss(articleId: Long) {
    _hiddenArticleIds.update { it - articleId }
}

fun confirmDismiss(articleId: Long) {
    _hiddenArticleIds.update { it - articleId }
    viewModelScope.launch { markAsReadUseCase(articleId) }
}
```

`MarkAsReadUseCase` is added as a new constructor dependency (it already exists at `domain/usecase/MarkAsReadUseCase.kt`).

## Screen Changes

In `ArticleListScreen`, each `ArticleCard` is wrapped in a `SwipeToDismissBox`:

```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
)
LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.dismissArticle(article.id)
        val result = snackbarHostState.showSnackbar(
            message = "Marked as read",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDismiss(article.id)
        else viewModel.confirmDismiss(article.id)
    }
}
SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    backgroundContent = { /* primaryContainer + DoneAll icon */ }
) {
    ArticleCard(...)
}
```

## Architecture Changes

| File | Change |
|------|--------|
| `ArticleListViewModel.kt` | + `MarkAsReadUseCase` dep; + `_hiddenArticleIds`; extend combine; + 3 methods |
| `ArticleListScreen.kt` | Wrap cards in `SwipeToDismissBox`; haptic + Snackbar handling |

No new DB queries, no new use cases, no navigation changes.
