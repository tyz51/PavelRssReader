package com.pavel.pavelrssreader.presentation.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.foundation.layout.WindowInsets
import com.pavel.pavelrssreader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (articleId: Long, feedId: Long?) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val markedAsReadText = stringResource(R.string.marked_as_read)
    val undoText = stringResource(R.string.undo)

    // Screen-level dismiss handling — survives item removal from LazyColumn
    data class PendingUndo(val gen: Int, val articleId: Long, val wasFirst: Boolean)
    val listState = rememberLazyListState()
    var pendingUndo by remember { mutableStateOf(PendingUndo(0, 0L, false)) }
    var pendingScrollToId by remember { mutableStateOf<Long?>(null) }

    // Scroll to the restored article once it actually appears in the list
    LaunchedEffect(state.articles) {
        val targetId = pendingScrollToId ?: return@LaunchedEffect
        if (state.articles.any { it.id == targetId }) {
            listState.scrollToItem(0)
            pendingScrollToId = null
        }
    }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo.gen == 0) return@LaunchedEffect
        val id = pendingUndo.articleId
        val wasFirst = pendingUndo.wasFirst
        snackbarHostState.currentSnackbarData?.dismiss()
        try {
            val result = snackbarHostState.showSnackbar(
                message = markedAsReadText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDismiss(id)
                if (wasFirst) pendingScrollToId = id
            } else {
                viewModel.confirmDismiss(id)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            viewModel.confirmDismiss(id)
            throw e
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.news_title)) },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
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
                            contentDescription = stringResource(R.string.search_hint)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (state.articles.isEmpty() && !state.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                LazyColumn(state = listState) {
                    items(state.articles, key = { it.id }) { article ->
                        var dismissCount by remember { mutableIntStateOf(0) }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dismissCount++
                                    true
                                } else false
                            }
                        )
                        LaunchedEffect(dismissCount) {
                            if (dismissCount > 0) {
                                val wasFirst = state.articles.firstOrNull()?.id == article.id
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                viewModel.dismissArticle(article.id)
                                pendingUndo = PendingUndo(pendingUndo.gen + 1, article.id, wasFirst)
                            }
                        }
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
                            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
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

