package com.ai.operator.core.agent

enum class AgentState {
    IDLE,
    THINKING,
    OBSERVING,
    PLANNING,
    WAITING_CONFIRMATION,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED,
    STOPPED
}
