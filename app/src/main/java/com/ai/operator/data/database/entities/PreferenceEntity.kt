package com.ai.operator.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val type: String // STRING, INT, FLOAT, BOOLEAN
)
