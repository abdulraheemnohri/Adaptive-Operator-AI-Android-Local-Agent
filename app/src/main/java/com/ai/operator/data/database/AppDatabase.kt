package com.ai.operator.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ai.operator.data.database.daos.AgentTaskDao
import com.ai.operator.data.database.daos.ConversationDao
import com.ai.operator.data.database.daos.ExperienceDao
import com.ai.operator.data.database.daos.PreferenceDao
import com.ai.operator.data.database.daos.SkillDao
import com.ai.operator.data.database.daos.ToolExecutionDao
import com.ai.operator.data.database.entities.AgentTaskEntity
import com.ai.operator.data.database.entities.ConversationEntity
import com.ai.operator.data.database.entities.ExperienceEntity
import com.ai.operator.data.database.entities.MessageEntity
import com.ai.operator.data.database.entities.PreferenceEntity
import com.ai.operator.data.database.entities.SkillEntity
import com.ai.operator.data.database.entities.SkillStepEntity
import com.ai.operator.data.database.entities.ToolExecutionEntity

@Database(
    entities = [
        AgentTaskEntity::class,
        ToolExecutionEntity::class,
        ExperienceEntity::class,
        SkillEntity::class,
        SkillStepEntity::class,
        PreferenceEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentTaskDao(): AgentTaskDao
    abstract fun toolExecutionDao(): ToolExecutionDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun skillDao(): SkillDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun conversationDao(): ConversationDao
}
