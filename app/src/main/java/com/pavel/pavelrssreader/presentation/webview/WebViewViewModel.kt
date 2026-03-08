package com.pavel.pavelrssreader.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebViewUiState(val article: Article? = null)

@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    fun loadArticle(articleId: Long) {
        viewModelScope.launch {
            val article = getArticlesUseCase()
                .first { list -> list.any { it.id == articleId } }
                .find { it.id == articleId }
            markAsReadUseCase(articleId)
            _uiState.update { it.copy(article = article) }
        }
    }

    fun toggleFavourite() {
        val current = _uiState.value.article ?: return
        viewModelScope.launch {
            toggleFavouriteUseCase(current.id, !current.isFavorite)
        }
    }
}
