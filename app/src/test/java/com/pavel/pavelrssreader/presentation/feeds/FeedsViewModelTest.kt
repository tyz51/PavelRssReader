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
import kotlinx.coroutines.test.advanceUntilIdle
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

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(feeds, state.feeds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addFeed success clears error`() = runTest {
        every { getFeedsUseCase() } returns flowOf(emptyList())
        val feed = Feed(id = 1L, url = "https://b.com/rss", title = "B", addedAt = 0L)
        coEvery { addFeedUseCase("https://b.com/rss") } returns Result.Success(feed)

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        vm.addFeed("https://b.com/rss")
        advanceUntilIdle()

        assertNull(vm.uiState.value.addFeedError)
    }

    @Test
    fun `addFeed error sets error message`() = runTest {
        every { getFeedsUseCase() } returns flowOf(emptyList())
        coEvery { addFeedUseCase("bad") } returns Result.Error("Invalid URL: bad")

        val vm = FeedsViewModel(getFeedsUseCase, addFeedUseCase, deleteFeedUseCase)
        vm.addFeed("bad")
        advanceUntilIdle()

        assertEquals("Invalid URL: bad", vm.uiState.value.addFeedError)
    }
}
