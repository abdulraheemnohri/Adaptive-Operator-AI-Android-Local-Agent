package com.adaptiveoperator.ai.ai.context

import com.adaptiveoperator.ai.ai.tools.ToolRegistry
import com.adaptiveoperator.ai.android.accessibility.AccessibilityTree
import com.adaptiveoperator.ai.android.accessibility.OperatorAccessibilityService
import com.adaptiveoperator.ai.memory.db.entity.SkillEntity
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

data class AgentContext(
    val userRequest: String,
    val currentApp: String?,
    val accessibilityTree: AccessibilityTree?,
    val hasScreenshot: Boolean,
    val matchingSkill: SkillEntity?,
    val recentPreferenceNotes: List<String>,
    val availableTools: List<Triple<String, String, Map<String, String>>>
)

/**
 * Section 15: never hand Gemma raw, unstructured Android state. Everything the model
 * sees for a single planning step is assembled here, once, from:
 *   user request + current app + accessibility tree + screenshot (flag only --
 *   the bitmap itself goes in as a separate multimodal input, not inlined into text)
 *   + relevant skill + tool availability.
 *
 * Security policy is deliberately NOT included in the prompt payload -- Gemma proposes
 * actions, but never sees (and therefore can't talk its way around) the policy that
 * gates them. That check happens after the fact in ToolRegistry.
 */
@Singleton
class ContextBuilder @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val memoryRepository: MemoryRepository
) {
    suspend fun build(userRequest: String, includeScreenshot: Boolean): AgentContext {
        val service = OperatorAccessibilityService.instance
        val tree = service?.captureTree()
        val currentApp = service?.currentPackage?.value

        val matchingSkill = memoryRepository.findMatchingSkill(userRequest)

        return AgentContext(
            userRequest = userRequest,
            currentApp = currentApp,
            accessibilityTree = tree,
            hasScreenshot = includeScreenshot,
            matchingSkill = matchingSkill,
            recentPreferenceNotes = emptyList(), // populated once Settings exposes user corrections as notes
            availableTools = toolRegistry.availableToolDescriptions()
        )
    }

    /** Renders [AgentContext] into the text block prepended to the user's turn. The
     *  accessibility tree is serialized compactly (Section 19's JSON shape) rather
     *  than pretty-printed, to keep prompt-token cost down on every single step. */
    fun toPromptText(context: AgentContext): String = buildString {
        appendLine("User request: ${context.userRequest}")
        context.currentApp?.let { appendLine("Current app: $it") }
        context.matchingSkill?.let {
            appendLine("Known skill available: '${it.name}' (confidence: ${it.confidenceLabel})")
        }
        context.accessibilityTree?.let { tree ->
            appendLine("Screen elements (${tree.elements.size}):")
            tree.elements.forEach { el ->
                appendLine("  [${el.index}] role=${el.role} text=\"${el.text ?: el.contentDescription ?: ""}\" clickable=${el.clickable}")
            }
        }
        if (context.hasScreenshot) appendLine("A screenshot of the current screen is attached.")
        appendLine("Available tools: ${context.availableTools.joinToString(", ") { it.first }}")
    }
}
