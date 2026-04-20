package com.isharaai.isl.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Chat session entity
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
