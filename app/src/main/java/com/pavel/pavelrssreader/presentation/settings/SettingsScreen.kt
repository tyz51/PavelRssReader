package com.pavel.pavelrssreader.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            FontSizeSection(
                label = "Article title",
                currentSize = state.titleFontSize,
                min = 10f,
                max = 22f,
                previewText = "Article title example",
                onSizeChange = viewModel::setTitleFontSize
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            FontSizeSection(
                label = "Article body",
                currentSize = state.bodyFontSize,
                min = 12f,
                max = 28f,
                previewText = "This is how the article body text will look at this size. " +
                        "Adjust until reading feels comfortable.",
                onSizeChange = viewModel::setBodyFontSize
            )
        }
    }
}

@Composable
private fun FontSizeSection(
    label: String,
    currentSize: Float,
    min: Float,
    max: Float,
    previewText: String,
    onSizeChange: (Float) -> Unit
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = previewText,
        style = remember(currentSize) { TextStyle(fontSize = currentSize.sp) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Slider(
        value = currentSize,
        onValueChange = { onSizeChange(it.roundToInt().toFloat()) },
        valueRange = min..max,
        steps = maxOf(0, (max - min).toInt() - 1),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label font size, ${currentSize.roundToInt()} sp" }
    )
    Text(
        "${currentSize.roundToInt()} sp",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
