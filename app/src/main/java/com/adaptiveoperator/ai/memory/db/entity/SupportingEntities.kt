package com.adaptiveoperator.ai.memory.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User corrections and standing preferences that bias future planning (Section 28). */
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAtEpochMs: Long
)

/** Every tool call ever executed, independent of which task/experience it belonged to.
 *  This is the audit trail referenced throughout the Security Center (Section 44). */
@Entity(tableName = "tool_history")
data class ToolHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val toolName: String,
    val argumentsJson: String,
    val resultSummary: String?,
    val succeeded: Boolean,
    val riskLevel: String,       // LOW | CONFIRM_REQUIRED | BLOCKED
    val executedAtEpochMs: Long
)

/** Chat transcript, kept separate from Experience/Task so casual conversation doesn't
 *  pollute the skill-mining pipeline. */
@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,            // user | assistant
    val content: String,
    val timestampEpochMs: Long
)
