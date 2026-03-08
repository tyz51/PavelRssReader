package com.pavel.pavelrssreader.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.usecase.GetFavouritesUseCase
import com.pavel.pavelrssreader.domain.usecase.ToggleFavouriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavouritesUiState(
    val favourites: List<Article> = emptyList()
)

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getFavouritesUseCase: GetFavouritesUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    init {
        getFavouritesUseCase()
            .onEach { favourites -> _uiState.update { it.copy(favourites = favourites) } }
            .launchIn(viewModelScope)
    }

    fun removeFavourite(articleId: Long) {
        viewModelScope.launch { toggleFavouriteUseCase(articleId, false) }
    }
}
