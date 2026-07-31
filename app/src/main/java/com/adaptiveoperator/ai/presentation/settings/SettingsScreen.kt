package com.adaptiveoperator.ai.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ttsSettings by viewModel.ttsManager.settings.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text("Voice Output", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row("Enabled", ttsSettings.enabled) { enabled ->
                        viewModel.ttsManager.updateSettings { it.copy(enabled = enabled) }
                    }
                    Text("Speech rate: ${"%.1f".format(ttsSettings.speechRate)}")
                    Slider(
                        value = ttsSettings.speechRate,
                        onValueChange = { rate -> viewModel.ttsManager.updateSettings { it.copy(speechRate = rate) } },
                        valueRange = 0.8f..1.5f
                    )
                    Text("Pitch: ${"%.1f".format(ttsSettings.pitch)}")
                    Slider(
                        value = ttsSettings.pitch,
                        onValueChange = { pitch -> viewModel.ttsManager.updateSettings { it.copy(pitch = pitch) } },
                        valueRange = 0.8f..1.5f
                    )
                }
            }

            Text("Privacy Center", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("AI Processing: ● On Device")
                    Text("Screenshots: ● Local")
                    Text("Memory: ● Local")
                    Text("Cloud AI: ● Disabled")
                }
            }
        }
    }
}

@Composable
private fun Row(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
