package com.adaptiveoperator.ai.presentation.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adaptiveoperator.ai.security.ConfirmationMode

@Composable
fun SecurityCenterScreen(viewModel: SecurityCenterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Security Center") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SecurityRow("Accessibility", state.accessibilityEnabled) {
                context.startActivity(viewModel.accessibilitySettingsIntent())
            }
            SecurityRow("Overlay", state.overlayGranted) {
                context.startActivity(viewModel.overlaySettingsIntent())
            }
            Text("Blocked apps: ${state.blockedAppCount}", modifier = Modifier.padding(vertical = 12.dp))

            Text("Risk Policy", style = MaterialTheme.typography.titleMedium)
            ConfirmationMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    androidx.compose.material3.RadioButton(
                        selected = state.confirmationMode == mode,
                        onClick = { viewModel.setConfirmationMode(mode) }
                    )
                    Text(mode.readableLabel(), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(label: String, enabled: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label)
        if (enabled) Text("● Enabled", color = MaterialTheme.colorScheme.primary)
        else Button(onClick = onFix) { Text("Enable") }
    }
}

private fun ConfirmationMode.readableLabel(): String = when (this) {
    ConfirmationMode.ASK_FOR_RISKY -> "Ask for risky actions"
    ConfirmationMode.ASK_FOR_EVERY_ACTION -> "Ask for every action"
    ConfirmationMode.AUTONOMOUS_LOW_RISK -> "Autonomous low-risk actions"
}
