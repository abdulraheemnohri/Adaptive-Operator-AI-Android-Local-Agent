package com.adaptiveoperator.ai.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adaptiveoperator.ai.data.repository.BatteryMode
import com.adaptiveoperator.ai.data.repository.ConfirmationMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // AI Runtime Settings
            SettingsSection(title = "AI Runtime") {
                HuggingFaceTokenCard(
                    hfToken = uiState.hfToken,
                    isTokenValidated = uiState.isTokenValidated,
                    onSaveToken = { token -> viewModel.saveHuggingFaceToken(token) },
                    onDeleteToken = { viewModel.deleteHuggingFaceToken() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Operator Settings
            SettingsSection(title = "Operator") {
                ConfirmationModeCard(
                    currentMode = uiState.confirmationMode,
                    onModeChanged = { viewModel.setConfirmationMode(it) }
                )
                
                Spacer(Modifier.height(12.dp))
                
                BatteryModeCard(
                    currentMode = uiState.batteryMode,
                    onModeChanged = { viewModel.setBatteryMode(it) }
                )
                
                Spacer(Modifier.height(12.dp))
                
                FloatingOperatorCard(
                    enabled = uiState.floatingEnabled,
                    onEnabledChanged = { viewModel.setFloatingEnabled(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Voice Settings
            SettingsSection(title = "Voice Output") {
                VoiceOutputCard(
                    enabled = uiState.ttsEnabled,
                    speechRate = uiState.ttsSpeechRate,
                    pitch = uiState.ttsPitch,
                    autoSpeak = uiState.autoSpeak,
                    onEnabledChanged = { viewModel.updateTtsSettings(uiState.ttsSpeechRate, uiState.ttsPitch, it) },
                    onAutoSpeakChanged = { viewModel.setAutoSpeak(it) },
                    onSpeechRateChanged = { viewModel.updateTtsSettings(it, uiState.ttsPitch, uiState.ttsEnabled) },
                    onPitchChanged = { viewModel.updateTtsSettings(uiState.ttsSpeechRate, it, uiState.ttsEnabled) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Privacy Center
            SettingsSection(title = "Privacy Center") {
                PrivacyCard()
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun HuggingFaceTokenCard(
    hfToken: String?,
    isTokenValidated: Boolean,
    onSaveToken: (String) -> Unit,
    onDeleteToken: () -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isTokenValidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hugging Face Token",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (isTokenValidated && hfToken != null) 
                            "Token configured ✓" else "Required for model download",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (hfToken == null) {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Enter Token (starts with hf_)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide" else "Show"
                            )
                        }
                    }
                )
                
                Spacer(Modifier.height(12.dp))
                
                Button(
                    onClick = { 
                        if (tokenInput.trim().startsWith("hf_")) {
                            onSaveToken(tokenInput.trim())
                            tokenInput = ""
                        }
                    },
                    enabled = tokenInput.trim().startsWith("hf_"),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Token")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Token: hf_••••••••••••••••",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDeleteToken) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Token",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Get your free token from huggingface.co/settings/tokens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfirmationModeCard(
    currentMode: ConfirmationMode,
    onModeChanged: (ConfirmationMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Confirmation Mode",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            ConfirmationMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            ConfirmationMode.ASK_FOR_RISKY -> "Ask for risky actions"
                            ConfirmationMode.ASK_FOR_EVERY_ACTION -> "Ask for every action"
                            ConfirmationMode.AUTONOMOUS_LOW_RISK -> "Autonomous (low-risk only)"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeChanged(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryModeCard(
    currentMode: BatteryMode,
    onModeChanged: (BatteryMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Battery Management",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            BatteryMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (mode) {
                                BatteryMode.MAXIMUM_BATTERY -> "Maximum Battery"
                                BatteryMode.BALANCED -> "Balanced"
                                BatteryMode.PERFORMANCE -> "Performance"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (mode) {
                                BatteryMode.MAXIMUM_BATTERY -> "Quick unload, minimal capture"
                                BatteryMode.BALANCED -> "Normal operation"
                                BatteryMode.PERFORMANCE -> "Keep model warm"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeChanged(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingOperatorCard(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Floating Operator",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Show floating bubble overlay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )
        }
    }
}

@Composable
private fun VoiceOutputCard(
    enabled: Boolean,
    speechRate: Float,
    pitch: Float,
    autoSpeak: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onAutoSpeakChanged: (Boolean) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TTS Enabled",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChanged
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto Speak Responses",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = onAutoSpeakChanged
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Speech Rate: ${"%.1f".format(speechRate)}")
            Slider(
                value = speechRate,
                onValueChange = onSpeechRateChanged,
                valueRange = 0.5f..2.0f,
                steps = 14
            )
            
            Text("Pitch: ${"%.1f".format(pitch)}")
            Slider(
                value = pitch,
                onValueChange = onPitchChanged,
                valueRange = 0.5f..2.0f,
                steps = 14
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PrivacyRow(label = "AI Processing", value = "● On Device")
            PrivacyRow(label = "Screenshots", value = "● Local Only")
            PrivacyRow(label = "Memory", value = "● Local Database")
            PrivacyRow(label = "Tasks & Skills", value = "● Local Only")
            PrivacyRow(label = "Cloud AI", value = "● Disabled")
            PrivacyRow(label = "API Calls", value = "● None (Runtime)")
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "All AI inference happens on your device. No data leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
