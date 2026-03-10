# UI Redesign Design — 2026-03-10

## Overview

Full visual redesign of PavelRssReader to match the provided HTML mockups. Approach: single-pass implementation covering color system, all four screens, DB migration, dark mode, and placeholder content.

---

## 1. Color System & Theme

### New Color Tokens (Light Theme)

| Token | Hex | Usage |
|---|---|---|
| primary | `#0061A4` | Source names, active icons, badges, FAB |
| surface | `#FEFBFF` | Background, nav bar |
| surfaceVariant | `#E0E2EC` | Dividers, nav top border, inactive backgrounds |
| onSurface | `#191C1E` | Primary text, active nav labels |
| onSurfaceVariant | `#44474E` | Secondary text, timestamps, inactive nav icons |
| secondaryContainer | `#D1E4FF` | Active nav pill, feed icon squares, FAB background |
| onSecondaryContainer | `#001D36` | Icons placed on secondaryContainer |

### Dark Theme Color Tokens

| Token | Hex |
|---|---|
| primary | `#9ECAFF` |
| surface | `#191C1E` |
| surfaceVariant | `#44474E` |
| onSurface | `#E2E2E6` |
| onSurfaceVariant | `#C5C6D0` |
| secondaryContainer | `#004881` |
| onSecondaryContainer | `#D1E4FF` |

### Theme Changes
- Disable `dynamicColor` (set to `false`)
- Add `ThemePreference` enum: `SYSTEM`, `LIGHT`, `DARK`
- Store preference in `DataStore<Preferences>` (key: `theme_preference`)
- `PavelRssReaderTheme` reads from DataStore and passes `darkTheme` accordingly
- `MainActivity` collects `ThemePreference` as state before rendering content

---

## 2. Database Migration

Single migration bump (current version → current+1) adding two columns to the `articles` table:

- `sourceName TEXT NOT NULL DEFAULT ''` — populated from RSS `<channel><title>` during sync
- `isRead INTEGER NOT NULL DEFAULT 0` — marked `1` when user opens the article reader

### Article Entity Changes
- Add `val sourceName: String` field
- Add `val isRead: Boolean` field (stored as `Int` in Room)

### RSS Parser Changes
- When parsing a feed, capture `<channel><title>` as the source name
- Pass source name when creating `Article` objects

### Repository/UseCase Changes
- Add `markAsRead(articleId: Long)` use case
- Add `getUnreadCountForFeed(feedId: Long): Flow<Int>` query
- `WebViewScreen` calls `markAsRead` when article loads

---

## 3. Bottom Navigation Bar

### Layout
- Height: 80dp
- Top border: 1dp `surfaceVariant`
- Active tab indicator: pill shape, 64×32dp, corner radius 16dp, `secondaryContainer` background
- Active icon: Material Symbols filled variant (`FILL=1`), `onSecondaryContainer` color
- Inactive icon: Material Symbols outlined variant (`FILL=0`), `onSurfaceVariant` color
- Active label: 12sp bold, `onSurface`
- Inactive label: 12sp medium, `onSurfaceVariant`

### Tabs

| Tab | Route | Inactive icon | Active icon |
|---|---|---|---|
| News | articles | `Outlined.Newspaper` | `Filled.Newspaper` (or FILL=1 variant) |
| Favorites | favourites | `Icons.Outlined.StarBorder` | `Icons.Filled.Star` |
| Feeds | feeds | `Icons.Outlined.RssFeed` | `Icons.Filled.RssFeed` |
| Settings | settings | `Icons.Outlined.Settings` | `Icons.Filled.Settings` |

### Visibility
- Hide bottom nav when current route is `webview/{articleId}` (use `NavBackStackEntry` in `MainActivity`)

---

## 4. Article Card (Shared Component)

```
┌──────────────────────────────────────────┐
│ TECHCRUNCH  •  2h ago                    │  11sp, bold, uppercase, tracking wide
│                                          │  source name = primary color
│ Article title here…                  ★  │  18sp bold, onSurface
└──────────────────────────────────────────┘
```

- Padding: 16dp all sides
- Row gap: 4dp between metadata row and title row
- Source name: `primary` color, rest of metadata row: `onSurfaceVariant`
- Star icon (right-aligned, top of title row):
  - News screen: `Icons.Outlined.StarBorder` (outlined) in `onSurfaceVariant` — tapping toggles favorite
  - Favorites screen: `Icons.Filled.Star` in `primary` — tapping removes from favorites
- Tap on card body navigates to article reader
- Pressed state: `surfaceVariant` background at 30% alpha

---

## 5. News Screen (`ArticleListScreen`)

- TopAppBar title: `"News"` (was `"Feed"`)
- Actions: search icon button (`Icons.Outlined.Search`) — placeholder, no action
- Pull-to-refresh: retained
- Empty state: retained
- Articles: `LazyColumn` with `HorizontalDivider` between items (no card elevation)
- Uses redesigned `ArticleCard`

---

## 6. Favorites Screen (`FavouritesScreen`)

- TopAppBar title: `"Favorites"` (was `"Favourites"`)
- Actions: search icon button — placeholder
- Articles: redesigned `ArticleCard` with filled star
- Empty state: retained

---

## 7. Feeds Screen (`FeedsScreen`)

- TopAppBar title: `"Feeds"` (was `"My Feeds"`)
- Actions: search icon + overflow `more_vert` — both placeholder
- Feed list item layout:
  - Leading: 48×48dp rounded square (`RoundedCornerShape(12dp)`), `secondaryContainer` bg, `rss_feed` icon in `onSecondaryContainer`
  - Center: feed title (16sp bold, `onSurface`), feed URL (14sp, `onSurfaceVariant`)
  - Trailing: unread count badge — pill shape, `primary` bg, white text, 11sp bold; hidden if count = 0
- FAB: `RoundedCornerShape(16dp)` (not fully circular), `secondaryContainer` bg, `add` icon in `onSecondaryContainer`, positioned at `bottom=88dp, end=16dp`
- Add feed dialog: retained as-is

---

## 8. Settings Screen (`SettingsScreen`)

Restructured into 4 grouped sections. Navigation to font size sub-screen retained.

### Section: Account (placeholder — static, no auth)
- User row: 48×48dp circular avatar placeholder (`Icons.Filled.Person`, `surfaceVariant` bg), name `"Your Account"`, subtitle `"Sign in to sync"`, chevron right
- Subscription row: `stars` icon, `"Subscription"`, `PRO` badge (`secondaryContainer` bg, `primary` text, uppercase, 10sp bold)

### Section: General
- **Appearance**: `dark_mode` icon, `"Appearance"`, segmented button with 3 options: `System` / `Light` / `Dark` — **functional**, updates DataStore preference
- **Text Size**: `text_fields` icon, `"Text Size"`, shows current size label (`"Medium"` etc.), chevron → navigates to existing font size sub-screen

### Section: Feeds (all placeholder — no action on tap)
- **Refresh Interval**: `sync` icon, `"Refresh Interval"`, value `"30 mins"`, chevron
- **Clear Cache**: `cleaning_services` icon, `"Clear Cache"`, value `"—"`
- **Push Notifications**: `notifications` icon, `"Push Notifications"`, toggle switch (static `off`)

### Section: About
- **Privacy Policy**: `policy` icon, `"Privacy Policy"`, `open_in_new` icon — placeholder
- **Version**: `info` icon, `"Version"`, value from `BuildConfig.VERSION_NAME`

### Log Out button
- Centered, outlined pill button, `primary` text — placeholder (no action)

---

## 9. Implementation Scope Summary

| Area | Change |
|---|---|
| `Color.kt` | Replace all colors with new palette (light + dark) |
| `Theme.kt` | Disable dynamic color; read ThemePreference from DataStore |
| `Article` entity | Add `sourceName`, `isRead` fields |
| DB migration | Single version bump, ALTER TABLE |
| RSS parser | Capture channel title as sourceName |
| `ArticleDao` | Add `markAsRead`, `getUnreadCountForFeed` queries |
| `ArticleCard` | Full redesign with source name + star icon |
| `ArticleListScreen` | Title → "News", search button |
| `FavouritesScreen` | Title → "Favorites", search button |
| `FeedsScreen` | Title → "Feeds", new item layout with unread badges, FAB shape |
| `SettingsScreen` | Full overhaul with 4 sections, Appearance toggle functional |
| `BottomNavBar` | Custom pill indicator, new icons, hide on WebView |
| `NavRoutes` | Update labels |
| `MainActivity` | Collect theme preference, hide bottom nav on webview route |
| `ThemePreferenceRepository` | New: DataStore read/write for System/Light/Dark |
| `MarkArticleReadUseCase` | New |
| `GetUnreadCountUseCase` | New |
| `WebViewScreen` | Call markAsRead on load |
