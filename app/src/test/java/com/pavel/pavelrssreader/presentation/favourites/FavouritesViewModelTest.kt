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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    private val getFavouritesUseCase = mockk<GetFavouritesUseCase>()
    private val toggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleArticle = Article(
        id = 1L, feedId = 1L, guid = "g1", title = "Favourite Article",
        link = "https://example.com", description = "Desc",
        publishedAt = 1_000_000L, fetchedAt = 1_000_000L,
        isFavorite = true
    )

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has empty favourites`() = runTest {
        every { getFavouritesUseCase() } returns flowOf(emptyList())

        val vm = FavouritesViewModel(getFavouritesUseCase, toggleFavouriteUseCase)

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.favourites.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favourites are loaded from use case`() = runTest {
        every { getFavouritesUseCase() } returns flowOf(listOf(sampleArticle))

        val vm = FavouritesViewModel(getFavouritesUseCase, toggleFavouriteUseCase)
        advanceUntilIdle()

        assertEquals(listOf(sampleArticle), vm.uiState.value.favourites)
    }

    @Test
    fun `removeFavourite calls toggleFavouriteUseCase with false`() = runTest {
        every { getFavouritesUseCase() } returns flowOf(listOf(sampleArticle))

        val vm = FavouritesViewModel(getFavouritesUseCase, toggleFavouriteUseCase)
        vm.removeFavourite(1L)
        advanceUntilIdle()

        coVerify { toggleFavouriteUseCase(1L, false) }
    }
}
