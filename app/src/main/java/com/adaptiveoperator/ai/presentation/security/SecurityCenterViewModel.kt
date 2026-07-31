package com.adaptiveoperator.ai.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptiveoperator.ai.security.BlocklistManager
import com.adaptiveoperator.ai.security.ConfirmationMode
import com.adaptiveoperator.ai.security.PermissionManager
import com.adaptiveoperator.ai.security.SecurityPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val accessibilityEnabled: Boolean = false,
    val overlayGranted: Boolean = false,
    val blockedAppCount: Int = 0,
    val confirmationMode: ConfirmationMode = ConfirmationMode.ASK_FOR_RISKY
)

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val blocklistManager: BlocklistManager,
    private val securityPolicy: SecurityPolicy
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            blocklistManager.load()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val permissions = permissionManager.snapshot()
            _uiState.value = SecurityUiState(
                accessibilityEnabled = permissions.accessibilityEnabled,
                overlayGranted = permissions.overlayGranted,
                blockedAppCount = blocklistManager.blockedPackages.value.size,
                confirmationMode = securityPolicy.currentMode()
            )
        }
    }

    fun setConfirmationMode(mode: ConfirmationMode) = viewModelScope.launch {
        securityPolicy.setMode(mode)
        refresh()
    }

    fun accessibilitySettingsIntent() = permissionManager.accessibilitySettingsIntent()
    fun overlaySettingsIntent() = permissionManager.overlaySettingsIntent()
}
