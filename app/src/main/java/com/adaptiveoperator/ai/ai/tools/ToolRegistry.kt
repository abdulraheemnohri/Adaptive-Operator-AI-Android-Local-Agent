package com.adaptiveoperator.ai.ai.tools

import com.adaptiveoperator.ai.android.accessibility.OperatorAccessibilityService
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import com.adaptiveoperator.ai.security.SecurityPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

sealed class DispatchOutcome {
    data class Executed(val result: ToolResult) : DispatchOutcome()
    data class NeedsConfirmation(val call: ToolCall, val riskLevel: RiskLevel) : DispatchOutcome()
    data class Rejected(val reason: String) : DispatchOutcome()
}

/**
 * Section 21's full pipeline:
 *   Gemma -> tool call -> JSON parser -> schema validation -> permission check
 *   -> policy check -> tool execution -> result -> Gemma
 *
 * This is the ONLY class that turns a parsed ToolCall into an actual side effect.
 * AgentOrchestrator never calls a Tool directly.
 */
@Singleton
class ToolRegistry @Inject constructor(
    private val securityPolicy: SecurityPolicy,
    private val memoryRepository: MemoryRepository,
    openAppTool: OpenAppTool,
    getScreenStateTool: GetScreenStateTool,
    findElementTool: FindElementTool,
    findTextTool: FindTextTool,
    tapElementTool: TapElementTool,
    tapCoordinateTool: TapCoordinateTool,
    longPressTool: LongPressTool,
    swipeTool: SwipeTool,
    scrollTool: ScrollTool,
    typeTextTool: TypeTextTool,
    clearTextTool: ClearTextTool,
    pressBackTool: PressBackTool,
    pressHomeTool: PressHomeTool,
    waitTool: WaitTool,
    screenshotTool: ScreenshotTool
) {
    private val tools: Map<String, Tool> = listOf(
        openAppTool, getScreenStateTool, findElementTool, findTextTool, tapElementTool,
        tapCoordinateTool, longPressTool, swipeTool, scrollTool, typeTextTool, clearTextTool,
        pressBackTool, pressHomeTool, waitTool, screenshotTool
    ).associateBy { it.name }

    /** Gemma is only ever told about these -- the model cannot request a tool that
     *  isn't in this list, and ToolRegistry rejects anything it doesn't recognize. */
    fun availableToolDescriptions(): List<Triple<String, String, Map<String, String>>> =
        tools.values.map { Triple(it.name, it.description, it.argumentSchema) }

    /** Parses raw JSON like `{"tool": "type_text", "arguments": {"text": "..."}}`. */
    fun parseToolCall(rawJson: String): ToolCall? = try {
        val element = Json.parseToJsonElement(rawJson)
        val obj = element as? JsonObject ?: return null
        val toolName = (obj["tool"] as? JsonPrimitive)?.content ?: return null
        val argsElement = obj["arguments"]
        val args = if (argsElement != null) jsonElementToMap(argsElement) else emptyMap()
        ToolCall(tool = toolName, arguments = args, rawJson = rawJson)
    } catch (e: Exception) {
        null
    }

    /**
     * Runs the full pipeline for one call. If the policy says confirmation is
     * required, execution stops at [DispatchOutcome.NeedsConfirmation] and the caller
     * (AgentOrchestrator / Operator UI) must call [executeConfirmed] once the user
     * has approved it -- this class never re-decides risk after the caller confirms.
     */
    suspend fun dispatch(call: ToolCall, taskId: Long?): DispatchOutcome {
        val tool = tools[call.tool]
            ?: return DispatchOutcome.Rejected("'${call.tool}' is not a registered tool")

        if (!validateArguments(tool, call.arguments)) {
            return DispatchOutcome.Rejected("Arguments for '${call.tool}' do not match its schema")
        }

        val foregroundPackage = OperatorAccessibilityService.instance?.currentPackage?.value
        val decision = securityPolicy.evaluate(call, foregroundPackage)

        if (!decision.allowed) {
            memoryRepository.recordToolCall(
                taskId, call.tool, argumentsToJson(call.arguments), decision.reason, false, "BLOCKED"
            )
            return DispatchOutcome.Rejected(decision.reason ?: "Blocked by security policy")
        }

        if (decision.requiresConfirmation) {
            return DispatchOutcome.NeedsConfirmation(call, decision.riskLevel)
        }

        return DispatchOutcome.Executed(executeAndLog(tool, call, taskId, decision.riskLevel))
    }

    /** Called after the user has explicitly approved a NeedsConfirmation outcome. */
    suspend fun executeConfirmed(call: ToolCall, riskLevel: RiskLevel, taskId: Long?): ToolResult {
        val tool = tools[call.tool] ?: return ToolResult.Failure("Tool disappeared before confirmation", retryable = false)
        return executeAndLog(tool, call, taskId, riskLevel)
    }

    private suspend fun executeAndLog(tool: Tool, call: ToolCall, taskId: Long?, riskLevel: RiskLevel): ToolResult {
        val result = try {
            tool.execute(call.arguments)
        } catch (e: Exception) {
            ToolResult.Failure(e.message ?: "Unhandled exception in ${tool.name}", retryable = true)
        }

        val succeeded = result is ToolResult.Success
        val summary = when (result) {
            is ToolResult.Success -> result.summary
            is ToolResult.Failure -> result.reason
            is ToolResult.Denied -> result.reason
        }
        memoryRepository.recordToolCall(taskId, tool.name, argumentsToJson(call.arguments), summary, succeeded, riskLevel.name)
        return result
    }

    private fun validateArguments(tool: Tool, arguments: Map<String, Any?>): Boolean =
        tool.argumentSchema.keys.all { required -> arguments.containsKey(required) }

    private fun argumentsToJson(arguments: Map<String, Any?>): String =
        arguments.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":\"$v\"" }

    private fun jsonElementToMap(element: JsonElement): Map<String, Any?> {
        val obj = element as? JsonObject ?: return emptyMap()
        return obj.mapValues { (_, v) -> jsonElementToAny(v) }
    }

    private fun jsonElementToAny(element: JsonElement): Any? = when (element) {
        is JsonObject -> jsonElementToMap(element)
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.boolean
            element.longOrNull != null -> element.long
            element.doubleOrNull != null -> element.double
            else -> element.content
        }
        else -> null
    }
}
