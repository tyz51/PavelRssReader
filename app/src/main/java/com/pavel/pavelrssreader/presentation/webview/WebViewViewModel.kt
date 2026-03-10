package com.pavel.pavelrssreader.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.data.network.ArticleContentFetcher
import com.pavel.pavelrssreader.data.parser.HtmlToBlocks
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import com.pavel.pavelrssreader.domain.usecase.GetArticlesUseCase
import com.pavel.pavelrssreader.domain.usecase.MarkAsReadUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebViewUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val contentBlocks: List<ContentBlock> = emptyList(),
    val titleFontSize: Float = SettingsRepository.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepository.DEFAULT_BODY_FONT_SIZE
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val articleContentFetcher: ArticleContentFetcher,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _articleId = MutableStateFlow<Long?>(null)
    private val _contentBlocks = MutableStateFlow<List<ContentBlock>>(emptyList())

    private val _articleFlow = _articleId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else getArticlesUseCase().map { articles -> articles.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<WebViewUiState> = combine(
        combine(_articleId, _articleFlow, _contentBlocks) { id, article, blocks ->
            Triple(id, article, blocks)
        },
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize
    ) { (id, article, contentBlocks), titleSize, bodySize ->
        when {
            id == null -> WebViewUiState(titleFontSize = titleSize, bodyFontSize = bodySize)
            article == null -> WebViewUiState(isLoading = true, titleFontSize = titleSize, bodyFontSize = bodySize)
            else -> WebViewUiState(
                article = article,
                isLoading = false,
                contentBlocks = contentBlocks,
                titleFontSize = titleSize,
                bodyFontSize = bodySize
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WebViewUiState(isLoading = true))

    init {
        viewModelScope.launch {
            _articleFlow
                .mapNotNull { it }
                .distinctUntilChanged { old, new -> old.link == new.link }
                .collect { article ->
                    _contentBlocks.value = emptyList()
                    val fetched = articleContentFetcher.fetch(article.link)
                    val html = fetched.ifBlank { article.description ?: "" }
                    _contentBlocks.value = HtmlToBlocks.parse(html)
                }
        }
    }

    fun loadArticle(articleId: Long) {
        if (_articleId.value != articleId) {
            _contentBlocks.value = emptyList()
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
