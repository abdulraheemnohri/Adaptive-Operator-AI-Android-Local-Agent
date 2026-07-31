package com.adaptiveoperator.ai.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 45: the big red "STOP ALL ACTIONS" button. A single shared flag that
 * AgentOrchestrator, ToolRegistry, ScreenCaptureManager, and the Floating Operator
 * all check between steps. Tripping it must cancel the agent loop, cancel any
 * pending tool execution, stop screen capture, and return every screen to idle --
 * this class only holds the signal; each subsystem is responsible for observing it.
 */
@Singleton
class EmergencyStop @Inject constructor() {
    private val _triggered = MutableStateFlow(false)
    val triggered: StateFlow<Boolean> = _triggered.asStateFlow()

    fun trip() {
        _triggered.value = true
    }

    /** Called once every subsystem has unwound and the operator is idle again. */
    fun reset() {
        _triggered.value = false
    }
}
