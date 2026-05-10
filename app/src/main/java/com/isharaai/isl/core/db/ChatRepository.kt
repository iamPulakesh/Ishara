package com.isharaai.isl.core.db

import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {

    // Sessions

    fun getAllSessions(): Flow<List<ChatSessionEntity>> =
        chatDao.getAllSessions()

    suspend fun getSession(sessionId: String): ChatSessionEntity? =
        chatDao.getSession(sessionId)

    suspend fun insertSession(session: ChatSessionEntity) =
        chatDao.insertSession(session)

    suspend fun updateSession(session: ChatSessionEntity) =
        chatDao.updateSession(session)

    /**
     * Deletes a session, its messages (via CASCADE) and all associated
     * image files from disk.
     */
    suspend fun deleteSession(sessionId: String) {
        val imagePaths = chatDao.getImagePathsForSession(sessionId)
        chatDao.deleteSession(sessionId)
        deleteImageFiles(imagePaths)
    }

    /** Deletes all sessions except the current session. */
    suspend fun deleteAllSessionsExcept(excludeId: String) {
        val imagePaths = chatDao.getAllImagePathsExcept(excludeId)
        chatDao.deleteAllSessionsExcept(excludeId)
        deleteImageFiles(imagePaths)
    }

    private fun deleteImageFiles(paths: List<String>) {
        paths.forEach { path ->
            try { File(path).delete() } catch (_: Exception) { }
        }
    }

    // Messages

    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun insertMessage(message: ChatMessageEntity) =
        chatDao.insertMessage(message)
}
