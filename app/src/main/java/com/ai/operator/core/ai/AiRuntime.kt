package com.ai.operator.core.ai

import com.ai.operator.core.ai.models.AgentRequest
import com.ai.operator.core.ai.models.AgentResult
import com.ai.operator.core.ai.models.GenerationRequest
import com.ai.operator.core.ai.models.GenerationResult
import com.ai.operator.core.ai.models.ResourceStatus
import com.ai.operator.core.ai.models.RuntimeConfig
import com.ai.operator.core.ai.models.RuntimeInfo
import kotlinx.coroutines.flow.Flow

interface AiRuntime {
    suspend fun loadModel(modelPath: String, config: RuntimeConfig): Result<Unit>
    suspend fun unloadModel(): Result<Unit>
    suspend fun generate(request: GenerationRequest): Flow<GenerationResult>
    suspend fun generateWithTools(request: AgentRequest): Flow<AgentResult>
    fun isLoaded(): Boolean
    fun getRuntimeInfo(): RuntimeInfo
    fun monitorResources(): Flow<ResourceStatus>
}
