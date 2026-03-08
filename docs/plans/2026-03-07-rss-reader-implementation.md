# RSS Reader Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a fully functional Android RSS reader with feed management, 24h article TTL, WebView reading, and Favourites persistence.

**Architecture:** MVVM + Clean Architecture — domain models and use-cases are pure Kotlin; data layer contains Room + Retrofit + XmlPullParser; presentation layer is Jetpack Compose screens with ViewModels exposing StateFlow.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Room 2.7.0, Retrofit 2.11.0, OkHttp 4.12.0, Hilt 2.52, Navigation Compose 2.8.5, KSP, kotlinx-coroutines-test, MockK, Turbine

---

## Task 1: Configure build files

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts` (root)

### Step 1: Replace `gradle/libs.versions.toml` with the full version catalog

```toml
[versions]
agp = "9.1.0"
kotlin = "2.2.10"
ksp = "2.2.10-1.0.31"
coreKtx = "1.16.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.9.0"
lifecycleViewmodelCompose = "2.9.0"
activityCompose = "1.10.1"
composeBom = "2025.05.00"
navigationCompose = "2.9.0"
room = "2.7.1"
hilt = "2.56"
hiltNavigationCompose = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
coroutines = "1.10.2"
mockk = "1.14.2"
turbine = "1.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-scalars = { group = "com.squareup.retrofit2", name = "converter-scalars", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

> **Note on KSP version:** Visit https://github.com/google/ksp/releases and find the release matching Kotlin 2.2.10. The pattern is `{kotlin}-{ksp_minor}`. Update `ksp` in versions if needed.

### Step 2: Replace root `build.gradle.kts`

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### Step 3: Replace `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pavel.pavelrssreader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pavel.pavelrssreader"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.scalars)
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### Step 4: Sync project

Open Android Studio → "Sync Project with Gradle Files" (or run `./gradlew build` from terminal).
Expected: BUILD SUCCESSFUL (all new dependencies resolve).

### Step 5: Commit

```bash
git add gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts
git commit -m "chore: add Room, Hilt, Retrofit, Navigation dependencies"
```

---

## Task 2: Domain models

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/model/Feed.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/model/Article.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/model/Result.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/domain/model/ArticleTest.kt`

### Step 1: Write failing test

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/domain/model/ArticleTest.kt
package com.pavel.pavelrssreader.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleTest {

    @Test
    fun `isExpired returns true when fetchedAt older than 24h and not favourite`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 90_000_000L,
            fetchedAt = now - 90_000_000L, // 25 hours ago
            isRead = false,
            isFavorite = false
        )
        assertTrue(article.isExpired(now))
    }

    @Test
    fun `isExpired returns false when favourite regardless of age`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 90_000_000L,
            fetchedAt = now - 90_000_000L, // 25 hours ago
            isRead = false,
            isFavorite = true
        )
        assertFalse(article.isExpired(now))
    }

    @Test
    fun `isExpired returns false when fetchedAt within 24h`() {
        val now = System.currentTimeMillis()
        val article = Article(
            id = 1L,
            feedId = 1L,
            guid = "guid-1",
            title = "Test",
            link = "https://example.com",
            description = "Desc",
            publishedAt = now - 3_600_000L,
            fetchedAt = now - 3_600_000L, // 1 hour ago
            isRead = false,
            isFavorite = false
        )
        assertFalse(article.isExpired(now))
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.domain.model.ArticleTest"
```

Expected: FAIL — `Article` class not found.

### Step 3: Create domain models

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/model/Feed.kt
package com.pavel.pavelrssreader.domain.model

data class Feed(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/model/Article.kt
package com.pavel.pavelrssreader.domain.model

private const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours in ms

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
    val isFavorite: Boolean = false
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        !isFavorite && (now - fetchedAt) > TTL_MS
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/model/Result.kt
package com.pavel.pavelrssreader.domain.model

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}
```

### Step 4: Run tests to verify they pass

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.domain.model.ArticleTest"
```

Expected: 3 tests PASS.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: add domain models Feed, Article, Result"
```

---

## Task 3: Room entities

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/entity/FeedEntity.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/entity/ArticleEntity.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/entity/Mappers.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/data/db/entity/MappersTest.kt`

### Step 1: Write failing test for mappers

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/data/db/entity/MappersTest.kt
package com.pavel.pavelrssreader.data.db.entity

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun `FeedEntity toDomain maps all fields`() {
        val entity = FeedEntity(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L)
        val domain = entity.toDomain()
        assertEquals(Feed(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L), domain)
    }

    @Test
    fun `Feed toEntity maps all fields`() {
        val domain = Feed(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L)
        val entity = domain.toEntity()
        assertEquals(FeedEntity(id = 1L, url = "https://feed.com/rss", title = "My Feed", addedAt = 12345L), entity)
    }

    @Test
    fun `ArticleEntity toDomain maps all fields`() {
        val now = 999L
        val entity = ArticleEntity(
            id = 2L, feedId = 1L, guid = "g1", title = "T", link = "L",
            description = "D", publishedAt = now, fetchedAt = now,
            isRead = true, isFavorite = false
        )
        val domain = entity.toDomain()
        assertEquals("g1", domain.guid)
        assertEquals(true, domain.isRead)
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.db.entity.MappersTest"
```

Expected: FAIL — entities not found.

### Step 3: Create entities and mappers

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/entity/FeedEntity.kt
package com.pavel.pavelrssreader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val url: String,
    val title: String,
    val addedAt: Long
)
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/entity/ArticleEntity.kt
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
    val isFavorite: Boolean = false
)
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/entity/Mappers.kt
package com.pavel.pavelrssreader.data.db.entity

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed

fun FeedEntity.toDomain() = Feed(id = id, url = url, title = title, addedAt = addedAt)
fun Feed.toEntity() = FeedEntity(id = id, url = url, title = title, addedAt = addedAt)

fun ArticleEntity.toDomain() = Article(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite
)

fun Article.toEntity() = ArticleEntity(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite
)
```

### Step 4: Run tests to verify they pass

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.db.entity.MappersTest"
```

Expected: 3 tests PASS.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: add Room entities and domain mappers"
```

---

## Task 4: Room DAOs and Database

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/dao/FeedDao.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/dao/ArticleDao.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/db/AppDatabase.kt`
- Create: `app/src/androidTest/java/com/pavel/pavelrssreader/data/db/dao/ArticleDaoTest.kt`

### Step 1: Create DAOs and AppDatabase

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/dao/FeedDao.kt
package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds ORDER BY addedAt DESC")
    fun getAllFeeds(): Flow<List<FeedEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeed(feed: FeedEntity): Long

    @Query("DELETE FROM feeds WHERE id = :feedId")
    suspend fun deleteFeed(feedId: Long)

    @Query("SELECT * FROM feeds")
    suspend fun getAllFeedsOneShot(): List<FeedEntity>
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/dao/ArticleDao.kt
package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavouriteArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isFavorite = :isFavorite WHERE id = :articleId")
    suspend fun setFavourite(articleId: Long, isFavorite: Boolean)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :articleId")
    suspend fun markAsRead(articleId: Long)

    @Query("DELETE FROM articles WHERE isFavorite = 0 AND fetchedAt < :cutoffTime")
    suspend fun deleteExpiredArticles(cutoffTime: Long)
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/db/AppDatabase.kt
package com.pavel.pavelrssreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity

@Database(
    entities = [FeedEntity::class, ArticleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
}
```

### Step 2: Write instrumented DAO test

```kotlin
// app/src/androidTest/java/com/pavel/pavelrssreader/data/db/dao/ArticleDaoTest.kt
package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.pavelrssreader.data.db.AppDatabase
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var articleDao: ArticleDao
    private lateinit var feedDao: FeedDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        articleDao = db.articleDao()
        feedDao = db.feedDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveArticles() = runTest {
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        val articles = listOf(
            ArticleEntity(feedId = 1L, guid = "g1", title = "T1", link = "L1",
                description = "D1", publishedAt = 1000L, fetchedAt = 1000L)
        )
        articleDao.insertArticles(articles)
        val result = articleDao.getAllArticles().first()
        assertEquals(1, result.size)
        assertEquals("T1", result[0].title)
    }

    @Test
    fun deleteExpiredArticles_keepsNonExpiredAndFavourites() = runTest {
        val now = System.currentTimeMillis()
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        articleDao.insertArticles(listOf(
            ArticleEntity(id = 1L, feedId = 1L, guid = "old", title = "Old", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = now - 90_000_000L), // expired
            ArticleEntity(id = 2L, feedId = 1L, guid = "fav", title = "Fav", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = now - 90_000_000L,
                isFavorite = true), // expired but favourite
            ArticleEntity(id = 3L, feedId = 1L, guid = "new", title = "New", link = "L",
                description = "D", publishedAt = now, fetchedAt = now) // fresh
        ))
        articleDao.deleteExpiredArticles(now - 86_400_000L)
        val result = articleDao.getAllArticles().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.guid == "fav" })
        assertTrue(result.any { it.guid == "new" })
    }

    @Test
    fun toggleFavourite() = runTest {
        feedDao.insertFeed(FeedEntity(id = 1L, url = "https://test.com/rss", title = "Test", addedAt = 0L))
        articleDao.insertArticles(listOf(
            ArticleEntity(id = 1L, feedId = 1L, guid = "g1", title = "T", link = "L",
                description = "D", publishedAt = 0L, fetchedAt = 0L)
        ))
        articleDao.setFavourite(1L, true)
        val article = articleDao.getArticleById(1L)!!
        assertTrue(article.isFavorite)
    }
}
```

### Step 3: Run instrumented tests on a device/emulator

```bash
./gradlew :app:connectedAndroidTest --tests "com.pavel.pavelrssreader.data.db.dao.ArticleDaoTest"
```

Expected: 3 tests PASS.

### Step 4: Commit

```bash
git add app/src/
git commit -m "feat: add Room DAOs and AppDatabase"
```

---

## Task 5: RSS Parser

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/network/RssParser.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/data/network/RssParserTest.kt`

### Step 1: Write failing test

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/data/network/RssParserTest.kt
package com.pavel.pavelrssreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RssParserTest {

    private val parser = RssParser()

    private val rss2Xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <item>
              <title>Article One</title>
              <link>https://example.com/1</link>
              <description>Body of article one</description>
              <guid>unique-guid-1</guid>
              <pubDate>Mon, 06 Mar 2026 12:00:00 +0000</pubDate>
            </item>
            <item>
              <title>Article Two</title>
              <link>https://example.com/2</link>
              <description>Body of article two</description>
              <guid>unique-guid-2</guid>
              <pubDate>Sun, 05 Mar 2026 09:00:00 +0000</pubDate>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val atomXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Atom Feed</title>
          <entry>
            <title>Atom Article</title>
            <link href="https://example.com/atom/1"/>
            <summary>Atom summary</summary>
            <id>atom-id-1</id>
            <updated>2026-03-06T12:00:00Z</updated>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun `parse RSS 2 returns feed title and articles`() {
        val result = parser.parse(rss2Xml, feedId = 1L)
        assertEquals("Test Feed", result.feedTitle)
        assertEquals(2, result.articles.size)
        assertEquals("Article One", result.articles[0].title)
        assertEquals("https://example.com/1", result.articles[0].link)
        assertEquals("unique-guid-1", result.articles[0].guid)
    }

    @Test
    fun `parse Atom returns feed title and articles`() {
        val result = parser.parse(atomXml, feedId = 1L)
        assertEquals("Atom Feed", result.feedTitle)
        assertEquals(1, result.articles.size)
        assertEquals("Atom Article", result.articles[0].title)
        assertEquals("https://example.com/atom/1", result.articles[0].link)
        assertEquals("atom-id-1", result.articles[0].guid)
    }

    @Test
    fun `parse assigns correct feedId to articles`() {
        val result = parser.parse(rss2Xml, feedId = 42L)
        result.articles.forEach { assertEquals(42L, it.feedId) }
    }

    @Test
    fun `parse malformed XML returns empty result without crashing`() {
        val result = parser.parse("<not-rss/>", feedId = 1L)
        assertEquals(0, result.articles.size)
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.network.RssParserTest"
```

Expected: FAIL — `RssParser` not found.

### Step 3: Implement RSS parser

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/network/RssParser.kt
package com.pavel.pavelrssreader.data.network

import android.util.Log
import android.util.Xml
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedFeed(val feedTitle: String, val articles: List<ArticleEntity>)

class RssParser {

    private val rfc822Format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

    fun parse(xml: String, feedId: Long): ParsedFeed {
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(xml))
            }
            when {
                xml.contains("<rss") -> parseRss2(parser, feedId)
                xml.contains("<feed") -> parseAtom(parser, feedId)
                else -> ParsedFeed("", emptyList())
            }
        } catch (e: Exception) {
            Log.w("RssParser", "Failed to parse feed for feedId=$feedId", e)
            ParsedFeed("", emptyList())
        }
    }

    private fun parseRss2(parser: XmlPullParser, feedId: Long): ParsedFeed {
        var feedTitle = ""
        val articles = mutableListOf<ArticleEntity>()
        var inItem = false
        var title = ""; var link = ""; var description = ""; var guid = ""; var pubDate = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { inItem = true; title = ""; link = ""; description = ""; guid = ""; pubDate = "" }
                    "title" -> if (!inItem) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> link = parser.nextText()
                    "description" -> description = parser.nextText()
                    "guid" -> guid = parser.nextText()
                    "pubDate" -> pubDate = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = guid.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = description,
                            publishedAt = parseRfc822(pubDate),
                            fetchedAt = System.currentTimeMillis()
                        )
                    )
                    inItem = false
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed(feedTitle, articles)
    }

    private fun parseAtom(parser: XmlPullParser, feedId: Long): ParsedFeed {
        var feedTitle = ""
        val articles = mutableListOf<ArticleEntity>()
        var inEntry = false
        var title = ""; var link = ""; var summary = ""; var id = ""; var updated = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> { inEntry = true; title = ""; link = ""; summary = ""; id = ""; updated = "" }
                    "title" -> if (!inEntry) feedTitle = parser.nextText() else title = parser.nextText()
                    "link" -> link = parser.getAttributeValue(null, "href") ?: ""
                    "summary", "content" -> summary = parser.nextText()
                    "id" -> id = parser.nextText()
                    "updated" -> updated = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry" && inEntry) {
                    articles.add(
                        ArticleEntity(
                            feedId = feedId,
                            guid = id.ifBlank { "$feedId-$link" },
                            title = title,
                            link = link,
                            description = summary,
                            publishedAt = parseIso8601(updated),
                            fetchedAt = System.currentTimeMillis()
                        )
                    )
                    inEntry = false
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed(feedTitle, articles)
    }

    private fun parseRfc822(date: String): Long =
        runCatching { rfc822Format.parse(date.trim())?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())

    private fun parseIso8601(date: String): Long =
        runCatching { iso8601Format.parse(date.replace("Z", "+0000").trim())?.time ?: System.currentTimeMillis() }
            .getOrDefault(System.currentTimeMillis())
}
```

> **Note:** `android.util.Xml` and `android.util.Log` are Android-framework classes. For unit tests to work, add `testOptions { unitTests { isReturnDefaultValues = true } }` to the `android {}` block in `app/build.gradle.kts`.

Add to `android {}` in `app/build.gradle.kts`:
```kotlin
testOptions {
    unitTests {
        isReturnDefaultValues = true
    }
}
```

### Step 4: Run tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.network.RssParserTest"
```

Expected: 4 tests PASS.

### Step 5: Commit

```bash
git add app/src/ app/build.gradle.kts
git commit -m "feat: add RSS/Atom XML parser"
```

---

## Task 6: Retrofit network service

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/network/RssApiService.kt`

### Step 1: Create Retrofit service (no test needed — interface only)

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/network/RssApiService.kt
package com.pavel.pavelrssreader.data.network

import retrofit2.http.GET
import retrofit2.http.Url

interface RssApiService {
    @GET
    suspend fun fetchFeed(@Url url: String): String
}
```

### Step 2: Commit

```bash
git add app/src/
git commit -m "feat: add Retrofit RSS API service interface"
```

---

## Task 7: Repository

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/repository/RssRepository.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImplTest.kt`

### Step 1: Create repository interface

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/repository/RssRepository.kt
package com.pavel.pavelrssreader.domain.repository

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun getAllArticles(): Flow<List<Article>>
    fun getFavouriteArticles(): Flow<List<Article>>
    fun getAllFeeds(): Flow<List<Feed>>
    suspend fun addFeed(url: String): Result<Feed>
    suspend fun deleteFeed(feedId: Long)
    suspend fun refreshFeed(feed: Feed): Result<Unit>
    suspend fun refreshAllFeeds(): Result<Unit>
    suspend fun setFavourite(articleId: Long, isFavorite: Boolean)
    suspend fun markAsRead(articleId: Long)
}
```

### Step 2: Write failing test for repository

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImplTest.kt
package com.pavel.pavelrssreader.data.repository

import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import com.pavel.pavelrssreader.data.network.ParsedFeed
import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import com.pavel.pavelrssreader.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RssRepositoryImplTest {

    private val feedDao = mockk<FeedDao>(relaxed = true)
    private val articleDao = mockk<ArticleDao>(relaxed = true)
    private val apiService = mockk<RssApiService>()
    private val parser = mockk<RssParser>()

    private val repo = RssRepositoryImpl(feedDao, articleDao, apiService, parser)

    @Test
    fun `refreshFeed fetches XML, parses it, inserts articles, and cleans expired`() = runTest {
        val feed = com.pavel.pavelrssreader.domain.model.Feed(id = 1L, url = "https://feed.com/rss", title = "Feed", addedAt = 0L)
        val rawXml = "<rss/>"
        val parsedArticle = ArticleEntity(feedId = 1L, guid = "g1", title = "T", link = "L",
            description = "D", publishedAt = 0L, fetchedAt = 0L)
        coEvery { apiService.fetchFeed("https://feed.com/rss") } returns rawXml
        every { parser.parse(rawXml, 1L) } returns ParsedFeed("Feed", listOf(parsedArticle))

        val result = repo.refreshFeed(feed)

        assertTrue(result is Result.Success)
        coVerify { articleDao.insertArticles(listOf(parsedArticle)) }
        coVerify { articleDao.deleteExpiredArticles(any()) }
    }

    @Test
    fun `refreshFeed returns Error when network throws`() = runTest {
        val feed = com.pavel.pavelrssreader.domain.model.Feed(id = 1L, url = "https://bad.url/rss", title = "Bad", addedAt = 0L)
        coEvery { apiService.fetchFeed(any()) } throws Exception("Connection refused")

        val result = repo.refreshFeed(feed)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `addFeed validates URL format before fetching`() = runTest {
        val result = repo.addFeed("not-a-url")
        assertTrue(result is Result.Error)
    }
}
```

### Step 3: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.repository.RssRepositoryImplTest"
```

Expected: FAIL — `RssRepositoryImpl` not found.

### Step 4: Implement repository

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/data/repository/RssRepositoryImpl.kt
package com.pavel.pavelrssreader.data.repository

import android.util.Patterns
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import com.pavel.pavelrssreader.data.db.entity.toDomain
import com.pavel.pavelrssreader.data.db.entity.toEntity
import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.repository.RssRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RssRepositoryImpl @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val apiService: RssApiService,
    private val parser: RssParser
) : RssRepository {

    override fun getAllArticles(): Flow<List<Article>> =
        articleDao.getAllArticles().map { it.map { entity -> entity.toDomain() } }

    override fun getFavouriteArticles(): Flow<List<Article>> =
        articleDao.getFavouriteArticles().map { it.map { entity -> entity.toDomain() } }

    override fun getAllFeeds(): Flow<List<Feed>> =
        feedDao.getAllFeeds().map { it.map { entity -> entity.toDomain() } }

    override suspend fun addFeed(url: String): Result<Feed> {
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return Result.Error("Invalid URL: $url")
        }
        return try {
            val rawXml = apiService.fetchFeed(url)
            val parsed = parser.parse(rawXml, feedId = 0L)
            val feedTitle = parsed.feedTitle.ifBlank { url }
            val entity = FeedEntity(url = url, title = feedTitle, addedAt = System.currentTimeMillis())
            val id = feedDao.insertFeed(entity)
            val feed = Feed(id = id, url = url, title = feedTitle, addedAt = entity.addedAt)
            val articles = parsed.articles.map { it.copy(feedId = id) }
            articleDao.insertArticles(articles)
            articleDao.deleteExpiredArticles(System.currentTimeMillis() - 86_400_000L)
            Result.Success(feed)
        } catch (e: Exception) {
            Result.Error("Failed to add feed: ${e.message}", e)
        }
    }

    override suspend fun deleteFeed(feedId: Long) {
        feedDao.deleteFeed(feedId)
        // Articles are CASCADE deleted by Room foreign key
    }

    override suspend fun refreshFeed(feed: Feed): Result<Unit> {
        return try {
            val rawXml = apiService.fetchFeed(feed.url)
            val parsed = parser.parse(rawXml, feed.id)
            articleDao.insertArticles(parsed.articles)
            articleDao.deleteExpiredArticles(System.currentTimeMillis() - 86_400_000L)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh ${feed.title}: ${e.message}", e)
        }
    }

    override suspend fun refreshAllFeeds(): Result<Unit> {
        val feeds = feedDao.getAllFeedsOneShot().map { it.toDomain() }
        val errors = feeds.mapNotNull { feed ->
            (refreshFeed(feed) as? Result.Error)?.message
        }
        return if (errors.isEmpty()) Result.Success(Unit)
        else Result.Error(errors.joinToString("\n"))
    }

    override suspend fun setFavourite(articleId: Long, isFavorite: Boolean) {
        articleDao.setFavourite(articleId, isFavorite)
    }

    override suspend fun markAsRead(articleId: Long) {
        articleDao.markAsRead(articleId)
    }
}
```

### Step 5: Run tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.data.repository.RssRepositoryImplTest"
```

Expected: 3 tests PASS.

### Step 6: Commit

```bash
git add app/src/
git commit -m "feat: add RssRepository interface and implementation"
```

---

## Task 8: Use Cases

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetArticlesUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetFavouritesUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetFeedsUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/AddFeedUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/DeleteFeedUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/RefreshFeedsUseCase.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/domain/usecase/ToggleFavouriteUseCase.kt`

### Step 1: Create all use cases (thin delegation to repository)

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetArticlesUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class GetArticlesUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke() = repo.getAllArticles()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetFavouritesUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class GetFavouritesUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke() = repo.getFavouriteArticles()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/GetFeedsUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class GetFeedsUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke() = repo.getAllFeeds()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/AddFeedUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class AddFeedUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(url: String) = repo.addFeed(url)
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/DeleteFeedUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class DeleteFeedUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(feedId: Long) = repo.deleteFeed(feedId)
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/RefreshFeedsUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class RefreshFeedsUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke() = repo.refreshAllFeeds()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/domain/usecase/ToggleFavouriteUseCase.kt
package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class ToggleFavouriteUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(articleId: Long, isFavorite: Boolean) =
        repo.setFavourite(articleId, isFavorite)
}
```

### Step 2: Compile check

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL, no errors.

### Step 3: Commit

```bash
git add app/src/
git commit -m "feat: add domain use cases"
```

---

## Task 9: Hilt dependency injection

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/pavel/pavelrssreader/RssReaderApp.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/di/NetworkModule.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/di/RepositoryModule.kt`

### Step 1: Create Application class

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/RssReaderApp.kt
package com.pavel.pavelrssreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RssReaderApp : Application()
```

### Step 2: Register Application class in AndroidManifest.xml

Edit `app/src/main/AndroidManifest.xml` — add `android:name=".RssReaderApp"` to `<application>`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".RssReaderApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PavelRssReader">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.PavelRssReader">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### Step 3: Create Hilt modules

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/di/DatabaseModule.kt
package com.pavel.pavelrssreader.di

import android.content.Context
import androidx.room.Room
import com.pavel.pavelrssreader.data.db.AppDatabase
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "rss_reader.db").build()

    @Provides
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/di/NetworkModule.kt
package com.pavel.pavelrssreader.di

import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/") // @Url overrides this
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideRssApiService(retrofit: Retrofit): RssApiService =
        retrofit.create(RssApiService::class.java)

    @Provides
    @Singleton
    fun provideRssParser(): RssParser = RssParser()
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/di/RepositoryModule.kt
package com.pavel.pavelrssreader.di

import com.pavel.pavelrssreader.data.repository.RssRepositoryImpl
import com.pavel.pavelrssreader.domain.repository.RssRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRssRepository(impl: RssRepositoryImpl): RssRepository
}
```

### Step 4: Compile check

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: add Hilt DI modules and application class"
```

---

## Task 10: Navigation setup

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavRoutes.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavGraph.kt`

### Step 1: Create navigation components

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavRoutes.kt
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(val route: String, val label: String, val icon: ImageVector) {
    object Articles : NavRoutes("articles", "Feed", Icons.Default.Home)
    object Favourites : NavRoutes("favourites", "Favourites", Icons.Default.Favorite)
    object Feeds : NavRoutes("feeds", "Feeds", Icons.Default.List)
    object WebView : NavRoutes("webview/{articleId}", "Article", Icons.Default.Home) {
        fun createRoute(articleId: Long) = "webview/$articleId"
    }
}

val bottomNavItems = listOf(NavRoutes.Articles, NavRoutes.Favourites, NavRoutes.Feeds)
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/BottomNavBar.kt
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/navigation/NavGraph.kt
package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pavel.pavelrssreader.presentation.articles.ArticleListScreen
import com.pavel.pavelrssreader.presentation.favourites.FavouritesScreen
import com.pavel.pavelrssreader.presentation.feeds.FeedsScreen
import com.pavel.pavelrssreader.presentation.webview.WebViewScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.Articles.route) {
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

### Step 2: Compile check (screens don't exist yet — add stub files)

Create empty stub files so the NavGraph compiles. They will be replaced in later tasks:

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt
package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.runtime.Composable

@Composable
fun ArticleListScreen(onArticleClick: (Long) -> Unit) {}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesScreen.kt
package com.pavel.pavelrssreader.presentation.favourites

import androidx.compose.runtime.Composable

@Composable
fun FavouritesScreen(onArticleClick: (Long) -> Unit) {}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt
package com.pavel.pavelrssreader.presentation.feeds

import androidx.compose.runtime.Composable

@Composable
fun FeedsScreen() {}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt
package com.pavel.pavelrssreader.presentation.webview

import androidx.compose.runtime.Composable

@Composable
fun WebViewScreen(articleId: Long, onBack: () -> Unit) {}
```

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

### Step 3: Commit

```bash
git add app/src/
git commit -m "feat: add navigation routes, bottom bar, and NavGraph"
```

---

## Task 11: MainActivity

**Files:**
- Modify: `app/src/main/java/com/pavel/pavelrssreader/MainActivity.kt`

### Step 1: Replace MainActivity with Hilt + Navigation wiring

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/MainActivity.kt
package com.pavel.pavelrssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pavel.pavelrssreader.presentation.navigation.BottomNavBar
import com.pavel.pavelrssreader.presentation.navigation.NavGraph
import com.pavel.pavelrssreader.ui.theme.PavelRssReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PavelRssReaderTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        // pass Modifier.padding(innerPadding) via each screen's root composable
                    )
                }
            }
        }
    }
}
```

> **Note:** Pass `innerPadding` down to screens as needed, or apply `Modifier.padding(innerPadding)` inside `NavGraph`.

### Step 2: Compile check

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

### Step 3: Commit

```bash
git add app/src/main/java/com/pavel/pavelrssreader/MainActivity.kt
git commit -m "feat: wire MainActivity with Hilt and bottom navigation"
```

---

## Task 12: FeedsScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModel.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModelTest.kt`

### Step 1: Write failing ViewModel test

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModelTest.kt
package com.pavel.pavelrssreader.presentation.feeds

import app.cash.turbine.test
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.AddFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.DeleteFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedsViewModelTest {

    private val getFeedsUseCase = mockk<GetFeedsUseCase>()
    private val addFeedUseCase = mockk<AddFeedUseCase>()
    private val deleteFeedUseCase = mockk<DeleteFeedUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `feeds StateFlow emits feeds from use case`() = runTest {
        val feeds = listOf(Feed(id = 1L, url = "https://a.com/rss", title = "A", addedAt = 0L))
        every { getFeedsUseCase() } returns flowOf(feeds)
        coEvery { addFeedUseCase(any()) } returns Result.Success(feeds[0])

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(feeds, state.feeds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addFeed success clears dialog`() = runTest {
        every { getFeedsUseCase() } returns flowOf(emptyList())
        val feed = Feed(id = 1L, url = "https://b.com/rss", title = "B", addedAt = 0L)
        coEvery { addFeedUseCase("https://b.com/rss") } returns Result.Success(feed)

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        vm.addFeed("https://b.com/rss")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.addFeedError)
    }

    @Test
    fun `addFeed error sets error message`() = runTest {
        every { getFeedsUseCase() } returns flowOf(emptyList())
        coEvery { addFeedUseCase("bad") } returns Result.Error("Invalid URL: bad")

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        vm.addFeed("bad")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Invalid URL: bad", vm.uiState.value.addFeedError)
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.presentation.feeds.FeedsViewModelTest"
```

Expected: FAIL — `FeedsViewModel` not found.

### Step 3: Implement FeedsViewModel and FeedsScreen

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsViewModel.kt
package com.pavel.pavelrssreader.presentation.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.AddFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.DeleteFeedUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedsUiState(
    val feeds: List<Feed> = emptyList(),
    val isLoading: Boolean = false,
    val addFeedError: String? = null
)

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val getFeedsUseCase: GetFeedsUseCase,
    private val addFeedUseCase: AddFeedUseCase,
    private val deleteFeedUseCase: DeleteFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedsUiState())
    val uiState: StateFlow<FeedsUiState> = _uiState.asStateFlow()

    init {
        getFeedsUseCase()
            .onEach { feeds -> _uiState.update { it.copy(feeds = feeds) } }
            .launchIn(viewModelScope)
    }

    fun addFeed(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, addFeedError = null) }
            when (val result = addFeedUseCase(url)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
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

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/feeds/FeedsScreen.kt
package com.pavel.pavelrssreader.presentation.feeds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(viewModel: FeedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Feeds") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add feed")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.feeds, key = { it.id }) { feed ->
                ListItem(
                    headlineContent = { Text(feed.title) },
                    supportingContent = { Text(feed.url, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteFeed(feed.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete feed")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        AddFeedDialog(
            urlInput = urlInput,
            onUrlChange = { urlInput = it; viewModel.clearAddFeedError() },
            errorMessage = state.addFeedError,
            isLoading = state.isLoading,
            onConfirm = { viewModel.addFeed(urlInput) },
            onDismiss = { showDialog = false; urlInput = ""; viewModel.clearAddFeedError() }
        )
        if (!state.isLoading && state.addFeedError == null && urlInput.isNotBlank()) {
            LaunchedEffect(state.feeds.size) { showDialog = false; urlInput = "" }
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading && urlInput.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

### Step 4: Run tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.presentation.feeds.FeedsViewModelTest"
```

Expected: 3 tests PASS.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: implement FeedsScreen and FeedsViewModel"
```

---

## Task 13: ArticleListScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModel.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt`
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleCard.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModelTest.kt`

### Step 1: Write failing ViewModel test

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModelTest.kt
package com.pavel.pavelrssreader.presentation.articles

import app.cash.turbine.test
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.RefreshFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {

    private val getArticlesUseCase = mockk<GetArticlesUseCase>()
    private val refreshFeedsUseCase = mockk<RefreshFeedsUseCase>()
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleArticle = Article(
        id = 1L, feedId = 1L, guid = "g1", title = "Test Article",
        link = "https://example.com", description = "Desc",
        publishedAt = System.currentTimeMillis(), fetchedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `articles StateFlow emits articles from use case`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.articles.contains(sampleArticle))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh sets isRefreshing true then false`() = runTest {
        every { getArticlesUseCase() } returns flowOf(emptyList())
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `toggleFavourite calls use case`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.toggleFavourite(1L, true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, true) }
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.presentation.articles.ArticleListViewModelTest"
```

Expected: FAIL.

### Step 3: Implement ArticleListViewModel, ArticleCard, and ArticleListScreen

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListViewModel.kt
package com.pavel.pavelrssreader.presentation.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.RefreshFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val refreshFeedsUseCase: RefreshFeedsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    init {
        getArticlesUseCase()
            .onEach { articles -> _uiState.update { it.copy(articles = articles) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshFeedsUseCase()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    errorMessage = (result as? com.pavel.pavelrssreader.domain.model.Result.Error)?.message
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

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleCard.kt
package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRelativeTime(article.publishedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onToggleFavourite(!article.isFavorite) }) {
                Icon(
                    imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (article.isFavorite) "Remove from favourites" else "Add to favourites",
                    tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/articles/ArticleListScreen.kt
package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Feed") }) },
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
                            onToggleFavourite = { isFav -> viewModel.toggleFavourite(article.id, isFav) }
                        )
                    }
                }
            }
        }
    }
}
```

### Step 4: Run tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.presentation.articles.ArticleListViewModelTest"
```

Expected: 3 tests PASS.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: implement ArticleListScreen with pull-to-refresh and article cards"
```

---

## Task 14: WebViewScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewViewModel.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt`

### Step 1: Implement WebViewViewModel

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewViewModel.kt
package com.pavel.pavelrssreader.presentation.webview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.entity.toDomain
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class WebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleDao: ArticleDao,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val articleId: Long = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val article = articleDao.getArticleById(articleId)?.toDomain()
            _uiState.update { it.copy(article = article, isLoading = false) }
            article?.let { articleDao.markAsRead(it.id) }
        }
    }

    fun toggleFavourite() {
        val article = _uiState.value.article ?: return
        viewModelScope.launch {
            val newFav = !article.isFavorite
            toggleFavouriteUseCase(article.id, newFav)
            _uiState.update { it.copy(article = article.copy(isFavorite = newFav)) }
        }
    }
}
```

### Step 2: Implement WebViewScreen

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/webview/WebViewScreen.kt
package com.pavel.pavelrssreader.presentation.webview

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    articleId: Long,
    onBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val article = state.article

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article?.title ?: "Loading…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavourite() },
                        enabled = article != null
                    ) {
                        Icon(
                            imageVector = if (article?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favourite"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading || article == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            settings.javaScriptEnabled = true
                            loadUrl(article.link)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
```

### Step 3: Compile check

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

### Step 4: Commit

```bash
git add app/src/
git commit -m "feat: implement WebViewScreen with favourite toggle and article reading"
```

---

## Task 15: FavouritesScreen

**Files:**
- Create: `app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesViewModel.kt`
- Modify: `app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesScreen.kt`
- Create: `app/src/test/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesViewModelTest.kt`

### Step 1: Write failing test

```kotlin
// app/src/test/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesViewModelTest.kt
package com.pavel.pavelrssreader.presentation.favourites

import app.cash.turbine.test
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetFavouritesUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    private val getFavouritesUseCase = mockk<GetFavouritesUseCase>()
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val favArticle = Article(
        id = 5L, feedId = 1L, guid = "fav-g", title = "Fav Title",
        link = "https://example.com/fav", description = "Fav",
        publishedAt = System.currentTimeMillis(), fetchedAt = System.currentTimeMillis(),
        isFavorite = true
    )

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `favourites StateFlow emits favourite articles`() = runTest {
        every { getFavouritesUseCase() } returns flowOf(listOf(favArticle))
        val vm = FavouritesViewModel(getFavouritesUseCase, toggleFavouriteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.favourites.test {
            assertEquals(listOf(favArticle), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeFavourite calls toggleFavourite with false`() = runTest {
        every { getFavouritesUseCase() } returns flowOf(listOf(favArticle))
        val vm = FavouritesViewModel(getFavouritesUseCase, toggleFavouriteUseCase)
        vm.removeFavourite(5L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(5L, false) }
    }
}
```

### Step 2: Run test to verify it fails

```bash
./gradlew :app:testDebugUnitTest --tests "com.pavel.pavelrssreader.presentation.favourites.FavouritesViewModelTest"
```

Expected: FAIL.

### Step 3: Implement FavouritesViewModel and FavouritesScreen

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesViewModel.kt
package com.pavel.pavelrssreader.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetFavouritesUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    getFavouritesUseCase: GetFavouritesUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    val favourites: StateFlow<List<Article>> = getFavouritesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavourite(articleId: Long) {
        viewModelScope.launch { toggleFavouriteUseCase(articleId, false) }
    }
}
```

```kotlin
// app/src/main/java/com/pavel/pavelrssreader/presentation/favourites/FavouritesScreen.kt
package com.pavel.pavelrssreader.presentation.favourites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.presentation.articles.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel()
) {
    val favourites by viewModel.favourites.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favourites") }) }
    ) { padding ->
        if (favourites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No favourites yet. Tap ☆ on an article to save it.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(favourites, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClick(article.id) },
                        onToggleFavourite = { isFav ->
                            if (!isFav) viewModel.removeFavourite(article.id)
                        }
                    )
                }
            }
        }
    }
}
```

### Step 4: Run all unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All unit tests PASS.

### Step 5: Commit

```bash
git add app/src/
git commit -m "feat: implement FavouritesScreen and FavouritesViewModel"
```

---

## Task 16: Final integration and compile check

### Step 1: Full debug build

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

### Step 2: Run all unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All tests PASS, no failures.

### Step 3: Run instrumented tests on device/emulator

```bash
./gradlew :app:connectedAndroidTest
```

Expected: Room DAO tests pass.

### Step 4: Final commit

```bash
git add .
git commit -m "feat: complete RSS reader app — feeds, articles, WebView, favourites, 24h TTL"
```

---

## Notes and Troubleshooting

### KSP version mismatch
If the build fails with a KSP/Kotlin version mismatch, visit https://github.com/google/ksp/releases and find the exact release for your Kotlin version. Update `ksp` in `libs.versions.toml`.

### Hilt + AGP 9.x
AGP 9.1.0 is cutting-edge. If Hilt annotation processing fails, ensure `hilt` and `ksp` plugin versions are the latest compatible releases. Check https://dagger.dev/hilt/gradle-setup.

### PullToRefreshBox API
`PullToRefreshBox` is the Material3 1.3+ API. If not available in the Compose BOM you've pinned, use `ExperimentalMaterialApi` SwipeRefresh from accompanist as an alternative or upgrade the Compose BOM.

### WebView JavaScript
`settings.javaScriptEnabled = true` enables JavaScript in the WebView. This is required for most modern web articles. If stricter security is needed, disable it — but most RSS article links require JS.
