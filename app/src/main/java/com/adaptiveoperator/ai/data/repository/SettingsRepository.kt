package com.adaptiveoperator.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.adaptiveoperator.ai.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "secure_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _confirmationMode = MutableStateFlow(ConfirmationMode.ASK_FOR_RISKY)
    val confirmationMode: Flow<ConfirmationMode> = _confirmationMode.asStateFlow()

    private val _batteryMode = MutableStateFlow(BatteryMode.BALANCED)
    val batteryMode: Flow<BatteryMode> = _batteryMode.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak: Flow<Boolean> = _autoSpeak.asStateFlow()

    private val _floatingEnabled = MutableStateFlow(true)
    val floatingEnabled: Flow<Boolean> = _floatingEnabled.asStateFlow()

    private val _hfToken = MutableStateFlow<String?>(null)
    val hfToken: Flow<String?> = _hfToken.asStateFlow()

    private val _isTokenValidated = MutableStateFlow(false)
    val isTokenValidated: Flow<Boolean> = _isTokenValidated.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _confirmationMode.value = ConfirmationMode.valueOf(
            prefs.getString(KEY_CONFIRMATION_MODE, ConfirmationMode.ASK_FOR_RISKY.name) ?: ConfirmationMode.ASK_FOR_RISKY.name
        )
        _batteryMode.value = BatteryMode.valueOf(
            prefs.getString(KEY_BATTERY_MODE, BatteryMode.BALANCED.name) ?: BatteryMode.BALANCED.name
        )
        _autoSpeak.value = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        _floatingEnabled.value = prefs.getBoolean(KEY_FLOATING_ENABLED, true)
        
        val token = prefs.getString(KEY_HF_TOKEN, null)
        _hfToken.value = token
        _isTokenValidated.value = prefs.getBoolean(KEY_HF_TOKEN_VALIDATED, false)
    }

    suspend fun setConfirmationMode(mode: ConfirmationMode) {
        _confirmationMode.value = mode
        prefs.edit().putString(KEY_CONFIRMATION_MODE, mode.name).apply()
    }

    suspend fun setBatteryMode(mode: BatteryMode) {
        _batteryMode.value = mode
        prefs.edit().putString(KEY_BATTERY_MODE, mode.name).apply()
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
    }

    suspend fun setFloatingEnabled(enabled: Boolean) {
        _floatingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FLOATING_ENABLED, enabled).apply()
    }

    suspend fun saveHuggingFaceToken(token: String): Result<Unit> {
        return try {
            // Basic format validation (starts with hf_)
            if (!token.trim().startsWith("hf_")) {
                return Result.Error("Invalid token format. Must start with 'hf_'")
            }
            
            prefs.edit()
                .putString(KEY_HF_TOKEN, token.trim())
                .putBoolean(KEY_HF_TOKEN_VALIDATED, true)
                .apply()
            
            _hfToken.value = token.trim()
            _isTokenValidated.value = true
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save token: ${e.message}")
        }
    }

    suspend fun deleteHuggingFaceToken() {
        prefs.edit()
            .remove(KEY_HF_TOKEN)
            .remove(KEY_HF_TOKEN_VALIDATED)
            .apply()
        _hfToken.value = null
        _isTokenValidated.value = false
    }

    suspend fun validateToken(): Result<Boolean> {
        val token = _hfToken.value
        if (token.isNullOrBlank()) {
            return Result.Error("No token saved")
        }
        
        // In V1, we assume valid if format is correct since real validation requires network call
        // Real implementation would do a lightweight API call to huggingface.co/api/whoami-v2
        return Result.Success(true)
    }

    companion object {
        private const val KEY_CONFIRMATION_MODE = "confirmation_mode"
        private const val KEY_BATTERY_MODE = "battery_mode"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_FLOATING_ENABLED = "floating_enabled"
        private const val KEY_HF_TOKEN = "hf_token"
        private const val KEY_HF_TOKEN_VALIDATED = "hf_token_validated"
    }
}

enum class ConfirmationMode { 
    ASK_FOR_RISKY, 
    ASK_FOR_EVERY_ACTION, 
    AUTONOMOUS_LOW_RISK 
}

enum class BatteryMode { 
    MAXIMUM_BATTERY, 
    BALANCED, 
    PERFORMANCE 
}
