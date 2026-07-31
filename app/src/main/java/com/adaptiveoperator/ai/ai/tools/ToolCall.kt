package com.adaptiveoperator.ai.ai.tools

/**
 * Parsed form of the JSON Gemma emits, e.g. `{"tool": "type_text", "arguments": {"text": "..."}}`
 * (Section 21). `rawJson` is retained for audit logging even after arguments are parsed.
 */
data class ToolCall(
    val tool: String,
    val arguments: Map<String, Any?>,
    val rawJson: String
)

sealed class ToolResult {
    data class Success(val data: Map<String, Any?> = emptyMap(), val summary: String) : ToolResult()
    data class Failure(val reason: String, val retryable: Boolean) : ToolResult()
    data class Denied(val reason: String) : ToolResult() // policy/permission rejected before execution
}

enum class RiskLevel { LOW, CONFIRM_REQUIRED, BLOCKED }
