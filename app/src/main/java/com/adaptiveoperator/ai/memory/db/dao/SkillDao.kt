package com.adaptiveoperator.ai.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.adaptiveoperator.ai.memory.db.entity.SkillEntity
import com.adaptiveoperator.ai.memory.db.entity.SkillStepEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Dao
interface SkillDao {
    @Insert
    suspend fun insertSkill(skill: SkillEntity): Long

    @Insert
    suspend fun insertSteps(steps: List<SkillStepEntity>)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Query("SELECT * FROM skills WHERE enabled = 1 ORDER BY successCount DESC")
    fun allEnabled(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills ORDER BY successCount DESC")
    fun all(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skill_steps WHERE skillId = :skillId ORDER BY stepOrder ASC")
    suspend fun stepsFor(skillId: Long): List<SkillStepEntity>

    @Query("DELETE FROM skills WHERE id = :skillId")
    suspend fun delete(skillId: Long)

    @Query(
        """
        UPDATE skills SET
            successCount = successCount + CASE WHEN :succeeded THEN 1 ELSE 0 END,
            failureCount = failureCount + CASE WHEN :succeeded THEN 0 ELSE 1 END,
            lastUsedEpochMs = :timestamp
        WHERE id = :skillId
        """
    )
    suspend fun recordOutcome(skillId: Long, succeeded: Boolean, timestamp: Long)

    /** Simple trigger match for Section 26: exact/substring match against stored
     *  trigger phrases (a real JSON string array, e.g. `["wifi settings","wifi kholo"]`).
     *  Good enough for V1; semantic matching would need an embedding model, which the
     *  Hard Architecture Rule explicitly rules out. */
    @Transaction
    suspend fun findByTrigger(utterance: String): SkillEntity? {
        val lower = utterance.lowercase()
        return allSnapshot().firstOrNull { skill ->
            if (!skill.enabled) return@firstOrNull false
            val triggers = runCatching { Json.decodeFromString<List<String>>(skill.triggersJson) }
                .getOrDefault(emptyList())
            triggers.any { trigger -> trigger.isNotBlank() && lower.contains(trigger.lowercase()) }
        }
    }

    @Query("SELECT * FROM skills")
    suspend fun allSnapshot(): List<SkillEntity>
}

