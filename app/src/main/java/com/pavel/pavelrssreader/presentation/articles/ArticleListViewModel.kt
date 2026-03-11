package com.pavel.pavelrssreader.presentation.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.GetFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.RefreshFeedsUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val feeds: List<Feed> = emptyList(),
    val selectedFeedId: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val getFeedsUseCase: GetFeedsUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val refreshFeedsUseCase: RefreshFeedsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private val _selectedFeedId = MutableStateFlow<Long?>(null)
    private val _hiddenArticleIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        combine(
            getArticlesUseCase(),
            getFeedsUseCase(),
            _selectedFeedId,
            _hiddenArticleIds
        ) { articles, feeds, selectedFeedId, hiddenIds ->
            val unread = articles.filter { !it.isRead && it.id !in hiddenIds }
            val filtered = if (selectedFeedId == null) unread
                           else unread.filter { it.feedId == selectedFeedId }
            Triple(filtered, feeds, selectedFeedId)
        }
        .onEach { (articles, feeds, selectedFeedId) ->
            _uiState.update { it.copy(
                articles = articles,
                feeds = feeds,
                selectedFeedId = selectedFeedId
            )}
        }
        .launchIn(viewModelScope)
    }

    fun selectFeed(feedId: Long?) {
        _selectedFeedId.value = feedId
    }

    fun dismissArticle(articleId: Long) {
        _hiddenArticleIds.update { it + articleId }
    }

    fun undoDismiss(articleId: Long) {
        _hiddenArticleIds.update { it - articleId }
    }

    fun confirmDismiss(articleId: Long) {
        _hiddenArticleIds.update { it - articleId }
        viewModelScope.launch { markAsReadUseCase(articleId) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshFeedsUseCase()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    errorMessage = (result as? Result.Error)?.message
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
