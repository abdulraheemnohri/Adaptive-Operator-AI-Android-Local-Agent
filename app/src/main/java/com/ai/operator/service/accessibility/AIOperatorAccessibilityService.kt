package com.ai.operator.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.operator.core.agent.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIOperatorAccessibilityService : AccessibilityService() {

    enum class OperatingMode {
        SAFE_MODE_READ_ONLY,
        OPERATOR_MODE_INTERACTIVE
    }

    companion object {
        private val _serviceConnected = MutableStateFlow(false)
        val serviceConnected = _serviceConnected.asStateFlow()

        private val _currentMode = MutableStateFlow(OperatingMode.SAFE_MODE_READ_ONLY)
        val currentMode = _currentMode.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _serviceConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle window changes or scroll events to update contextual information
    }

    override fun onInterrupt() {
        _serviceConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _serviceConnected.value = false
    }

    fun setOperatingMode(mode: OperatingMode) {
        _currentMode.value = mode
    }

    fun getScreenLayoutTreeJson(): String {
        val root = rootInActiveWindow ?: return "{}"
        val json = AccessibilityTreeParser.parseTreeToJson(root)
        root.recycle()
        return json
    }

    fun performClickOnText(targetText: String): Boolean {
        if (_currentMode.value != OperatingMode.OPERATOR_MODE_INTERACTIVE) {
            return false // Restricted under Safe Mode Read Only
        }
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        var clickSuccess = false
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (!clickSuccess && node.isClickable) {
                    clickSuccess = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                node.recycle() // Safely recycles every single element to avoid leaks
            }
        }
        root.recycle()
        return clickSuccess
    }

    fun performBackAction(): Boolean {
        if (_currentMode.value != OperatingMode.OPERATOR_MODE_INTERACTIVE) return false
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHomeAction(): Boolean {
        if (_currentMode.value != OperatingMode.OPERATOR_MODE_INTERACTIVE) return false
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
