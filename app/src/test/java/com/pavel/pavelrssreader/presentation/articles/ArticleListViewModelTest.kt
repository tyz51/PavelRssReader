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
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `articles StateFlow emits articles from use case`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
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
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `toggleFavourite calls use case with correct arguments`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { refreshFeedsUseCase() } returns Result.Success(Unit)

        val vm = ArticleListViewModel(getArticlesUseCase, refreshFeedsUseCase, toggleFavouriteUseCase)
        vm.toggleFavourite(1L, true)
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, true) }
    }
}
