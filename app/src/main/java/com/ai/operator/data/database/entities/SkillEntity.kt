package com.ai.operator.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerPhrasesJson: String,
    val description: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastUsedTimestamp: Long = 0,
    val averageDurationMs: Long = 0,
    val confidence: Double = 0.0,
    val isEnabled: Boolean = true
)
