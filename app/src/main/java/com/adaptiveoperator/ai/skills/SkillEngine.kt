package com.adaptiveoperator.ai.skills

import com.adaptiveoperator.ai.memory.db.entity.SkillStepEntity
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 26 pipeline: successful task -> analyze workflow -> detect reusable pattern
 * -> generate skill -> validate -> save. Section 28's four learning signals
 * (successful execution / failed execution / user correction / repeated workflow)
 * are handled as follows in V1:
 *   - successful execution -> SkillDao.recordOutcome reinforces an existing skill
 *   - failed execution -> same call with succeeded=false, dragging confidence down
 *   - repeated workflow -> this class's [evaluateForSkillPromotion], called after
 *     every successful task
 *   - user correction -> not yet wired to a UI affordance; see README roadmap
 */
@Singleton
class SkillEngine @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    companion object {
        private const val MIN_OCCURRENCES_TO_PROMOTE = 3
    }

    /** Called after any task finishes successfully. Cheap by design -- it only looks
     *  at experiences whose goal exactly matches [goal], not the whole table. */
    suspend fun evaluateForSkillPromotion(goal: String) {
        val existing = memoryRepository.findMatchingSkill(goal)
        if (existing != null) {
            memoryRepository.recordSkillOutcome(existing.id, succeeded = true)
            return
        }

        val experiences = memoryRepository.experiencesForGoal(goal)
        val successfulRuns = experiences.filter { it.succeeded }
        if (successfulRuns.size < MIN_OCCURRENCES_TO_PROMOTE) return

        // Section 26: "detect reusable pattern" -- V1's pattern detection is
        // intentionally simple (same goal text, repeated >= N times, all successful).
        // A more sophisticated version could diff the action sequences themselves to
        // confirm they're actually the same steps, not just the same request text.
        val mostRecent = successfulRuns.maxByOrNull { it.createdAtEpochMs } ?: return
        val steps = parseStepsFromActionsJson(mostRecent.actionsJson)
        if (steps.isEmpty()) return

        val triggers = buildTriggerVariants(goal)
        memoryRepository.createSkill(
            name = goal.replaceFirstChar { it.uppercase() },
            triggers = triggers,
            steps = steps
        )
    }

    suspend fun runSkillOutcome(skillId: Long, succeeded: Boolean) =
        memoryRepository.recordSkillOutcome(skillId, succeeded)

    suspend fun deleteSkill(skillId: Long) = memoryRepository.deleteSkill(skillId)

    /** Turns an experience's logged tool-call JSON array (Section 25) into ordered
     *  SkillStepEntity rows a future task can replay directly, without re-invoking
     *  Gemma for a screen it has already solved once. */
    private fun parseStepsFromActionsJson(actionsJson: String): List<SkillStepEntity> = try {
        val array = Json.parseToJsonElement(actionsJson) as? JsonArray ?: return emptyList()
        array.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val tool = (obj["tool"] as? JsonPrimitive)?.content ?: return@mapIndexedNotNull null
            val argsJson = obj["arguments"]?.toString() ?: "{}"
            SkillStepEntity(skillId = 0, stepOrder = index, toolName = tool, argumentsJson = argsJson)
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Section 26's own example ships a skill with multiple trigger phrases,
     *  including a transliterated one ("wifi kholo") alongside the English original --
     *  V1 doesn't auto-translate, it just keeps whatever phrasing the user actually
     *  used across their repeated requests as separate trigger entries. */
    private fun buildTriggerVariants(goal: String): List<String> = listOf(goal.lowercase())
}
