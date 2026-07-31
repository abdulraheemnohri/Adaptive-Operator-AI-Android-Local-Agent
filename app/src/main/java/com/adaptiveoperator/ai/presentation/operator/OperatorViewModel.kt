package com.adaptiveoperator.ai.presentation.operator

import androidx.lifecycle.ViewModel
import com.adaptiveoperator.ai.agent.AgentOrchestrator
import com.adaptiveoperator.ai.security.EmergencyStop
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OperatorViewModel @Inject constructor(
    private val agentOrchestrator: AgentOrchestrator,
    private val emergencyStop: EmergencyStop
) : ViewModel() {

    val agentState = agentOrchestrator.state

    /** Section 45: cancels the agent, cancels pending tools, and returns to idle.
     *  Screen capture / accessibility teardown is each subsystem's own responsibility
     *  reacting to [EmergencyStop.triggered]; this call just raises the flag and stops
     *  the orchestrator's own loop immediately rather than waiting a step. */
    fun emergencyStop() {
        emergencyStop.trip()
        agentOrchestrator.stop()
        emergencyStop.reset()
    }
}
