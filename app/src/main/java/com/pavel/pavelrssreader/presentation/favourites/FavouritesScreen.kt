package com.pavel.pavelrssreader.presentation.favourites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.presentation.articles.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favourites") }) }
    ) { padding ->
        if (state.favourites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No favourite articles yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(state.favourites, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClick(article.id) },
                        onToggleFavourite = { isFav ->
                            if (!isFav) viewModel.removeFavourite(article.id)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
