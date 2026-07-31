package com.adaptiveoperator.ai.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adaptiveoperator.ai.ai.runtime.ModelInstallState

private data class DashboardCard(val title: String, val body: String, val actionLabel: String?, val onAction: (() -> Unit)?)

@Composable
fun HomeScreen(
    onOpenModelManager: () -> Unit,
    onOpenOperator: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adaptive Operator AI") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        val cards = listOf(
            DashboardCard(
                title = "AI STATUS",
                body = "${state.modelDisplayName}\n${state.modelInstallState.readableLabel()}",
                actionLabel = if (state.modelInstallState != ModelInstallState.READY) "Set up model" else null,
                onAction = if (state.modelInstallState != ModelInstallState.READY) onOpenModelManager else null
            ),
            DashboardCard(
                title = "RUNTIME",
                body = "Backend: ${state.backendLabel}",
                actionLabel = null,
                onAction = null
            ),
            DashboardCard(
                title = "MEMORY",
                body = "${state.experienceCount} tasks recorded\n${state.skillCount} learned skills",
                actionLabel = "View skills",
                onAction = onOpenSkills
            ),
            DashboardCard(
                title = "OPERATOR",
                body = if (state.operatorReady) "Ready" else "Model not ready",
                actionLabel = if (state.operatorReady) "Open Operator" else null,
                onAction = if (state.operatorReady) onOpenOperator else null
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card -> DashboardCardView(card) }
        }
    }
}

@Composable
private fun DashboardCardView(card: DashboardCard) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(card.title, style = MaterialTheme.typography.labelSmall)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            Text(card.body, style = MaterialTheme.typography.bodyLarge)
            card.actionLabel?.let { label ->
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                Button(onClick = { card.onAction?.invoke() }) { Text(label) }
            }
        }
    }
}

private fun ModelInstallState.readableLabel(): String = when (this) {
    ModelInstallState.NOT_DOWNLOADED -> "Not installed"
    ModelInstallState.DOWNLOADING -> "Downloading..."
    ModelInstallState.PAUSED -> "Download paused"
    ModelInstallState.VERIFYING -> "Verifying..."
    ModelInstallState.INSTALLING -> "Installing..."
    ModelInstallState.WARMING_UP -> "Warming up..."
    ModelInstallState.READY -> "● Ready"
    ModelInstallState.FAILED -> "Failed"
    ModelInstallState.CORRUPTED -> "Corrupted -- redownload needed"
}
