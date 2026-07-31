package com.adaptiveoperator.ai.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.adaptiveoperator.ai.memory.db.entity.ExperienceEntity
import com.adaptiveoperator.ai.memory.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY startedAtEpochMs DESC LIMIT :limit")
    fun recentTasks(limit: Int = 100): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?
}

@Dao
interface ExperienceDao {
    @Insert
    suspend fun insert(experience: ExperienceEntity): Long

    @Query("SELECT * FROM experiences WHERE goalDescription = :goal ORDER BY createdAtEpochMs DESC")
    suspend fun forGoal(goal: String): List<ExperienceEntity>

    @Query("SELECT * FROM experiences ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<ExperienceEntity>>

    /** Section 28: a goal repeated at least [minOccurrences] times, all successful,
     *  is a skill candidate for SkillEngine.discoverCandidates() to pick up. */
    @Query(
        """
        SELECT goalDescription, COUNT(*) as occurrences
        FROM experiences
        WHERE succeeded = 1
        GROUP BY goalDescription
        HAVING occurrences >= :minOccurrences
        """
    )
    suspend fun repeatedSuccessfulGoals(minOccurrences: Int = 3): List<GoalOccurrence>
}

data class GoalOccurrence(val goalDescription: String, val occurrences: Int)
