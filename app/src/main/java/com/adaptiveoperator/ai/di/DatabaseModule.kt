package com.adaptiveoperator.ai.di

import android.content.Context
import androidx.room.Room
import com.adaptiveoperator.ai.memory.db.AppDatabase
import com.adaptiveoperator.ai.memory.db.dao.ConversationDao
import com.adaptiveoperator.ai.memory.db.dao.ExperienceDao
import com.adaptiveoperator.ai.memory.db.dao.PreferenceDao
import com.adaptiveoperator.ai.memory.db.dao.SkillDao
import com.adaptiveoperator.ai.memory.db.dao.TaskDao
import com.adaptiveoperator.ai.memory.db.dao.ToolHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "adaptive_operator.db")
            // Section 24 memory tables are additive by design; a real migration path
            // replaces this once the schema needs to change post-install.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideExperienceDao(db: AppDatabase): ExperienceDao = db.experienceDao()
    @Provides fun provideSkillDao(db: AppDatabase): SkillDao = db.skillDao()
    @Provides fun providePreferenceDao(db: AppDatabase): PreferenceDao = db.preferenceDao()
    @Provides fun provideToolHistoryDao(db: AppDatabase): ToolHistoryDao = db.toolHistoryDao()
    @Provides fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()
}
