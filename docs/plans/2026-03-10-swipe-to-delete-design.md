# Swipe-to-Delete Feed Design — 2026-03-10

## Overview

Add swipe-left-to-delete to the Feeds screen. Swiping a feed row left reveals a red background, snaps back, and opens a confirmation dialog before the feed is deleted.

---

## Interaction Flow

1. User swipes a feed row left past the dismiss threshold
2. Red `errorContainer` background with a trash icon is visible during the swipe
3. On release past threshold: item snaps back to original position; confirmation dialog opens
4. Dialog title: **"Remove [feed title]?"**
5. Dialog body: **"All articles from this feed will also be removed."**
6. **Remove** → `viewModel.deleteFeed(feedId)`
7. **Cancel** → dialog closes, item stays

---

## Visual Design

- Swipe direction: `endToStart` only (left swipe)
- Background color: `MaterialTheme.colorScheme.errorContainer`
- Background icon: `Icons.Outlined.Delete`, tint `MaterialTheme.colorScheme.onErrorContainer`
- Icon aligned to the right edge with 24dp horizontal padding

---

## Implementation Scope

Only `FeedsScreen.kt` is modified. No ViewModel, domain, or DB changes needed.

### Changes

- Wrap each `FeedListItem` in `SwipeToDismissBox` (endToStart direction only) inside the `LazyColumn`
- Each item gets its own `rememberSwipeToDismissBoxState()`
- A `LaunchedEffect` per item detects `dismissState.currentValue == SwipeToDismissBoxValue.EndToStart`, stores `feedToDelete: Feed?` at screen level, then calls `dismissState.reset()` to snap the item back
- A `ConfirmDeleteDialog` private composable shown at screen level when `feedToDelete != null`
- On confirm: call `viewModel.deleteFeed(feedToDelete.id)`, set `feedToDelete = null`
- On cancel: set `feedToDelete = null`

### New state at `FeedsScreen` level

```kotlin
var feedToDelete: Feed? by remember { mutableStateOf(null) }
```

### `ConfirmDeleteDialog` signature

```kotlin
@Composable
private fun ConfirmDeleteDialog(
    feedTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
)
```
