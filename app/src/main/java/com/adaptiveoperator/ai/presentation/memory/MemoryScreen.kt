package com.adaptiveoperator.ai.presentation.memory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val tasks by viewModel.recentTasks.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("Task History") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(tasks, key = { it.id }) { task ->
                Card(Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        val icon = when (task.status) { "SUCCESS" -> "✓"; "FAILED" -> "✕"; else -> "●" }
                        Text("$icon ${task.requestText}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${task.status}" + (task.durationMs?.let { " · ${it}ms" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
