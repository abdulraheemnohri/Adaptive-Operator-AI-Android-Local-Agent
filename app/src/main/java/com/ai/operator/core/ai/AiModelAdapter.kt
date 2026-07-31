package com.ai.operator.core.ai

import com.ai.operator.core.ai.models.StructuredContext

interface AiModelAdapter {
    fun formatPrompt(context: StructuredContext): String
    fun parseResponse(rawOutput: String): ResponseType
}

sealed interface ResponseType {
    data class TextResponse(val text: String) : ResponseType
    data class ToolCallResponse(val toolCalls: List<ToolCall>) : ResponseType
    data class ThoughtAndAction(val thoughts: String, val toolCalls: List<ToolCall>) : ResponseType
}

data class ToolCall(
    val toolName: String,
    val arguments: Map<String, Any>
)
