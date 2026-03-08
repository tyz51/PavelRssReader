package com.pavel.pavelrssreader.presentation.feeds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(viewModel: FeedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Feeds") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add feed")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.feeds, key = { it.id }) { feed ->
                ListItem(
                    headlineContent = { Text(feed.title) },
                    supportingContent = {
                        Text(feed.url, style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteFeed(feed.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete feed")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        AddFeedDialog(
            urlInput = urlInput,
            onUrlChange = {
                urlInput = it
                viewModel.clearAddFeedError()
            },
            errorMessage = state.addFeedError,
            isLoading = state.isLoading,
            onConfirm = { viewModel.addFeed(urlInput) },
            onDismiss = {
                showDialog = false
                urlInput = ""
                viewModel.clearAddFeedError()
            }
        )
    }

    // Close dialog on successful add (feeds list grew)
    LaunchedEffect(state.feeds.size) {
        if (showDialog && !state.isLoading && state.addFeedError == null && urlInput.isNotBlank()) {
            showDialog = false
            urlInput = ""
        }
    }
}

@Composable
private fun AddFeedDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RSS Feed") },
        text = {
            Column {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading && urlInput.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
