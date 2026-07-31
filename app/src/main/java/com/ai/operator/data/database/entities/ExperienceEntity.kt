package com.ai.operator.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiences")
data class ExperienceEntity(
    @PrimaryKey val id: String, // UUID
    val taskDescription: String,
    val stepsSequenceJson: String,
    val result: String, // SUCCESS, FAILURE
    val durationMs: Long,
    val timestamp: Long,
    val applicationPackage: String
)
