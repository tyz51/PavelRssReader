# Settings Screen — Font Size Preferences

**Date:** 2026-03-08
**Status:** Approved

## Goal

Add a persistent Settings screen where the user can independently adjust the font size used for article titles (TopAppBar) and article body text. Choices survive app restarts.

## Architecture

### Persistence — DataStore Preferences

`androidx.datastore:datastore-preferences` stores two float keys:

| Key | Default | Range | Step |
|-----|---------|-------|------|
| `title_font_size` | 14f sp | 10–22 sp | 1 sp |
| `body_font_size` | 17f sp | 12–28 sp | 1 sp |

A `DataStore<Preferences>` singleton is provided by Hilt via a new `DataStoreModule` in `di/`.

### SettingsRepository

```
domain/repository/SettingsRepository.kt       (interface)
data/repository/SettingsRepositoryImpl.kt      (DataStore impl)
di/DataStoreModule.kt                          (Hilt provider)
```

Interface:
```kotlin
interface SettingsRepository {
    val titleFontSize: Flow<Float>
    val bodyFontSize: Flow<Float>
    suspend fun setTitleFontSize(sp: Float)
    suspend fun setBodyFontSize(sp: Float)
}
```

### SettingsViewModel

`presentation/settings/SettingsViewModel.kt`
Collects both flows into `StateFlow<SettingsUiState>`. Exposes `setTitleFontSize` / `setBodyFontSize` which delegate to the repository.

```kotlin
data class SettingsUiState(
    val titleFontSize: Float = 14f,
    val bodyFontSize: Float = 17f
)
```

### SettingsScreen

`presentation/settings/SettingsScreen.kt`
4th bottom-nav tab with icon `Icons.Default.Settings`, label "Settings".

Layout:
```
[ Article title ]
  Sample title text rendered at chosen size
  Slider  10sp ——O—————— 22sp

[ Article body ]
  Short paragraph rendered at chosen size
  Slider  12sp ——————O—— 28sp
```

- Slider changes call `viewModel.setTitleFontSize` / `setBodyFontSize` immediately — no Save button.
- Preview text updates in real time as the slider moves.

### Navigation changes

- `NavRoutes`: add `Settings` data object
- `BottomNavBar`: `bottomNavItems` list gains the Settings entry
- `NavGraph`: add `composable(NavRoutes.Settings.route) { SettingsScreen() }`

### WebViewScreen changes

- Remove the local `fontSize` `mutableFloatStateOf` and the A−/A+ toolbar buttons.
- Collect `bodyFontSize` from `SettingsRepository` via `WebViewViewModel` (add `settingsRepository` injection and expose `bodyFontSize: StateFlow<Float>`).
- Collect `titleFontSize` the same way; pass it as a parameter to the `TopAppBar` title `Text`.

## Files created / modified

| Action | File |
|--------|------|
| Create | `domain/repository/SettingsRepository.kt` |
| Create | `data/repository/SettingsRepositoryImpl.kt` |
| Create | `di/DataStoreModule.kt` |
| Create | `presentation/settings/SettingsViewModel.kt` |
| Create | `presentation/settings/SettingsScreen.kt` |
| Modify | `presentation/navigation/NavRoutes.kt` |
| Modify | `presentation/navigation/BottomNavBar.kt` (via `bottomNavItems`) |
| Modify | `presentation/navigation/NavGraph.kt` |
| Modify | `presentation/webview/WebViewViewModel.kt` |
| Modify | `presentation/webview/WebViewScreen.kt` |
| Modify | `gradle/libs.versions.toml` |
| Modify | `app/build.gradle.kts` |
| Modify | `di/RepositoryModule.kt` |
