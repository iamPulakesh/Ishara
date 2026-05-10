package com.isharaai.isl.feature.chat.service

import com.isharaai.isl.core.db.ChatRepository
import com.isharaai.isl.core.db.ChatSessionEntity
import com.isharaai.isl.feature.chat.ChatMessage
import java.util.UUID
import javax.inject.Inject

/** Manages chat session lifecycle and message persistence. */
class SessionService @Inject constructor(
    private val repo: ChatRepository
) {

    companion object {
        const val DEFAULT_SESSION_TITLE = "New Chat"
        private const val TITLE_PREVIEW_LIMIT = 40
    }

    suspend fun createSession(title: String = DEFAULT_SESSION_TITLE): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        repo.insertSession(
            ChatSessionEntity(
                id = id,
                title = title.ifBlank { DEFAULT_SESSION_TITLE },
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun loadMessages(sessionId: String): List<ChatMessage> =
        repo.getMessagesForSession(sessionId).map { it.toChatMessage() }

    suspend fun persistMessage(sessionId: String, msg: ChatMessage) {
        if (sessionId.isEmpty()) return
        repo.insertMessage(msg.toEntity(sessionId))
        repo.getSession(sessionId)?.let { repo.updateSession(it.copy(updatedAt = msg.timestamp)) }
    }

    suspend fun updateTitleIfNew(sessionId: String, firstMessage: String) {
        repo.getSession(sessionId)?.let { session ->
            if (session.title == DEFAULT_SESSION_TITLE) {
                val title = firstMessage.take(TITLE_PREVIEW_LIMIT)
                    .let { if (firstMessage.length > TITLE_PREVIEW_LIMIT) "$it..." else it }
                repo.updateSession(session.copy(title = title))
            }
        }
    }
}
