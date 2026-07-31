package com.adaptiveoperator.ai.agent

import com.adaptiveoperator.ai.ai.tools.ToolCall

/** Section 50's global AI Status Indicator, plus Section 39's per-task plan view. */
enum class AgentStatus { READY, LOADING, THINKING, ACTING, OBSERVING, SUCCESS, ERROR, IDLE }

data class PlanStep(
    val description: String,
    val toolCall: ToolCall? = null,
    val done: Boolean = false
)

data class AgentTaskState(
    val status: AgentStatus = AgentStatus.IDLE,
    val taskDescription: String? = null,
    val plan: List<PlanStep> = emptyList(),
    val currentStepIndex: Int = -1,
    val lastMessage: String? = null,
    val awaitingConfirmation: ToolCall? = null
)
