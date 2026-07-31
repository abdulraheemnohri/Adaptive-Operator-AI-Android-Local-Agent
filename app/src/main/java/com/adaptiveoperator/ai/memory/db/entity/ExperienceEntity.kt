package com.adaptiveoperator.ai.memory.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded attempt at a task, successful or not. `actionsJson` is a JSON array of
 * the tool calls issued in order -- this is the raw material SkillEngine mines for
 * repeated, reusable patterns (Section 26).
 */
@Entity(tableName = "experiences")
data class ExperienceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val goalDescription: String,
    val actionsJson: String,
    val succeeded: Boolean,
    val failureReason: String? = null,
    val durationMs: Long,
    val createdAtEpochMs: Long
)
