# UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the HTML mockup design across all four screens of PavelRssReader, including a new blue color system, custom bottom nav pill indicator, article source names, per-feed unread counts, and a fully restructured Settings screen with functional dark mode toggle.

**Architecture:** Single-pass full redesign. DB bumped from v2 → v3 (add `sourceName` to articles). Theme preference stored in existing DataStore. Custom `BottomNavBar` replaces Material3 `NavigationBar`. Font size sliders move to a `FontSizeScreen` sub-screen; `SettingsScreen` becomes the new 4-section hub.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, DataStore Preferences, Hilt, material-icons-extended (new dep)

---

### Task 1: Add material-icons-extended dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Step 1: Add library entry to version catalog**

In `gradle/libs.versions.toml`, inside `[libraries]`, add after `androidx-compose-material3`:

```toml
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

**Step 2: Add implementation to app module**

In `app/build.gradle.kts`, add after `implementation(libs.androidx.compose.material3)`:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

**Step 3: Sync and verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep "material-icons"`

Expected: line containing `material-icons-extended`

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add material-icons-extended dependency"
```

---

### Task 2: DB Migration — add sourceName column

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/db/entity/ArticleEntity.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/db/AppDatabase.kt`

**Step 1: Add sourceName to ArticleEntity**

Replace the file content:

```kotlin
package com.pavel.pavelrssreader.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [ForeignKey(
        entity = FeedEntity::class,
        parentColumns = ["id"],
        childColumns = ["feedId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("feedId"), Index(value = ["feedId", "guid"], unique = true)]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val publishedAt: Long,
    val fetchedAt: Long,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val sourceName: String = ""
)
```

**Step 2: Add migration and bump version in AppDatabase**

Replace the `AppDatabase` class:

```kotlin
package com.pavel.pavelrssreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity

@Database(
    entities = [FeedEntity::class, ArticleEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN imageUrl TEXT")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN sourceName TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
```

**Step 3: Register migration in DatabaseModule**

In `app/src/main/java/com/pavel/pavelrssreader/di/DatabaseModule.kt`, find the Room builder and add `.addMigrations(AppDatabase.MIGRATION_2_3)`. The full builder should look like:

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "rss_reader.db")
    .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
    .build()
```

**Step 4: Build to verify no compile errors**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/data/db/entity/ArticleEntity.kt \
        app/src/main/java/com/pavel/pavelrssreader/data/db/AppDatabase.kt \
        app/src/main/java/com/pavel/pavelrssreader/di/DatabaseModule.kt
git commit -m "feat: db migration v3 — add sourceName column to articles"
```

---

### Task 3: Domain model + mapper — add sourceName

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/domain/model/Article.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/db/entity/Mappers.kt`

**Step 1: Add sourceName to Article domain model**

```kotlin
package com.pavel.pavelrssreader.domain.model

private const val TTL_MS = 24 * 60 * 60 * 1000L

data class Article(
    val id: Long = 0L,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val publishedAt: Long,
    val fetchedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val sourceName: String = ""
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        !isFavorite && (now - fetchedAt) > TTL_MS
}
```

**Step 2: Update mappers to include sourceName**

Replace `Mappers.kt`:

```kotlin
package com.pavel.pavelrssreader.data.db.entity

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed

fun FeedEntity.toDomain() = Feed(id = id, url = url, title = title, addedAt = addedAt)
fun Feed.toEntity() = FeedEntity(id = id, url = url, title = title, addedAt = addedAt)

fun ArticleEntity.toDomain() = Article(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite, imageUrl = imageUrl, sourceName = sourceName
)

fun Article.toEntity() = ArticleEntity(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite, imageUrl = imageUrl, sourceName = sourceName
)
```

**Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/domain/model/Article.kt \
        app/src/main/java/com/pavel/pavelrssreader/data/db/entity/Mappers.kt
git commit -m "feat: add sourceName to Article domain model and mapper"
```

---

### Task 4: RSS Parser + Repository — populate sourceName

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/network/RssParser.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt`

**Step 1: Update parseRss2 to pass sourceName to each ArticleEntity**

In `RssParser.kt`, in the `parseRss2` function, change the `ArticleEntity(...)` constructor call (around line 83) to add `sourceName = feedTitle`:

```kotlin
articles.add(
    ArticleEntity(
        feedId = feedId,
        guid = guid.ifBlank { "$feedId-$link" },
        title = title,
        link = link,
        description = body,
        publishedAt = parseRfc822(pubDate),
        fetchedAt = System.currentTimeMillis(),
        imageUrl = resolvedImage.ifBlank { null },
        sourceName = feedTitle
    )
)
```

**Step 2: Update parseAtom to pass sourceName**

In `parseAtom` (around line 146), same change:

```kotlin
articles.add(
    ArticleEntity(
        feedId = feedId,
        guid = id.ifBlank { "$feedId-$link" },
        title = title,
        link = link,
        description = summary,
        publishedAt = parseIso8601(updated),
        fetchedAt = System.currentTimeMillis(),
        imageUrl = resolvedImage.ifBlank { null },
        sourceName = feedTitle
    )
)
```

**Step 3: Update RssRepositoryImpl.addFeed to ensure sourceName is set**

In `RssRepositoryImpl.kt`, in `addFeed`, replace:

```kotlin
val articles = parsed.articles.map { it.copy(feedId = id) }
```

with:

```kotlin
val resolvedTitle = parsed.feedTitle.ifBlank { normalizedUrl }
val articles = parsed.articles.map { it.copy(feedId = id, sourceName = resolvedTitle) }
```

**Step 4: Update RssRepositoryImpl.refreshFeed to ensure sourceName is set**

In `refreshFeed`, after fetching and parsing, replace:

```kotlin
articleDao.insertArticles(parsed.articles)
```

with:

```kotlin
val resolvedTitle = parsed.feedTitle.ifBlank { feed.title }
val articlesWithSource = parsed.articles.map { it.copy(sourceName = resolvedTitle) }
articleDao.insertArticles(articlesWithSource)
```

**Step 5: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/data/network/RssParser.kt \
        app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt
git commit -m "feat: populate sourceName from feed channel title during parse"
```

---

### Task 5: Unread counts per feed — DAO, repository, use case

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/db/dao/ArticleDao.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/domain/repository/RssRepository.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetUnreadCountsPerFeedUseCase.kt`

**Step 1: Add FeedUnreadCount + query to ArticleDao**

In `ArticleDao.kt`, add above the `@Dao` annotation:

```kotlin
data class FeedUnreadCount(val feedId: Long, val count: Int)
```

Then inside the `ArticleDao` interface, add:

```kotlin
@Query("SELECT feedId, COUNT(*) as count FROM articles WHERE isRead = 0 GROUP BY feedId")
fun getUnreadCountsPerFeed(): Flow<List<FeedUnreadCount>>
```

**Step 2: Add getUnreadCountsPerFeed to RssRepository interface**

In `RssRepository.kt`, add:

```kotlin
fun getUnreadCountsPerFeed(): Flow<List<FeedUnreadCount>>
```

Also add the import at the top:

```kotlin
import com.pavel.pavelrssreader.data.db.dao.FeedUnreadCount
```

**Step 3: Implement in RssRepositoryImpl**

In `RssRepositoryImpl.kt`, add the import and override:

```kotlin
import com.pavel.pavelrssreader.data.db.dao.FeedUnreadCount
```

```kotlin
override fun getUnreadCountsPerFeed(): Flow<List<FeedUnreadCount>> =
    articleDao.getUnreadCountsPerFeed()
```

**Step 4: Create GetUnreadCountsPerFeedUseCase**

Create new file `GetUnreadCountsPerFeedUseCase.kt`:

```kotlin
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.data.db.dao.FeedUnreadCount
import com.pavel.pavelrssreader.domain.repository.RssRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUnreadCountsPerFeedUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke(): Flow<List<FeedUnreadCount>> = repo.getUnreadCountsPerFeed()
}
```

**Step 5: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/data/db/dao/ArticleDao.kt \
        app/src/main/java/com/pavel/pavelrssreader/domain/repository/RssRepository.kt \
        app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt \
        app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetUnreadCountsPerFeedUseCase.kt
git commit -m "feat: add unread counts per feed query and use case"
```

---

### Task 6: FeedsViewModel — expose unread counts

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModel.kt`

**Step 1: Update FeedsUiState and ViewModel**

Replace the entire file:

```kotlin
package com.pavel.pavelrssreader.presentation.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.AddFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.DeleteFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.GetUnreadCountsPerFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedsUiState(
    val feeds: List<Feed> = emptyList(),
    val unreadCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val addFeedError: String? = null
)

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val getFeedsUseCase: GetFeedsUseCase,
    private val addFeedUseCase: AddFeedUseCase,
    private val deleteFeedUseCase: DeleteFeedUseCase,
    private val getUnreadCountsPerFeedUseCase: GetUnreadCountsPerFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedsUiState())
    val uiState: StateFlow<FeedsUiState> = _uiState.asStateFlow()

    private val _feedAddedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val feedAddedEvent: SharedFlow<Unit> = _feedAddedEvent.asSharedFlow()

    init {
        combine(
            getFeedsUseCase(),
            getUnreadCountsPerFeedUseCase()
        ) { feeds, counts ->
            feeds to counts.associate { it.feedId to it.count }
        }.onEach { (feeds, counts) ->
            _uiState.update { it.copy(feeds = feeds, unreadCounts = counts) }
        }.launchIn(viewModelScope)
    }

    fun addFeed(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, addFeedError = null) }
            when (val result = addFeedUseCase(url)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _feedAddedEvent.tryEmit(Unit)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, addFeedError = result.message) }
            }
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch { deleteFeedUseCase(feedId) }
    }

    fun clearAddFeedError() {
        _uiState.update { it.copy(addFeedError = null) }
    }
}
```

**Step 2: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModel.kt
git commit -m "feat: expose unread counts per feed in FeedsViewModel"
```

---

### Task 7: ThemePreference — infrastructure

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/model/ThemePreference.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/domain/repository/SettingsRepository.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/data/repository/SettingsRepositoryImpl.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsViewModel.kt`

**Step 1: Create ThemePreference enum**

Create `ThemePreference.kt`:

```kotlin
package com.pavel.pavelrssreader.domain.model

enum class ThemePreference { SYSTEM, LIGHT, DARK }
```

**Step 2: Add themePreference to SettingsRepository**

Replace `SettingsRepository.kt`:

```kotlin
package com.pavel.pavelrssreader.domain.repository

import com.pavel.pavelrssreader.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val titleFontSize: Flow<Float>
    val bodyFontSize: Flow<Float>
    val themePreference: Flow<ThemePreference>
    suspend fun setTitleFontSize(sp: Float)
    suspend fun setBodyFontSize(sp: Float)
    suspend fun setThemePreference(pref: ThemePreference)

    companion object {
        const val DEFAULT_TITLE_FONT_SIZE = 14f
        const val DEFAULT_BODY_FONT_SIZE = 17f
    }
}
```

**Step 3: Implement themePreference in SettingsRepositoryImpl**

Replace `SettingsRepositoryImpl.kt`:

```kotlin
package com.pavel.pavelrssreader.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val titleFontSize: Flow<Float> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[KEY_TITLE_FONT_SIZE] ?: SettingsRepository.DEFAULT_TITLE_FONT_SIZE }

    override val bodyFontSize: Flow<Float> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[KEY_BODY_FONT_SIZE] ?: SettingsRepository.DEFAULT_BODY_FONT_SIZE }

    override val themePreference: Flow<ThemePreference> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                when (prefs[KEY_THEME_PREFERENCE]) {
                    ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                    ThemePreference.DARK.name -> ThemePreference.DARK
                    else -> ThemePreference.SYSTEM
                }
            }

    override suspend fun setTitleFontSize(sp: Float) {
        dataStore.edit { it[KEY_TITLE_FONT_SIZE] = sp }
    }

    override suspend fun setBodyFontSize(sp: Float) {
        dataStore.edit { it[KEY_BODY_FONT_SIZE] = sp }
    }

    override suspend fun setThemePreference(pref: ThemePreference) {
        dataStore.edit { it[KEY_THEME_PREFERENCE] = pref.name }
    }

    companion object {
        private val KEY_TITLE_FONT_SIZE = floatPreferencesKey("title_font_size")
        private val KEY_BODY_FONT_SIZE = floatPreferencesKey("body_font_size")
        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }
}
```

**Step 4: Update SettingsViewModel to expose themePreference**

Replace `SettingsViewModel.kt`:

```kotlin
package com.pavel.pavelrssreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val titleFontSize: Float = SettingsRepository.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepository.DEFAULT_BODY_FONT_SIZE,
    val themePreference: ThemePreference = ThemePreference.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize,
        settingsRepository.themePreference
    ) { title, body, theme ->
        SettingsUiState(title, body, theme)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTitleFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setTitleFontSize(sp) }
    }

    fun setBodyFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setBodyFontSize(sp) }
    }

    fun setThemePreference(pref: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(pref) }
    }
}
```

**Step 5: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/domain/model/ThemePreference.kt \
        app/src/main/java/com/pavel/pavelrssreader/domain/repository/SettingsRepository.kt \
        app/src/main/java/com/pavel/pavelrssreader/data/repository/SettingsRepositoryImpl.kt \
        app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsViewModel.kt
git commit -m "feat: add ThemePreference enum, DataStore key, and SettingsViewModel support"
```

---

### Task 8: Color system + Theme

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/ui/theme/Theme.kt`

**Step 1: Replace Color.kt with new blue palette**

```kotlin
package com.pavel.pavelrssreader.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme tokens
val Primary = Color(0xFF0061A4)
val Surface = Color(0xFFFEFBFF)
val SurfaceVariant = Color(0xFFE0E2EC)
val OnSurface = Color(0xFF191C1E)
val OnSurfaceVariant = Color(0xFF44474E)
val SecondaryContainer = Color(0xFFD1E4FF)
val OnSecondaryContainer = Color(0xFF001D36)

// Dark theme tokens
val PrimaryDark = Color(0xFF9ECAFF)
val SurfaceDark = Color(0xFF191C1E)
val SurfaceVariantDark = Color(0xFF44474E)
val OnSurfaceDark = Color(0xFFE2E2E6)
val OnSurfaceVariantDark = Color(0xFFC5C6D0)
val SecondaryContainerDark = Color(0xFF004881)
val OnSecondaryContainerDark = Color(0xFFD1E4FF)
```

**Step 2: Replace Theme.kt — fixed colors, no dynamic color**

```kotlin
package com.pavel.pavelrssreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Surface,
    onBackground = OnSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
)

@Composable
fun PavelRssReaderTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

**Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/ui/theme/Color.kt \
        app/src/main/java/com/pavel/pavelrssreader/ui/theme/Theme.kt
git commit -m "feat: replace purple/pink palette with blue M3 color system"
```

---

### Task 9: MainActivity — theme preference + bottom nav hide

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/MainActivity.kt`

**Step 1: Update MainActivity to collect theme preference and hide nav on webview**

Replace the entire file:

```kotlin
package com.pavel.pavelrssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.presentation.navigation.BottomNavBar
import com.pavel.pavelrssreader.presentation.navigation.NavGraph
import com.pavel.pavelrssreader.presentation.navigation.NavRoutes
import com.pavel.pavelrssreader.presentation.settings.SettingsViewModel
import com.pavel.pavelrssreader.ui.theme.PavelRssReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by settingsViewModel.uiState.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (state.themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemDark
            }

            PavelRssReaderTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                // Hide bottom nav on webview and font size screens
                val showBottomBar = currentRoute != null &&
                    !currentRoute.startsWith("webview/") &&
                    currentRoute != NavRoutes.FontSize.route

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) BottomNavBar(navController)
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

**Step 2: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL (may fail if `NavRoutes.FontSize` doesn't exist yet — that's fine, it will be added in the next task)

**Step 3: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/MainActivity.kt
git commit -m "feat: wire theme preference to app theme and hide bottom nav on detail screens"
```

---

### Task 10: NavRoutes + NavGraph + BottomNavBar redesign

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/BottomNavBar.kt`

**Step 1: Update NavRoutes with new icons, labels, and FontSize route**

Replace `NavRoutes.kt`:

```kotlin
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
) {
    data object Articles : NavRoutes(
        route = "articles",
        label = "News",
        icon = Icons.Outlined.Newspaper,
        activeIcon = Icons.Filled.Newspaper
    )
    data object Favourites : NavRoutes(
        route = "favourites",
        label = "Favorites",
        icon = Icons.Outlined.StarBorder,
        activeIcon = Icons.Filled.Star
    )
    data object Feeds : NavRoutes(
        route = "feeds",
        label = "Feeds",
        icon = Icons.Outlined.RssFeed,
        activeIcon = Icons.Filled.RssFeed
    )
    data object Settings : NavRoutes(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings,
        activeIcon = Icons.Filled.Settings
    )
    data object WebView : NavRoutes(
        route = "webview/{articleId}",
        label = "Article",
        icon = Icons.Outlined.Newspaper,
        activeIcon = Icons.Filled.Newspaper
    ) {
        fun createRoute(articleId: Long) = "webview/$articleId"
    }
    data object FontSize : NavRoutes(
        route = "font_size",
        label = "Text Size",
        icon = Icons.Outlined.Settings,
        activeIcon = Icons.Filled.Settings
    )
}

val bottomNavItems = listOf(
    NavRoutes.Articles,
    NavRoutes.Favourites,
    NavRoutes.Feeds,
    NavRoutes.Settings
)
```

**Step 2: Add FontSize destination to NavGraph**

In `NavGraph.kt`, add the `FontSizeScreen` import and composable. The function signature needs an `onNavigateToFontSize` is handled internally. Replace the full file:

```kotlin
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pavel.pavelrssreader.presentation.articles.ArticleListScreen
import com.pavel.pavelrssreader.presentation.favourites.FavouritesScreen
import com.pavel.pavelrssreader.presentation.feeds.FeedsScreen
import com.pavel.pavelrssreader.presentation.settings.FontSizeScreen
import com.pavel.pavelrssreader.presentation.settings.SettingsScreen
import com.pavel.pavelrssreader.presentation.webview.WebViewScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = NavRoutes.Articles.route, modifier = modifier) {
        composable(NavRoutes.Articles.route) {
            ArticleListScreen(onArticleClick = { articleId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId))
            })
        }
        composable(NavRoutes.Favourites.route) {
            FavouritesScreen(onArticleClick = { articleId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId))
            })
        }
        composable(NavRoutes.Feeds.route) {
            FeedsScreen()
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(onNavigateToFontSize = {
                navController.navigate(NavRoutes.FontSize.route)
            })
        }
        composable(NavRoutes.FontSize.route) {
            FontSizeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.WebView.route,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            WebViewScreen(articleId = articleId, onBack = { navController.popBackStack() })
        }
    }
}
```

**Step 3: Replace BottomNavBar with custom pill indicator**

Replace `BottomNavBar.kt`:

```kotlin
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route
    val borderColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            val interactionSource = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = interactionSource
                    ) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

**Step 4: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL (may fail if FontSizeScreen doesn't exist — add a placeholder `@Composable fun FontSizeScreen(onBack: () -> Unit) {}` temporarily if needed)

**Step 5: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavRoutes.kt \
        app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavGraph.kt \
        app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/BottomNavBar.kt
git commit -m "feat: custom bottom nav pill indicator with new icons and labels"
```

---

### Task 11: Article Card redesign

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleCard.kt`

**Step 1: Replace ArticleCard with new design**

```kotlin
package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavel.pavelrssreader.domain.model.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    onToggleFavourite: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.sourceName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRelativeTime(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { onToggleFavourite(!article.isFavorite) }) {
            Icon(
                imageVector = if (article.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (article.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (article.isFavorite)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
```

**Step 2: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleCard.kt
git commit -m "feat: redesign ArticleCard with source name, timestamp row, and star icon"
```

---

### Task 12: ArticleListScreen + FavouritesScreen updates

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesScreen.kt`

**Step 1: Update ArticleListScreen — title "News" + search placeholder**

In `ArticleListScreen.kt`, change the `topBar` lambda. Find:

```kotlin
topBar = { TopAppBar(title = { Text("Feed") }) },
```

Replace with:

```kotlin
topBar = {
    TopAppBar(
        title = { Text("News") },
        actions = {
            IconButton(onClick = { /* search placeholder */ }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            }
        }
    )
},
```

Also add to imports:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
```

Also remove the Card padding from the `LazyColumn` — the new `ArticleCard` handles its own padding. Replace:

```kotlin
LazyColumn {
    items(state.articles, key = { it.id }) { article ->
        ArticleCard(
            article = article,
            onClick = { onArticleClick(article.id) },
            onToggleFavourite = { isFav ->
                viewModel.toggleFavourite(article.id, isFav)
            }
        )
    }
}
```

With:

```kotlin
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
```

Also add `import androidx.compose.material3.HorizontalDivider` if not present.

**Step 2: Update FavouritesScreen — title "Favorites" + search placeholder**

In `FavouritesScreen.kt`, update the `TopAppBar`:

```kotlin
topBar = {
    TopAppBar(
        title = { Text("Favorites") },
        actions = {
            IconButton(onClick = { /* search placeholder */ }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            }
        }
    )
}
```

Add imports:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
```

**Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt \
        app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesScreen.kt
git commit -m "feat: update News and Favorites screens with new titles and search placeholder"
```

---

### Task 13: FeedsScreen redesign

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt`

**Step 1: Replace FeedsScreen with new design**

```kotlin
package com.pavel.pavelrssreader.presentation.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.domain.model.Feed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(viewModel: FeedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feeds") },
                actions = {
                    IconButton(onClick = { /* search placeholder */ }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* overflow placeholder */ }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add feed")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(state.feeds, key = { it.id }) { feed ->
                FeedListItem(
                    feed = feed,
                    unreadCount = state.unreadCounts[feed.id] ?: 0,
                    onDelete = { viewModel.deleteFeed(feed.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        AddFeedDialog(
            urlInput = urlInput,
            onUrlChange = {
                urlInput = it
                viewModel.clearAddFeedError()
            },
            errorMessage = state.addFeedError,
            isLoading = state.isLoading,
            onConfirm = { viewModel.addFeed(urlInput) },
            onDismiss = {
                showDialog = false
                urlInput = ""
                viewModel.clearAddFeedError()
            }
        )
    }

    LaunchedEffect(viewModel.feedAddedEvent) {
        viewModel.feedAddedEvent.collect {
            showDialog = false
            urlInput = ""
        }
    }
}

@Composable
private fun FeedListItem(
    feed: Feed,
    unreadCount: Int,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.RssFeed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = feed.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = feed.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RSS Feed") },
        text = {
            Column {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading && urlInput.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

**Step 2: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt
git commit -m "feat: redesign FeedsScreen with icon squares, unread badges, and rounded FAB"
```

---

### Task 14: Settings overhaul — FontSizeScreen + new SettingsScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/settings/FontSizeScreen.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsScreen.kt`

**Step 1: Create FontSizeScreen (extracted from old SettingsScreen)**

Create `FontSizeScreen.kt`:

```kotlin
package com.pavel.pavelrssreader.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Size") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
        style = remember(currentSize) { TextStyle(fontSize = currentSize.sp) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Slider(
        value = currentSize,
        onValueChange = { onSizeChange(it.roundToInt().toFloat()) },
        valueRange = min..max,
        steps = maxOf(0, (max - min).toInt() - 1),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label font size, ${currentSize.roundToInt()} sp" }
    )
    Text(
        "${currentSize.roundToInt()} sp",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

**Step 2: Replace SettingsScreen with 4-section design**

```kotlin
package com.pavel.pavelrssreader.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.BuildConfig
import com.pavel.pavelrssreader.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToFontSize: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ACCOUNT
            item {
                SectionHeader("Account")
                AccountRow()
                HorizontalDivider()
                SubscriptionRow()
                HorizontalDivider()
            }
            // GENERAL
            item {
                SectionHeader("General")
                AppearanceRow(
                    preference = state.themePreference,
                    onPreferenceChange = viewModel::setThemePreference
                )
                HorizontalDivider()
                SimpleSettingsRow(
                    icon = Icons.Outlined.TextFields,
                    label = "Text Size",
                    trailingText = textSizeLabel(state.bodyFontSize),
                    showChevron = true,
                    onClick = onNavigateToFontSize
                )
                HorizontalDivider()
            }
            // FEEDS
            item {
                SectionHeader("Feeds")
                SimpleSettingsRow(
                    icon = Icons.Outlined.Sync,
                    label = "Refresh Interval",
                    trailingText = "30 mins",
                    showChevron = true
                )
                HorizontalDivider()
                SimpleSettingsRow(
                    icon = Icons.Outlined.CleaningServices,
                    label = "Clear Cache",
                    trailingText = "—"
                )
                HorizontalDivider()
                SimpleSettingsRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Push Notifications",
                    trailingText = "Off"
                )
                HorizontalDivider()
            }
            // ABOUT
            item {
                SectionHeader("About")
                SimpleSettingsRow(
                    icon = Icons.Outlined.Policy,
                    label = "Privacy Policy",
                    trailingIcon = Icons.Outlined.OpenInNew
                )
                HorizontalDivider()
                SimpleSettingsRow(
                    icon = Icons.Outlined.Info,
                    label = "Version",
                    trailingText = BuildConfig.VERSION_NAME
                )
                HorizontalDivider()
            }
            // LOG OUT
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { /* placeholder */ },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "Log Out",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun AccountRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Your Account", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text("Sign in to sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubscriptionRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("Subscription", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "PRO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AppearanceRow(
    preference: ThemePreference,
    onPreferenceChange: (ThemePreference) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("Appearance", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            ThemeChip("System", preference == ThemePreference.SYSTEM) { onPreferenceChange(ThemePreference.SYSTEM) }
            ThemeChip("Light", preference == ThemePreference.LIGHT) { onPreferenceChange(ThemePreference.LIGHT) }
            ThemeChip("Dark", preference == ThemePreference.DARK) { onPreferenceChange(ThemePreference.DARK) }
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimpleSettingsRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        if (showChevron) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

private fun textSizeLabel(bodyFontSize: Float): String = when {
    bodyFontSize <= 14f -> "Small"
    bodyFontSize <= 17f -> "Medium"
    bodyFontSize <= 21f -> "Large"
    else -> "Extra Large"
}
```

**Step 3: Build to verify**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 4: Final full build**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/pavel/pavelrssreader/presentation/settings/FontSizeScreen.kt \
        app/src/main/java/com/pavel/pavelrssreader/presentation/settings/SettingsScreen.kt
git commit -m "feat: overhaul Settings screen with 4 sections and functional theme toggle"
```

---

## Summary

| Task | What changes |
|---|---|
| 1 | `material-icons-extended` added to deps |
| 2 | `ArticleEntity` + `AppDatabase` v2→v3 migration (sourceName) |
| 3 | `Article` domain model + `Mappers.kt` (sourceName) |
| 4 | `RssParser` + `RssRepositoryImpl` populate sourceName |
| 5 | `ArticleDao` unread counts query + `GetUnreadCountsPerFeedUseCase` |
| 6 | `FeedsViewModel` exposes `unreadCounts: Map<Long, Int>` |
| 7 | `ThemePreference` enum + DataStore key + SettingsViewModel |
| 8 | `Color.kt` blue palette + `Theme.kt` no dynamic color |
| 9 | `MainActivity` reads theme pref, hides bottom nav on WebView |
| 10 | `NavRoutes` + `NavGraph` + custom `BottomNavBar` pill indicator |
| 11 | `ArticleCard` source name row + star icon |
| 12 | `ArticleListScreen` + `FavouritesScreen` titles + search icon |
| 13 | `FeedsScreen` icon squares + unread badges + rounded FAB |
| 14 | `FontSizeScreen` (extracted) + new 4-section `SettingsScreen` |
