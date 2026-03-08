package com.pavel.pavelrssreader.presentation.webview

import com.pavel.pavelrssreader.data.network.ArticleContentFetcher
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {

    private val getArticlesUseCase = mockk<GetArticlesUseCase>()
    private val markAsReadUseCase = mockk<MarkAsReadUseCase>()
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>()
    private val articleContentFetcher = mockk<ArticleContentFetcher>()

    private val testDispatcher = StandardTestDispatcher()

    private val sampleArticle = Article(
        id = 1L,
        feedId = 1L,
        guid = "guid-1",
        title = "Test Article",
        link = "https://example.com/article",
        description = "Test description",
        publishedAt = 1_000_000L,
        fetchedAt = 1_000_000L,
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

    private fun createViewModel() = WebViewViewModel(
        getArticlesUseCase, markAsReadUseCase, toggleFavouriteUseCase, articleContentFetcher
    )

    @Test
    fun `loadArticle sets article in state and marks as read`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { markAsReadUseCase(1L) } just Runs
        coEvery { articleContentFetcher.fetch(sampleArticle.link) } returns ""

        val viewModel = createViewModel()
        viewModel.loadArticle(1L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.article)
        assertEquals(sampleArticle, viewModel.uiState.value.article)
        coVerify { markAsReadUseCase(1L) }
    }

    @Test
    fun `loadArticle with unknown id leaves article null`() = runTest {
        every { getArticlesUseCase() } returns flowOf(listOf(sampleArticle))
        coEvery { markAsReadUseCase(999L) } just Runs
        coEvery { articleContentFetcher.fetch(sampleArticle.link) } returns ""

        val viewModel = createViewModel()
        viewModel.loadArticle(999L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.article)
    }

    @Test
    fun `toggleFavourite calls use case with negated isFavorite`() = runTest {
        val article = sampleArticle.copy(isFavorite = false)
        every { getArticlesUseCase() } returns flowOf(listOf(article))
        coEvery { markAsReadUseCase(1L) } just Runs
        coEvery { toggleFavouriteUseCase(1L, true) } just Runs
        coEvery { articleContentFetcher.fetch(sampleArticle.link) } returns ""

        val viewModel = createViewModel()
        viewModel.loadArticle(1L)
        advanceUntilIdle()
        viewModel.toggleFavourite()
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, true) }
    }

    @Test
    fun `toggleFavourite with isFavorite true calls use case with false`() = runTest {
        val article = sampleArticle.copy(isFavorite = true)
        every { getArticlesUseCase() } returns flowOf(listOf(article))
        coEvery { markAsReadUseCase(1L) } just Runs
        coEvery { toggleFavouriteUseCase(1L, false) } just Runs
        coEvery { articleContentFetcher.fetch(sampleArticle.link) } returns ""

        val viewModel = createViewModel()
        viewModel.loadArticle(1L)
        advanceUntilIdle()
        viewModel.toggleFavourite()
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, false) }
    }
}
