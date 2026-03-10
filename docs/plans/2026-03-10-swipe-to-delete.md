# Swipe-to-Delete Feed Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add swipe-left-to-delete on the Feeds screen — swiping a feed row reveals a red background, the item snaps back, and a confirmation dialog appears before the feed is deleted.

**Architecture:** Pure UI change to `FeedsScreen.kt`. `FeedsViewModel.deleteFeed()` already exists. Uses Material3's `SwipeToDismissBox` (experimental, already opted into in the project). `feedToDelete: Feed?` state hoisted to screen level drives the confirmation dialog.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 `SwipeToDismissBox` (`@ExperimentalMaterial3Api`)

---

### Task 1: Implement swipe-to-delete in FeedsScreen

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt`

---

**Step 1: Add new imports to the existing import block**

Add the following imports (after the existing imports):

```kotlin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
```

Note: `LaunchedEffect` is already imported — don't add it twice. Check before adding.

---

**Step 2: Add `feedToDelete` state at the top of `FeedsScreen`**

Inside `FeedsScreen`, directly below the existing `var urlInput` line, add:

```kotlin
var feedToDelete: Feed? by remember { mutableStateOf(null) }
```

---

**Step 3: Wrap `FeedListItem` with `SwipeToDismissBox` in the `LazyColumn`**

Replace the `items` block inside the `LazyColumn`:

**Before:**
```kotlin
items(state.feeds, key = { it.id }) { feed ->
    FeedListItem(
        feed = feed,
        unreadCount = state.unreadCounts[feed.id] ?: 0
    )
    HorizontalDivider()
}
```

**After:**
```kotlin
items(state.feeds, key = { it.id }) { feed ->
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it != SwipeToDismissBoxValue.StartToEnd }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            feedToDelete = feed
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete feed",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        FeedListItem(
            feed = feed,
            unreadCount = state.unreadCounts[feed.id] ?: 0
        )
    }
    HorizontalDivider()
}
```

---

**Step 4: Show `ConfirmDeleteDialog` when `feedToDelete` is set**

Inside `FeedsScreen`, directly below the existing `if (showDialog)` block and its `LaunchedEffect`, add:

```kotlin
feedToDelete?.let { feed ->
    ConfirmDeleteDialog(
        feedTitle = feed.title,
        onConfirm = {
            viewModel.deleteFeed(feed.id)
            feedToDelete = null
        },
        onDismiss = { feedToDelete = null }
    )
}
```

---

**Step 5: Add `ConfirmDeleteDialog` private composable**

At the bottom of the file (after `AddFeedDialog`), add:

```kotlin
@Composable
private fun ConfirmDeleteDialog(
    feedTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove \"$feedTitle\"?") },
        text = { Text("All articles from this feed will also be removed.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

---

**Step 6: Build to verify**

Run:
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

If you get an `Unresolved reference: Delete` error, the `Icons.Outlined.Delete` icon may not be in the extended icons set. In that case, replace `Icons.Outlined.Delete` with `Icons.Outlined.DeleteOutline` or `Icons.Filled.Delete` — both are available in `material-icons-extended`.

If you get `Unresolved reference: onErrorContainer`, add the `onErrorContainer` mapping to the `LightColorScheme` / `DarkColorScheme` in `ui/theme/Theme.kt`. The `errorContainer` and `onErrorContainer` roles are built into `lightColorScheme()` / `darkColorScheme()` with default values, so they should resolve without explicit tokens.

---

**Step 7: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt
git commit -m "feat: swipe-to-delete feed with confirmation dialog"
```
