package com.ai.operator.core.ai.models

data class GenerationRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null
)

data class GenerationResult(
    val text: String,
    val latencyMs: Long,
    val tokensGenerated: Int,
    val isComplete: Boolean
)

data class AgentRequest(
    val userGoal: String,
    val context: StructuredContext
)

data class AgentResult(
    val plan: List<String>,
    val nextAction: String?,
    val thoughts: String?,
    val rawModelResponse: String
)

data class StructuredContext(
    val activePackageName: String,
    val screenMetadata: String,
    val accessibilityTreeJson: String,
    val recentActionHistoryJson: String,
    val screenshotPath: String?,
    val matchedSkillsJson: String?
)
