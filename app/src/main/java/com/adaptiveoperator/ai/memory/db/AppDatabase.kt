package com.adaptiveoperator.ai.memory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.adaptiveoperator.ai.memory.db.dao.ConversationDao
import com.adaptiveoperator.ai.memory.db.dao.ExperienceDao
import com.adaptiveoperator.ai.memory.db.dao.PreferenceDao
import com.adaptiveoperator.ai.memory.db.dao.SkillDao
import com.adaptiveoperator.ai.memory.db.dao.TaskDao
import com.adaptiveoperator.ai.memory.db.dao.ToolHistoryDao
import com.adaptiveoperator.ai.memory.db.entity.ConversationEntity
import com.adaptiveoperator.ai.memory.db.entity.ExperienceEntity
import com.adaptiveoperator.ai.memory.db.entity.PreferenceEntity
import com.adaptiveoperator.ai.memory.db.entity.SkillEntity
import com.adaptiveoperator.ai.memory.db.entity.SkillStepEntity
import com.adaptiveoperator.ai.memory.db.entity.TaskEntity
import com.adaptiveoperator.ai.memory.db.entity.ToolHistoryEntity

@Database(
    entities = [
        TaskEntity::class,
        ExperienceEntity::class,
        SkillEntity::class,
        SkillStepEntity::class,
        PreferenceEntity::class,
        ToolHistoryEntity::class,
        ConversationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun skillDao(): SkillDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun toolHistoryDao(): ToolHistoryDao
    abstract fun conversationDao(): ConversationDao
}
