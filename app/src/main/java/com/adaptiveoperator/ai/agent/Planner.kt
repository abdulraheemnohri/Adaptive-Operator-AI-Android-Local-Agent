package com.adaptiveoperator.ai.agent

import com.adaptiveoperator.ai.ai.tools.ToolCall
import com.adaptiveoperator.ai.ai.tools.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlannedStep {
    data class ToolInvocation(val call: ToolCall) : PlannedStep()
    data class ConversationalReply(val text: String) : PlannedStep()
    data object TaskComplete : PlannedStep()
}

/**
 * Turns one turn of raw Gemma output into something AgentOrchestrator can act on.
 * Gemma is prompted (see the system preamble AgentOrchestrator sends before the
 * user's request) to emit EITHER a single `{"tool": ..., "arguments": {...}}` JSON
 * object per step, OR the literal token `DONE` once it considers the task finished,
 * OR plain prose when it's just answering a question with no action required.
 */
@Singleton
class Planner @Inject constructor(
    private val toolRegistry: ToolRegistry
) {
    fun parse(rawModelOutput: String): PlannedStep {
        val trimmed = rawModelOutput.trim()

        if (trimmed.equals("DONE", ignoreCase = true)) return PlannedStep.TaskComplete

        val jsonCandidate = extractJsonObject(trimmed)
        if (jsonCandidate != null) {
            toolRegistry.parseToolCall(jsonCandidate)?.let { return PlannedStep.ToolInvocation(it) }
        }

        return PlannedStep.ConversationalReply(trimmed)
    }

    /** Gemma sometimes wraps JSON in prose or a markdown fence despite the prompt
     *  asking for raw JSON -- pull out the first balanced {...} block defensively
     *  rather than requiring an exact-format match that breaks on the first surprise. */
    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
