# RSS Reader Android App — Design Document
**Date:** 2026-03-07
**Project:** PavelRssReader
**Status:** Approved

---

## 1. Overview

An Android RSS reader app (Jetpack Compose, minSdk 26) where users add RSS feeds by URL.
Articles are automatically purged after 24 hours unless saved to Favourites.
Favourites persist until the user explicitly removes them.

---

## 2. Architecture

**Pattern:** MVVM + Clean Architecture

```
UI (Compose screens)
    │
    ▼
ViewModels  (StateFlow, one per screen)
    │
    ▼
Use Cases  (domain layer, pure Kotlin)
    │
    ▼
Repository  (single source of truth)
    │            │
    ▼            ▼
Room DB      Retrofit/OkHttp
(local)      (network → XmlPullParser)
```

**Package structure:**
```
com.pavel.pavelrssreader/
├── data/
│   ├── db/           (Room entities, DAOs, AppDatabase)
│   ├── network/      (RssApiService, RssParser)
│   └── repository/   (RssRepositoryImpl)
├── domain/
│   ├── model/        (Feed, Article — plain Kotlin data classes)
│   ├── repository/   (RssRepository interface)
│   └── usecase/      (GetArticlesUseCase, ToggleFavouriteUseCase, etc.)
├── presentation/
│   ├── articles/     (ArticleListScreen, ArticleListViewModel)
│   ├── webview/      (WebViewScreen)
│   ├── favourites/   (FavouritesScreen, FavouritesViewModel)
│   ├── feeds/        (FeedsScreen, FeedsViewModel)
│   └── navigation/   (NavGraph, BottomBar)
└── di/               (Hilt modules)
```

---

## 3. Data Model

### Room Entities

```kotlin
FeedEntity(
    id: Long,          // PK auto-generate
    url: String,       // RSS feed URL
    title: String,     // Feed title (from <channel><title>)
    addedAt: Long      // epoch ms
)

ArticleEntity(
    id: Long,          // PK auto-generate
    feedId: Long,      // FK → FeedEntity
    guid: String,      // unique per feed (for deduplication)
    title: String,
    link: String,
    description: String,
    publishedAt: Long, // epoch ms (from pubDate / updated)
    fetchedAt: Long,   // epoch ms — used for 24h TTL
    isRead: Boolean,
    isFavorite: Boolean
)
```

### Retention Rules

- **24h TTL:** On every feed refresh, after inserting new articles, execute:
  `DELETE FROM articles WHERE is_favorite = 0 AND fetched_at < (now - 86400000)`
- **Favourites:** `isFavorite = true` articles are never auto-deleted.
  Removed only when the user explicitly unfavourites (swipe or button).
- **Deduplication:** `guid` is unique per feed; `INSERT OR IGNORE` prevents duplicates.

---

## 4. Screens & Navigation

**Navigation:** Jetpack Navigation Compose with a bottom navigation bar.

### Bottom Tabs
| Tab | Icon | Screen |
|-----|------|--------|
| Feed | list | ArticleListScreen |
| Favourites | star | FavouritesScreen |
| Feeds | rss | FeedsScreen |

### Screen Descriptions

**ArticleListScreen**
- Merged timeline of all feeds, sorted by `publishedAt` descending
- Pull-to-refresh triggers network fetch for all feeds
- Article card: title, source feed name, relative time, star toggle button
- Tap card → navigate to WebViewScreen

**WebViewScreen**
- Renders `article.link` in an Android WebView
- Top app bar: back button, article title, star/unstar icon button
- Toggling favourite updates Room immediately (optimistic UI)

**FavouritesScreen**
- Same card list filtered to `isFavorite = true`
- Swipe-to-dismiss or button removes from favourites (does NOT delete article if < 24h old)

**FeedsScreen**
- List of added `FeedEntity` items with title + URL
- Swipe-to-delete removes feed and all its non-favourite articles
- FAB opens `AddFeedDialog`:
  - TextField for URL
  - Validate URL format + attempt HEAD/GET to confirm it's a valid RSS endpoint
  - On success: persist feed, fetch articles immediately

---

## 5. Dependencies

```kotlin
// DI
hilt-android, hilt-compiler, hilt-navigation-compose

// Navigation
navigation-compose

// Room
room-runtime, room-ktx, room-compiler (KSP)

// Network
retrofit, converter-scalars, okhttp

// Lifecycle / ViewModel
lifecycle-viewmodel-compose, lifecycle-runtime-ktx

// Coroutines
kotlinx-coroutines-android

// Build plugin
KSP (replaces kapt)
```

---

## 6. Error Handling

| Scenario | Handling |
|----------|----------|
| Network error on refresh | Snackbar: "Couldn't refresh feed. Check your connection." |
| Invalid/non-RSS URL in AddFeedDialog | Inline validation error under TextField |
| Malformed XML from feed | Skip that feed; log warning; other feeds unaffected |
| All async results | Wrapped in `sealed class Result<T>` (Success / Error) |

---

## 7. Key Decisions

- **XmlPullParser** chosen over ROME library — no extra dependency, handles RSS 2.0 + Atom adequately
- **Manual pull-to-refresh** only — no background WorkManager job needed
- **Single merged feed** — simpler UX, no per-feed tabs needed
- **Hilt** for DI — standard Android recommendation, reduces ViewModel boilerplate
