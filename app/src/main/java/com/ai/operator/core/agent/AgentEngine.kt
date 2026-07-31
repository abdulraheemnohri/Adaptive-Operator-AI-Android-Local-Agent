package com.ai.operator.core.agent

import kotlinx.coroutines.flow.Flow

data class AgentRequestDto(
    val userRequest: String,
    val maxSteps: Int = 30,
    val timeoutMs: Long = 300000 // 5 minutes
)

data class AgentResultDto(
    val isSuccess: Boolean,
    val finalState: AgentState,
    val stepsTaken: Int,
    val durationMs: Long,
    val message: String
)

interface AgentEngine {
    suspend fun execute(request: AgentRequestDto): Flow<AgentState>
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
    fun getCurrentState(): AgentState
}
