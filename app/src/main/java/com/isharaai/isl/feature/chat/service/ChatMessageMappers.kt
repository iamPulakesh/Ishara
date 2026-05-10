package com.isharaai.isl.feature.chat.service

import com.isharaai.isl.core.db.ChatMessageEntity
import com.isharaai.isl.feature.chat.ChatMessage

internal fun ChatMessageEntity.toChatMessage(): ChatMessage =
    ChatMessage(
        id = id,
        text = text,
        stringResId = stringResId,
        imagePath = imagePath,
        isUser = isUser,
        timestamp = timestamp
    )

internal fun ChatMessage.toEntity(sessionId: String): ChatMessageEntity =
    ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        text = text,
        stringResId = stringResId,
        imagePath = imagePath,
        isUser = isUser,
        timestamp = timestamp
    )
