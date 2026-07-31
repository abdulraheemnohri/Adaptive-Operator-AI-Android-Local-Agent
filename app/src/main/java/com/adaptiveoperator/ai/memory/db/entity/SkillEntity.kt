package com.adaptiveoperator.ai.memory.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A learned, reusable workflow (Section 26). `triggersJson` is a JSON array of
 * phrases -- deliberately multilingual per the spec's own example
 * ("wifi settings" / "wifi kholo" / "open wifi").
 *
 * Confidence fields (Section 27) are denormalized onto the skill row itself so the
 * Skill Library list (Section 56) can render without a join on every frame.
 */
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggersJson: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastUsedEpochMs: Long? = null,
    val averageDurationMs: Long = 0,
    val enabled: Boolean = true,
    val createdAtEpochMs: Long
) {
    val successRate: Float
        get() {
            val total = successCount + failureCount
            return if (total == 0) 0f else successCount.toFloat() / total.toFloat()
        }

    val confidenceLabel: String
        get() = when {
            successCount + failureCount < 3 -> "Learning"
            successRate >= 0.9f -> "High"
            successRate >= 0.7f -> "Medium"
            else -> "Low"
        }
}

@Entity(
    tableName = "skill_steps",
    foreignKeys = [
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SkillStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: Long,
    val stepOrder: Int,
    val toolName: String,
    val argumentsJson: String,
    val verificationHint: String? = null
)
