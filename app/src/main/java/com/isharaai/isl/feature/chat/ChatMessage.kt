package com.isharaai.isl.feature.chat

import androidx.annotation.StringRes
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    @StringRes val stringResId: Int = 0,
    val imagePath: String? = null,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isModelReady: Boolean = false,
    val sessionId: String = "",
    val isRecording: Boolean = false,
    val partialTranscript: String = ""
)
