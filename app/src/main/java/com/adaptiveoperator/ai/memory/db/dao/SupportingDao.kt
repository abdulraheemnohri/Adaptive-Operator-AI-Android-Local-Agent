package com.adaptiveoperator.ai.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adaptiveoperator.ai.memory.db.entity.ConversationEntity
import com.adaptiveoperator.ai.memory.db.entity.PreferenceEntity
import com.adaptiveoperator.ai.memory.db.entity.ToolHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key")
    suspend fun get(key: String): PreferenceEntity?

    @androidx.room.Upsert
    suspend fun set(preference: PreferenceEntity)

    @Query("SELECT * FROM preferences")
    fun all(): Flow<List<PreferenceEntity>>
}

@Dao
interface ToolHistoryDao {
    @Insert
    suspend fun insert(entry: ToolHistoryEntity): Long

    @Query("SELECT * FROM tool_history ORDER BY executedAtEpochMs DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<ToolHistoryEntity>>
}

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(turn: ConversationEntity): Long

    @Query("SELECT * FROM conversation ORDER BY timestampEpochMs ASC")
    fun history(): Flow<List<ConversationEntity>>

    @Query("DELETE FROM conversation")
    suspend fun clear()
}
