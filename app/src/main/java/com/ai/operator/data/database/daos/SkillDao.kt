package com.ai.operator.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ai.operator.data.database.entities.SkillEntity
import com.ai.operator.data.database.entities.SkillStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillById(id: String): SkillEntity?

    @Query("SELECT * FROM skill_steps WHERE skillId = :skillId ORDER BY stepOrder ASC")
    suspend fun getStepsForSkill(skillId: String): List<SkillStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillSteps(steps: List<SkillStepEntity>)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("DELETE FROM skill_steps WHERE skillId = :skillId")
    suspend fun deleteStepsForSkill(skillId: String)

    @Transaction
    suspend fun saveSkillWithSteps(skill: SkillEntity, steps: List<SkillStepEntity>) {
        insertSkill(skill)
        deleteStepsForSkill(skill.id)
        insertSkillSteps(steps)
    }
}
