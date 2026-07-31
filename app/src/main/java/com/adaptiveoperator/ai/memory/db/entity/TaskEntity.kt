package com.adaptiveoperator.ai.memory.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestText: String,
    val sourceApp: String?,
    val status: String,          // PLANNING | EXECUTING | SUCCESS | FAILED | STOPPED
    val skillIdUsed: Long? = null,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val durationMs: Long? = null
)
