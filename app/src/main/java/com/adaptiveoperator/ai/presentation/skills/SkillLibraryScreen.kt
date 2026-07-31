package com.adaptiveoperator.ai.presentation.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SkillLibraryScreen(viewModel: SkillLibraryViewModel = hiltViewModel()) {
    val skills by viewModel.skills.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("My Skills") }) }) { padding ->
        if (skills.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(
                    "No skills learned yet. Repeat the same request a few times and Adaptive " +
                        "Operator AI will turn it into a reusable skill automatically (Section 26).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(skills, key = { it.id }) { skill ->
                    Card(Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("⭐ ${skill.name}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${"%.0f".format(skill.successRate * 100)}% success · ${skill.confidenceLabel} confidence",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { viewModel.delete(skill.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete skill")
                            }
                        }
                    }
                }
            }
        }
    }
}
