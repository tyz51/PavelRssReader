# iOS RSS Reader — Design Document

**Date:** 2026-03-25
**Status:** Approved
**Target:** iOS app in `~/Documents/Ios_Dev/PavelRssReader-iOS`

---

## Overview

A full feature-parity iOS port of the Android PavelRssReader app. Implemented in SwiftUI (iOS 17+), MVVM + Clean Architecture, with a fully Swift-native dependency stack (SwiftData, URLSession, AppStorage). The only third-party dependency is **SwiftSoup** (Swift port of Jsoup) for HTML parsing.

---

## Architecture

**MVVM + Clean Architecture** — three distinct layers, mirroring the Android project.

```
Domain Layer       → pure Swift structs/enums/protocols, zero Apple-framework imports
Data Layer         → SwiftData models, URLSession networking, SwiftSoup HTML parsing
Presentation Layer → SwiftUI Views + @Observable ViewModels
```

No DI framework. Dependencies injected via initializers and `@Environment`.

---

## Project Structure

```
PavelRssReader-iOS/
├── PavelRssReaderApp.swift
├── domain/
│   ├── model/          Article.swift, Feed.swift, ContentBlock.swift,
│   │                   FeedUnreadCount.swift, ThemePreference.swift, AppError.swift
│   ├── repository/     RssRepositoryProtocol.swift, SettingsRepositoryProtocol.swift
│   └── usecase/        GetArticlesUseCase.swift, GetFavouritesUseCase.swift,
│                       GetFeedsUseCase.swift, GetUnreadCountsPerFeedUseCase.swift,
│                       AddFeedUseCase.swift, DeleteFeedUseCase.swift,
│                       RefreshFeedsUseCase.swift, ToggleFavouriteUseCase.swift,
│                       MarkAsReadUseCase.swift, UnmarkAsReadUseCase.swift
├── data/
│   ├── db/             ArticleModel.swift (@Model), FeedModel.swift (@Model), Mappers.swift
│   ├── network/        RssNetworkService.swift, RssParser.swift, ArticleContentFetcher.swift
│   ├── parser/         HtmlToBlocks.swift
│   └── repository/     RssRepositoryImpl.swift, SettingsRepositoryImpl.swift
├── presentation/
│   ├── articles/       ArticleListView.swift, ArticleListViewModel.swift, ArticleCard.swift
│   ├── favourites/     FavouritesView.swift, FavouritesViewModel.swift
│   ├── feeds/          FeedsView.swift, FeedsViewModel.swift
│   ├── settings/       SettingsView.swift, SettingsViewModel.swift, FontSizeView.swift
│   ├── reader/         ArticleReaderView.swift, ArticleReaderViewModel.swift
│   └── navigation/     RootTabView.swift, AppRouter.swift
└── ui/
    └── theme/          Colors.swift, Typography.swift
```

---

## Data Layer

### SwiftData Models

**`FeedModel`** (`@Model`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| url | String | RSS feed URL |
| title | String | Parsed from feed XML |
| addedAt | Date | |
| articles | [ArticleModel] | Cascade delete relationship |

**`ArticleModel`** (`@Model`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| feedId | UUID | Foreign key |
| guid | String | Unique per (feedId, guid) |
| title | String | |
| link | String | Original article URL |
| description | String | Raw HTML body/summary |
| publishedAt | Date | |
| fetchedAt | Date | |
| isRead | Bool | |
| isFavorite | Bool | |
| imageUrl | String? | Nullable |
| sourceName | String | Feed display name |

**TTL Rule:** On every refresh, delete articles where `isFavorite == false && fetchedAt < Date.now - 86400s`.

### Networking

- `RssNetworkService` — `async throws` methods using `URLSession.shared`
- `RssParser` — Apple `XMLParser` (`XMLParserDelegate`) supporting RSS 2.0 and Atom feeds
- `ArticleContentFetcher` — `URLSession` fetch + `SwiftSoup` HTML scraping. Extracts article body via CSS selector priority list (`article`, `[itemprop=articleBody]`, `[class*=article-body]`, etc.)
- `HtmlToBlocks` — converts HTML `String` → `[ContentBlock]` using `SwiftSoup`

### Settings

`SettingsRepositoryImpl` wraps `UserDefaults` (via `@AppStorage` keys):
- `titleFontSize: Float` (default 14)
- `bodyFontSize: Float` (default 17)
- `themePreference: String` (SYSTEM / LIGHT / DARK)

---

## Domain Layer

### Models

| Model | Key Fields |
|---|---|
| `Article` | id, feedId, guid, title, link, description, publishedAt, fetchedAt, isRead, isFavorite, imageUrl, sourceName |
| `Feed` | id, url, title, addedAt |
| `FeedUnreadCount` | feedId, count |
| `ThemePreference` | enum: system, light, dark |
| `ContentBlock` | enum: heading(level, text), paragraph(spans), image(url, caption), quote(text) |
| `TextSpan` | enum: plain, bold, italic, link |
| `AppError` | enum: networkError, parseError, databaseError |

### Use Cases (10)

| Use Case | Operation |
|---|---|
| `GetArticlesUseCase` | Fetch all unread articles sorted by publishedAt desc |
| `GetFavouritesUseCase` | Fetch all favourite articles |
| `GetFeedsUseCase` | Fetch all feeds |
| `GetUnreadCountsPerFeedUseCase` | Unread article count grouped by feedId |
| `AddFeedUseCase` | Fetch + parse + persist feed and articles |
| `DeleteFeedUseCase` | Delete feed (cascade deletes articles) |
| `RefreshFeedsUseCase` | Re-fetch all feeds, apply TTL pruning |
| `ToggleFavouriteUseCase` | Toggle isFavorite flag |
| `MarkAsReadUseCase` | Set isRead = true |
| `UnmarkAsReadUseCase` | Set isRead = false |

---

## Presentation Layer

### Navigation

`RootTabView` hosts a `TabView` with 4 tabs. Each tab wraps its root view in a `NavigationStack`.

| Tab | Icon | Root View |
|---|---|---|
| News | newspaper | ArticleListView |
| Favorites | star | FavouritesView |
| Feeds | antenna.radiowaves.left.and.right | FeedsView |
| Settings | gearshape | SettingsView |

Article reader (`ArticleReaderView`) and font size screen (`FontSizeView`) are pushed onto their respective `NavigationStack`s — tab bar remains hidden on those screens via `.toolbar(.hidden, for: .tabBar)`.

### ViewModels

All ViewModels use `@Observable` (iOS 17). Passed to views via `@State` at root and `@Bindable` or direct property access in child views.

**`ArticleListViewModel`**
- State: `articles: [Article]`, `feeds: [Feed]`, `selectedFeedId: UUID?`, `hiddenArticleIds: Set<UUID>`, `isRefreshing: Bool`, `error: String?`
- Actions: `selectFeed(_:)`, `dismissArticle(_:)`, `undoDismiss(_:)`, `confirmDismiss(_:)`, `refresh()`, `toggleFavourite(_:)`, `clearError()`

**`FeedsViewModel`**
- State: `feeds: [Feed]`, `unreadCounts: [UUID: Int]`, `isAddingFeed: Bool`, `addFeedError: String?`
- Actions: `addFeed(url:)`, `deleteFeed(_:)`, `clearAddFeedError()`

**`FavouritesViewModel`**
- State: `favourites: [Article]`
- Actions: `removeFavourite(_:)`

**`ArticleReaderViewModel`**
- State: `contentBlocks: [ContentBlock]`, `article: Article?`, `isLoading: Bool`, `titleFontSize: Float`, `bodyFontSize: Float`
- Actions: `loadArticle(id:)`, `toggleFavourite()`, `goToNextArticle()`

**`SettingsViewModel`**
- State: `titleFontSize: Float`, `bodyFontSize: Float`, `themePreference: ThemePreference`
- Actions: `setTitleFontSize(_:)`, `setBodyFontSize(_:)`, `setThemePreference(_:)`

### Screens

**`ArticleListView`**
- `List` with `ArticleCard` rows, `.refreshable` modifier for pull-to-refresh
- Source filter `Menu`/`Picker` in toolbar
- `.swipeActions(edge: .leading)` → mark as read, haptic feedback (`UIImpactFeedbackGenerator`)
- Undo overlay (custom `ToastView` with timed auto-dismiss) — replaces Android Snackbar

**`ArticleCard`**
- `HStack`: source name + relative timestamp (top) + bold title (middle) + star `Button` (trailing)
- Flat design, no card elevation

**`FeedsView`**
- `List` with unread count `Badge`
- `.swipeActions` → delete with `Alert` confirmation
- Toolbar `+` button → sheet with URL `TextField` + "Add" button + loading indicator

**`FavouritesView`**
- `List` with same `ArticleCard`

**`ArticleReaderView`**
- `ScrollView` + `LazyVStack` rendering `ContentBlock` items
- `HeadingView`, `ParagraphView` (`AttributedString` for bold/italic/links), `AsyncImage` for images, `QuoteView` (left-border via overlay)
- "Next Article" button at bottom

**`SettingsView`**
- `Form` with `Section`s: Appearance (theme `Picker`), General (Text Size `NavigationLink`), About (version info)

**`FontSizeView`**
- Two `Slider` controls: title (10–22), body (12–28)
- Live preview `Text` below each slider

### Theme

`@AppStorage("themePreference")` drives `.preferredColorScheme` on the root `RootTabView`. Values: SYSTEM (`.none`), LIGHT (`.light`), DARK (`.dark`).

Colors defined in `Colors.swift` to mirror the Android blue M3 palette (primary `#0061A4`).

---

## Dependencies

| Dependency | Purpose | Source |
|---|---|---|
| SwiftSoup | HTML parsing (Jsoup equivalent) | Swift Package Manager |

All other functionality uses Apple frameworks: SwiftUI, SwiftData, Foundation, XMLParser, URLSession, AppStorage.

---

## Xcode Project Setup

- **Bundle ID**: `com.pavel.pavelrssreader.ios`
- **Deployment Target**: iOS 17.0
- **Swift Package**: SwiftSoup from `https://github.com/scinfu/SwiftSoup`
- **Xcode project location**: `~/Documents/Ios_Dev/PavelRssReader-iOS/`
