package com.pavel.pavelrssreader.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _articleId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<WebViewUiState> = _articleId
        .flatMapLatest { id ->
            if (id == null) flowOf(WebViewUiState())
            else getArticlesUseCase()
                .map { articles ->
                    val article = articles.find { it.id == id }
                    WebViewUiState(article = article, isLoading = false)
                }
                .onStart { emit(WebViewUiState(isLoading = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WebViewUiState(isLoading = true))

    fun loadArticle(articleId: Long) {
        if (_articleId.value != articleId) {
            _articleId.value = articleId
            viewModelScope.launch { markAsReadUseCase(articleId) }
        }
    }

    fun toggleFavourite() {
        val current = uiState.value.article ?: return
        viewModelScope.launch {
            toggleFavouriteUseCase(current.id, !current.isFavorite)
        }
    }
}
