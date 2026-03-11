# Source Filter Design

**Goal:** Let users filter the News screen to show articles from a single feed, with "All" as the default.

## UI

A `FilterList` icon button is added to the `TopAppBar` actions (left of the existing search icon).
Tapping it opens a `DropdownMenu` anchored to that button.

Menu contents:
- "All" radio item — selected by default
- One radio item per feed (feed title, alphabetical order)

Selecting any item closes the menu and immediately filters the list.
When a specific source is active the filter icon is tinted with `MaterialTheme.colorScheme.primary`
(vs `LocalContentColor` when "All" is active) so the user can tell a filter is applied at a glance.

## Data flow

Filtering happens in-memory in the ViewModel — no new DB queries.

`ArticleListViewModel` combines three flows:
1. `getArticlesUseCase()` — all unread articles
2. `getFeedsUseCase()` — all feeds (for menu items)
3. `_selectedFeedId: MutableStateFlow<Long?>` — `null` means "All"

When `selectedFeedId` is non-null the combined flow filters articles whose `feedId` matches.

## State

`ArticleListUiState` gains two fields:
```kotlin
val feeds: List<Feed> = emptyList()
val selectedFeedId: Long? = null
```

## ViewModel

New method:
```kotlin
fun selectFeed(feedId: Long?) { _selectedFeedId.value = feedId }
```

## Architecture changes

| File | Change |
|------|--------|
| `ArticleListUiState` | + `feeds`, `selectedFeedId` |
| `ArticleListViewModel` | + `_selectedFeedId` flow, `selectFeed()`, combine with feeds |
| `ArticleListScreen` | + filter `IconButton` + `DropdownMenu` |

No new use cases, no new DB queries, no navigation changes.
