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
