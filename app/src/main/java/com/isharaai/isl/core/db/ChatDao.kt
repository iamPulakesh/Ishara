package com.isharaai.isl.core.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // Sessions

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ChatSessionEntity?

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM chat_sessions WHERE id != :excludeId")
    suspend fun deleteAllSessionsExcept(excludeId: String)

    // Messages

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    /** Get all image paths for a session */
    @Query("SELECT imagePath FROM chat_messages WHERE sessionId = :sessionId AND imagePath IS NOT NULL")
    suspend fun getImagePathsForSession(sessionId: String): List<String>

    /** Get ALL image paths across all sessions */
    @Query("SELECT imagePath FROM chat_messages WHERE imagePath IS NOT NULL")
    suspend fun getAllImagePaths(): List<String>

    /** Get all image paths except those belonging to a specific session */
    @Query("SELECT imagePath FROM chat_messages WHERE imagePath IS NOT NULL AND sessionId != :excludeId")
    suspend fun getAllImagePathsExcept(excludeId: String): List<String>
}
