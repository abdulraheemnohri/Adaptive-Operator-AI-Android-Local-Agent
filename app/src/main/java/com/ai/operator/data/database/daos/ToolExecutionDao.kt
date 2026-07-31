package com.ai.operator.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai.operator.data.database.entities.ToolExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolExecutionDao {
    @Query("SELECT * FROM tool_executions WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getToolExecutionsForTask(taskId: Long): Flow<List<ToolExecutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolExecution(execution: ToolExecutionEntity): Long

    @Query("DELETE FROM tool_executions WHERE taskId = :taskId")
    suspend fun deleteToolExecutionsForTask(taskId: Long)
}
