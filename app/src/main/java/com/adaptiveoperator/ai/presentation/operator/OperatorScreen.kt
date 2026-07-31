package com.adaptiveoperator.ai.presentation.operator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.adaptiveoperator.ai.presentation.components.StatusIndicator

@Composable
fun OperatorScreen(viewModel: OperatorViewModel = hiltViewModel()) {
    val agentState by viewModel.agentState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Operator") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Task", style = MaterialTheme.typography.labelSmall)
            Text(agentState.taskDescription ?: "No active task", style = MaterialTheme.typography.titleMedium)

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            Text("Plan", style = MaterialTheme.typography.labelSmall)
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(agentState.plan) { step ->
                    Text((if (step.done) "✓ " else "● ") + step.description)
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            Text("AI State", style = MaterialTheme.typography.labelSmall)
            StatusIndicator(agentState.status)

            agentState.lastMessage?.let {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            Button(
                onClick = viewModel::emergencyStop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("🛑 STOP") }
        }
    }
}
