package com.adaptiveoperator.ai.memory.repository

import com.adaptiveoperator.ai.memory.db.dao.ConversationDao
import com.adaptiveoperator.ai.memory.db.dao.ExperienceDao
import com.adaptiveoperator.ai.memory.db.dao.SkillDao
import com.adaptiveoperator.ai.memory.db.dao.TaskDao
import com.adaptiveoperator.ai.memory.db.dao.ToolHistoryDao
import com.adaptiveoperator.ai.memory.db.entity.ConversationEntity
import com.adaptiveoperator.ai.memory.db.entity.ExperienceEntity
import com.adaptiveoperator.ai.memory.db.entity.TaskEntity
import com.adaptiveoperator.ai.memory.db.entity.ToolHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything in the app reads/writes memory through this one class rather than
 * touching DAOs directly -- keeps AgentOrchestrator, SkillEngine, and the UI
 * ViewModels from needing to know Room exists.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val experienceDao: ExperienceDao,
    private val skillDao: SkillDao,
    private val toolHistoryDao: ToolHistoryDao,
    private val conversationDao: ConversationDao
) {
    fun recentTasks(limit: Int = 100): Flow<List<TaskEntity>> = taskDao.recentTasks(limit)

    suspend fun startTask(requestText: String, sourceApp: String?): Long =
        taskDao.insert(
            TaskEntity(
                requestText = requestText,
                sourceApp = sourceApp,
                status = "PLANNING",
                startedAtEpochMs = System.currentTimeMillis()
            )
        )

    suspend fun finishTask(task: TaskEntity, status: String) {
        val now = System.currentTimeMillis()
        taskDao.update(
            task.copy(status = status, finishedAtEpochMs = now, durationMs = now - task.startedAtEpochMs)
        )
    }

    suspend fun finishTaskById(taskId: Long, status: String) {
        val task = taskDao.getById(taskId) ?: return
        finishTask(task, status)
    }

    suspend fun recordExperience(
        taskId: Long,
        goal: String,
        actionsJson: String,
        succeeded: Boolean,
        failureReason: String?,
        durationMs: Long
    ): Long = experienceDao.insert(
        ExperienceEntity(
            taskId = taskId,
            goalDescription = goal,
            actionsJson = actionsJson,
            succeeded = succeeded,
            failureReason = failureReason,
            durationMs = durationMs,
            createdAtEpochMs = System.currentTimeMillis()
        )
    )

    suspend fun recordToolCall(
        taskId: Long?,
        toolName: String,
        argumentsJson: String,
        resultSummary: String?,
        succeeded: Boolean,
        riskLevel: String
    ) {
        toolHistoryDao.insert(
            ToolHistoryEntity(
                taskId = taskId,
                toolName = toolName,
                argumentsJson = argumentsJson,
                resultSummary = resultSummary,
                succeeded = succeeded,
                riskLevel = riskLevel,
                executedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    fun conversationHistory(): Flow<List<ConversationEntity>> = conversationDao.history()

    suspend fun appendConversationTurn(role: String, content: String) {
        conversationDao.insert(ConversationEntity(role = role, content = content, timestampEpochMs = System.currentTimeMillis()))
    }

    fun skills() = skillDao.allEnabled()
    fun allSkills() = skillDao.all()

    suspend fun findMatchingSkill(utterance: String) = skillDao.findByTrigger(utterance)

    suspend fun repeatedGoalsAwaitingSkill(minOccurrences: Int = 3) =
        experienceDao.repeatedSuccessfulGoals(minOccurrences)

    suspend fun experiencesForGoal(goal: String) = experienceDao.forGoal(goal)

    suspend fun createSkill(
        name: String,
        triggers: List<String>,
        steps: List<com.adaptiveoperator.ai.memory.db.entity.SkillStepEntity>
    ): Long {
        val skillId = skillDao.insertSkill(
            com.adaptiveoperator.ai.memory.db.entity.SkillEntity(
                name = name,
                triggersJson = kotlinx.serialization.json.Json.encodeToString(triggers),
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
        skillDao.insertSteps(steps.map { it.copy(skillId = skillId) })
        return skillId
    }

    suspend fun recordSkillOutcome(skillId: Long, succeeded: Boolean) =
        skillDao.recordOutcome(skillId, succeeded, System.currentTimeMillis())

    suspend fun deleteSkill(skillId: Long) = skillDao.delete(skillId)
}
