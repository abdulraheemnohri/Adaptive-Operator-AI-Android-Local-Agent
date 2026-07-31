package com.ai.operator.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai.operator.data.database.entities.ExperienceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceDao {
    @Query("SELECT * FROM experiences ORDER BY timestamp DESC")
    fun getAllExperiences(): Flow<List<ExperienceEntity>>

    @Query("SELECT * FROM experiences WHERE id = :id")
    suspend fun getExperienceById(id: String): ExperienceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: ExperienceEntity)

    @Delete
    suspend fun deleteExperience(experience: ExperienceEntity)

    @Query("DELETE FROM experiences")
    suspend fun deleteAllExperiences()
}
