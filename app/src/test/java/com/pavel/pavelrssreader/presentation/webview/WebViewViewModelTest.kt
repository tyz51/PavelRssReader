package com.pavel.pavelrssreader.presentation.webview

import app.cash.turbine.test
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {

    private val getArticlesUseCase = mockk<GetArticlesUseCase>()
    private val markAsReadUseCase = mockk<MarkAsReadUseCase>(relaxed = true)
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val sampleArticle = Article(
        id = 1L,
        feedId = 1L,
        guid = "guid1",
        title = "Test Article",
        link = "https://example.com/article",
        description = "Article description",
        publishedAt = System.currentTimeMillis(),
        fetchedAt = System.currentTimeMillis(),
        isRead = false,
        isFavorite = false
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
    fun `loadArticle sets article in state and marks as read`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))

        val vm = WebViewViewModel(getArticlesUseCase, markAsReadUseCase, toggleFavouriteUseCase)
        vm.loadArticle(1L)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.article)
            assertEquals(sampleArticle, state.article)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { markAsReadUseCase(1L) }
    }

    @Test
    fun `toggleFavourite calls use case with negated isFavorite`() = runTest {
        val favoriteArticle = sampleArticle.copy(isFavorite = false)
        every { getArticlesUseCase() } returns flowOf(listOf(favoriteArticle))

        val vm = WebViewViewModel(getArticlesUseCase, markAsReadUseCase, toggleFavouriteUseCase)
        vm.loadArticle(1L)
        advanceUntilIdle()

        vm.toggleFavourite()
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, true) }
    }

    @Test
    fun `toggleFavourite with isFavorite true calls use case with false`() = runTest {
        val favoriteArticle = sampleArticle.copy(isFavorite = true)
        every { getArticlesUseCase() } returns flowOf(listOf(favoriteArticle))

        val vm = WebViewViewModel(getArticlesUseCase, markAsReadUseCase, toggleFavouriteUseCase)
        vm.loadArticle(1L)
        advanceUntilIdle()

        vm.toggleFavourite()
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, false) }
    }
}
