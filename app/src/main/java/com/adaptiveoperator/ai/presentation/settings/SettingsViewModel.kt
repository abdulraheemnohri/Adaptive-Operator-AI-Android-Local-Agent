package com.adaptiveoperator.ai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptiveoperator.ai.android.voice.TextToSpeechManager
import com.adaptiveoperator.ai.data.repository.BatteryMode
import com.adaptiveoperator.ai.data.repository.ConfirmationMode
import com.adaptiveoperator.ai.data.repository.SettingsRepository
import com.adaptiveoperator.ai.core.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val confirmationMode: ConfirmationMode = ConfirmationMode.ASK_FOR_RISKY,
    val batteryMode: BatteryMode = BatteryMode.BALANCED,
    val autoSpeak: Boolean = true,
    val floatingEnabled: Boolean = true,
    val hfToken: String? = null,
    val isTokenValidated: Boolean = false,
    val tokenSaveLoading: Boolean = false,
    val tokenSaveError: String? = null,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = kotlinx.coroutines.flow.combine(
        settingsRepository.confirmationMode,
        settingsRepository.batteryMode,
        settingsRepository.autoSpeak,
        settingsRepository.floatingEnabled,
        settingsRepository.hfToken,
        settingsRepository.isTokenValidated,
        ttsManager.settings
    ) { confirmationMode, batteryMode, autoSpeak, floatingEnabled, hfToken, isTokenValidated, ttsSettings ->
        SettingsUiState(
            confirmationMode = confirmationMode,
            batteryMode = batteryMode,
            autoSpeak = autoSpeak,
            floatingEnabled = floatingEnabled,
            hfToken = hfToken,
            isTokenValidated = isTokenValidated,
            ttsSpeechRate = ttsSettings.speechRate,
            ttsPitch = ttsSettings.pitch,
            ttsEnabled = ttsSettings.enabled
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    fun setConfirmationMode(mode: ConfirmationMode) {
        viewModelScope.launch {
            settingsRepository.setConfirmationMode(mode)
        }
    }

    fun setBatteryMode(mode: BatteryMode) {
        viewModelScope.launch {
            settingsRepository.setBatteryMode(mode)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSpeak(enabled)
        }
    }

    fun setFloatingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingEnabled(enabled)
        }
    }

    fun saveHuggingFaceToken(token: String) {
        viewModelScope.launch {
            settingsRepository.saveHuggingFaceToken(token)
        }
    }

    fun deleteHuggingFaceToken() {
        viewModelScope.launch {
            settingsRepository.deleteHuggingFaceToken()
        }
    }

    fun validateToken() {
        viewModelScope.launch {
            settingsRepository.validateToken()
        }
    }

    fun updateTtsSettings(speechRate: Float, pitch: Float, enabled: Boolean) {
        ttsManager.updateSettings { 
            it.copy(speechRate = speechRate, pitch = pitch, enabled = enabled)
        }
    }
}
