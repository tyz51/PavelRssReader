package com.pavel.pavelrssreader.presentation.articles

import app.cash.turbine.test
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {

    private val getArticlesUseCase = mockk<GetArticlesUseCase>()
    private val getFeedsUseCase = mockk<GetFeedsUseCase>()
    private val markAsReadUseCase = mockk<MarkAsReadUseCase>(relaxed = true)
    private val refreshFeedsUseCase = mockk<RefreshFeedsUseCase>()
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleArticle = Article(
        id = 1L, feedId = 1L, guid = "g1", title = "Test Article",
        link = "https://example.com", description = "Desc",
        publishedAt = System.currentTimeMillis(), fetchedAt = System.currentTimeMillis()
    )

    private val sampleFeed = Feed(id = 1L, url = "https://example.com/rss", title = "Example", addedAt = 0L)

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `articles StateFlow emits articles from use case`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.articles.contains(sampleArticle))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh sets isRefreshing false after completion`() = runTest {
        every { getArticlesUseCase() } returns flowOf(emptyList())
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `toggleFavourite calls use case with correct arguments`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.toggleFavourite(1L, true)
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, true) }
    }

    @Test
    fun `selectFeed filters articles to chosen feedId`() = runTest {
        val article1 = sampleArticle.copy(id = 1L, feedId = 1L)
        val article2 = sampleArticle.copy(id = 2L, feedId = 2L)
        every { getArticlesUseCase() } returns flowOf(listOf(article1, article2))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        advanceUntilIdle()
        vm.selectFeed(1L)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.articles.size)
            assertEquals(1L, state.articles.first().feedId)
            assertEquals(1L, state.selectedFeedId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectFeed null restores all articles`() = runTest {
        val article1 = sampleArticle.copy(id = 1L, feedId = 1L)
        val article2 = sampleArticle.copy(id = 2L, feedId = 2L)
        every { getArticlesUseCase() } returns flowOf(listOf(article1, article2))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.selectFeed(1L)
        advanceUntilIdle()
        vm.selectFeed(null)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.articles.size)
            assertNull(state.selectedFeedId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissArticle hides article from list`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        advanceUntilIdle()
        vm.dismissArticle(sampleArticle.id)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.articles.none { it.id == sampleArticle.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undoDismiss restores dismissed article`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        advanceUntilIdle()
        vm.dismissArticle(sampleArticle.id)
        advanceUntilIdle()
        vm.undoDismiss(sampleArticle.id)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.articles.any { it.id == sampleArticle.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDismiss calls markAsReadUseCase`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        every { getFeedsUseCase() } returns flowOf(listOf(sampleFeed))

        val vm = ArticleListViewModel(getArticlesUseCase, getFeedsUseCase, markAsReadUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.confirmDismiss(sampleArticle.id)
        advanceUntilIdle()

        coVerify { markAsReadUseCase(sampleArticle.id) }
    }
}
