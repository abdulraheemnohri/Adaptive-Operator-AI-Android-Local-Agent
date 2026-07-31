package com.adaptiveoperator.ai.presentation.modelmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adaptiveoperator.ai.ai.runtime.ModelInstallState

@Composable
fun ModelManagerScreen(viewModel: ModelManagerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var hfToken by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("AI Model") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(state.spec.displayName, style = MaterialTheme.typography.titleLarge)
            Text("Runtime: ${state.spec.runtime}", style = MaterialTheme.typography.bodyMedium)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

            when (state.installState) {
                ModelInstallState.NOT_DOWNLOADED -> {
                    Text(
                        "This model is gated on Hugging Face -- paste a read-scoped access token to download it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = hfToken,
                        onValueChange = { hfToken = it },
                        label = { Text("Hugging Face access token") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = { viewModel.startDownload(hfToken.ifBlank { null }) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Download AI Model") }
                }

                ModelInstallState.DOWNLOADING -> {
                    val progress = state.progress
                    Text("Download", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Text("${"%.0f".format(progress.fraction * 100)}%")
                    Text("${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}")
                    Text("Speed: ${formatBytes(progress.bytesPerSecond)}/s")
                    if (progress.etaSeconds >= 0) Text("ETA: ${progress.etaSeconds}s")
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::pause) { Text("Pause") }
                        OutlinedButton(onClick = viewModel::cancel) { Text("Cancel") }
                    }
                }

                ModelInstallState.PAUSED -> {
                    Text("Download paused -- ${formatBytes(state.progress.bytesDownloaded)} / ${formatBytes(state.progress.totalBytes)}")
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::resume) { Text("Resume") }
                        OutlinedButton(onClick = viewModel::cancel) { Text("Cancel") }
                    }
                }

                ModelInstallState.VERIFYING -> Text("Verifying checksum...")
                ModelInstallState.INSTALLING -> Text("Installing...")
                ModelInstallState.WARMING_UP -> {
                    Text("Preparing Operator...")
                    Text("Loading AI runtime and running a warm-up inference.")
                }

                ModelInstallState.READY -> {
                    Text("Status: ● Installed", style = MaterialTheme.typography.titleMedium)
                    Text("Backend: ${state.backendInUse?.name ?: "Automatic"}")
                    Text("Context length: ${state.spec.contextLength} tokens")
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::remove) { Text("Remove") }
                    }
                }

                ModelInstallState.FAILED, ModelInstallState.CORRUPTED -> {
                    Text(
                        state.errorMessage ?: "Something went wrong installing the model.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.startDownload(hfToken.ifBlank { null }) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Retry") }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
