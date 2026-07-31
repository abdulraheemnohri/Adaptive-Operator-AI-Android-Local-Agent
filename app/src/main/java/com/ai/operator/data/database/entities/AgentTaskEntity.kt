package com.ai.operator.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val timestamp: Long,
    val status: String, // PENDING, RUNNING, COMPLETED, FAILED, STOPPED
    val totalSteps: Int,
    val durationMs: Long,
    val usedSkillId: String?
)
