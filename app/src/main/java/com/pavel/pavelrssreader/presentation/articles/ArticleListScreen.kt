package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavel.pavelrssreader.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (articleId: Long, feedId: Long?) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val markedAsReadText = stringResource(R.string.marked_as_read)
    val undoText = stringResource(R.string.undo)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.news_title)) },
                actions = {
                    // Source filter dropdown
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter_by_source),
                                tint = if (state.selectedFeedId != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    LocalContentColor.current
                            )
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_all)) },
                                onClick = {
                                    viewModel.selectFeed(null)
                                    filterMenuExpanded = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = state.selectedFeedId == null,
                                        onClick = null
                                    )
                                }
                            )
                            state.feeds.forEach { feed ->
                                DropdownMenuItem(
                                    text = { Text(feed.title) },
                                    onClick = {
                                        viewModel.selectFeed(feed.id)
                                        filterMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = state.selectedFeedId == feed.id,
                                            onClick = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                    // Search (existing placeholder)
                    IconButton(onClick = { /* search placeholder */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.articles.isEmpty() && !state.isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.selectedFeedId != null)
                        stringResource(R.string.no_articles_filtered)
                    else
                        stringResource(R.string.no_articles_empty)
                )
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            ) {
                LazyColumn {
                    items(state.articles, key = { it.id }) { article ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.dismissArticle(article.id)
                                    coroutineScope.launch {
                                        try {
                                            val result = snackbarHostState.showSnackbar(
                                                message = markedAsReadText,
                                                actionLabel = undoText,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDismiss(article.id)
                                            } else {
                                                viewModel.confirmDismiss(article.id)
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            viewModel.confirmDismiss(article.id)
                                            throw e
                                        }
                                    }
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        ) {
                            Column {
                                ArticleCard(
                                    article = article,
                                    onClick = { onArticleClick(article.id, state.selectedFeedId) },
                                    onToggleFavourite = { isFav ->
                                        viewModel.toggleFavourite(article.id, isFav)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
