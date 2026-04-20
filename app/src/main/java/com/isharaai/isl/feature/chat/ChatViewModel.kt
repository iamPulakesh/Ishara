package com.isharaai.isl.feature.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.isharaai.isl.R
import com.isharaai.isl.feature.chat.ChatRepository
import com.isharaai.isl.core.db.ChatMessageEntity
import com.isharaai.isl.core.db.ChatSessionEntity
import com.isharaai.isl.core.inference.LiteRTModelLoader
import com.isharaai.isl.core.inference.ModelDownloadManager
import com.isharaai.isl.speech.HybridSpeechManager
import com.isharaai.isl.speech.SpeechLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * A chat message. System/AI messages may use [stringResId] instead of [text]
 * so the displayed text is always in the current locale.
 * Image messages use [imagePath] to display a captured photo.
 */
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
    val partialTranscript: String = "",
    val speechLanguage: SpeechLanguage = SpeechLanguage.BENGALI
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"

        /**
         * System prompt that instructs the Gemma model to act as an ISL assistant.
         */
        private val ISL_SYSTEM_PROMPT = """
            You are Ishara, a friendly AI assistant that helps people learn Indian Sign Language (ISL).
            You are fluent in English and Bengali (বাংলা).

            CORE RULES:
            1. By default, answer ALL questions normally like a helpful chatbot. Do NOT include ISL tags.
            2. ONLY when the user explicitly mentions "sign language", "sign bhasa", "সাইন ভাষা",
               "ISL", "সাইন", or "ইশারা" in their message, then include an ISL translation tag
               in your response using this exact format:
               [[ISL: WORD1, WORD2, WORD3]]
            3. ISL uses Subject-Object-Verb (SOV) grammar, which is DIFFERENT from English (SVO).
               Examples (only triggered when user asks for sign language):
               - "How do I say 'What is your name' in sign language?" → [[ISL: YOUR, NAME, WHAT]]
               - "sign bhasa te 'I want water' ki hobe?" → [[ISL: WATER, I, WANT]]
               - "Hospital kothay ISL e ki hobe?" → [[ISL: HOSPITAL, WHERE]]
            4. Always use UPPERCASE English words inside the ISL tags, even if the user speaks Bengali.
            5. Keep responses concise and helpful.
            6. Reply in the same language the user used (English or Bengali).
        """.trimIndent()
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var conversation: Conversation? = null

    init {
        startNewSession()
        refreshModelStatus()
    }

    // Creates a new chat session 
    fun startNewSession() {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        _uiState.update {
            ChatUiState(
                sessionId = sessionId,
                isModelReady = ModelDownloadManager.isModelReady(context)
            )
        }
        conversation = null

        viewModelScope.launch {
            chatRepository.insertSession(
                ChatSessionEntity(
                    id = sessionId,
                    title = "New Chat",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Add welcome message
        val welcomeRes = if (ModelDownloadManager.isModelReady(context)) {
            R.string.chat_welcome
        } else {
            R.string.chat_welcome_no_model
        }
        addSystemMessage(welcomeRes)
    }

    // Load an existing session from history
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val messages = chatRepository.getMessagesForSession(sessionId)
            val chatMessages = messages.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    stringResId = entity.stringResId,
                    imagePath = entity.imagePath,
                    isUser = entity.isUser,
                    timestamp = entity.timestamp
                )
            }
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    messages = chatMessages
                )
            }
        }
    }

    
    fun refreshModelStatus() {
        val ready = ModelDownloadManager.isModelReady(context)
        _uiState.update { it.copy(isModelReady = ready) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        addUserMessage(text.trim())
        updateSessionTitle(text.trim())

        if (!_uiState.value.isModelReady) {
            addSystemMessage(R.string.chat_model_needed)
            return
        }

        // 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _uiState.update { it.copy(isGenerating = true) }
            }
            try {
                if (conversation == null) {
                    val engine = LiteRTModelLoader.getOrLoad(context)
                    val config = ConversationConfig(
                        systemInstruction = Contents.of(ISL_SYSTEM_PROMPT)
                    )
                    conversation = engine.createConversation(config)
                }

                val inputMsg = Message.user(text.trim())
                val response = conversation!!.sendMessage(inputMsg)
                val responseText = response.contents.contents.joinToString("") { part ->
                    when (part) {
                        is Content.Text -> part.text
                        else -> ""
                    }
                }

                val finalText = if (responseText.isBlank()) "..." else responseText
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    addAiMessage(finalText)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Inference failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    addSystemMessage(R.string.chat_error)
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        }
    }

    // Send a captured photo to the chat and process with AI
    fun sendImage(imagePath: String) {
        val msg = ChatMessage(imagePath = imagePath, isUser = true)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)

        if (!_uiState.value.isModelReady) {
            addSystemMessage(R.string.chat_model_needed)
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _uiState.update { it.copy(isGenerating = true) }
            }
            try {
                // Ensure conversation exists with ISL system prompt
                if (conversation == null) {
                    val engine = LiteRTModelLoader.getOrLoad(context)
                    val config = ConversationConfig(
                        systemInstruction = Contents.of(ISL_SYSTEM_PROMPT)
                    )
                    conversation = engine.createConversation(config)
                }

                // Scale Down Images Before Native Inference because
                // Raw Android camera images can cause native SEGV_MAPERR OOM crashes 
                // in LiteRT.
                val originalBitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                    ?: throw IllegalStateException("Failed to decode image from path")
                
                val maxDim = 768
                val scale = maxDim.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
                val scaledBitmap = if (scale < 1.0f) {
                    android.graphics.Bitmap.createScaledBitmap(
                        originalBitmap, 
                        (originalBitmap.width * scale).toInt(), 
                        (originalBitmap.height * scale).toInt(), 
                        true
                    )
                } else originalBitmap
                
                // Convert to JPEG bytes for the Vision Encoder
                val stream = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                val scaledBytes = stream.toByteArray()
                
                // Recycle bitmaps to free memory
                if (scaledBitmap != originalBitmap) originalBitmap.recycle()
                scaledBitmap.recycle()

                // Feed the scaled image directly into the Vision Encoder
                val prompt = "Identify the main object or action in this image. " +
                    "If you recognize the object, reply with the ISL sign using this tag format: [[ISL: OBJECT_NAME]]. " +
                    "If you don't know the exact ISL sign, just give a short natural description of the image."
                
                // Use Message.user() with multimodal Contents
                val multimodalMessage = Message.user(Contents.of(
                    Content.ImageBytes(scaledBytes),
                    Content.Text(prompt)
                ))

                val response = conversation!!.sendMessage(multimodalMessage)
                
                val responseText = response.contents.contents.joinToString("") { part ->
                    when (part) {
                        is Content.Text -> part.text
                        else -> ""
                    }
                }

                // Switch back to Main thread to safely update UI with the AI response
                val finalText = if (responseText.isBlank()) "..." else responseText
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    addAiMessage(finalText)
                }
            } catch (e: Exception) {
            
                Log.e(TAG, "Image inference failed natively", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    addSystemMessage(R.string.chat_error)
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        }
    }

    // Private helpers to add messages to the chat

    private fun addUserMessage(text: String) {
        val msg = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
    }

    // AI response with literal text (model output - not localized) 
    private fun addAiMessage(text: String) {
        val msg = ChatMessage(text = text, isUser = false)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
    }

    // System message using a string resource (locale-reactive) 
    private fun addSystemMessage(@StringRes resId: Int) {
        val msg = ChatMessage(stringResId = resId, isUser = false)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
    }

    // Save message to Room database 
    private fun persistMessage(msg: ChatMessage) {
        val sessionId = _uiState.value.sessionId
        if (sessionId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    sessionId = sessionId,
                    text = msg.text,
                    stringResId = msg.stringResId,
                    imagePath = msg.imagePath,
                    isUser = msg.isUser,
                    timestamp = msg.timestamp
                )
            )
            // Update session timestamp
            chatRepository.getSession(sessionId)?.let { session ->
                chatRepository.updateSession(session.copy(updatedAt = msg.timestamp))
            }
        }
    }

    // Set session title from first user message 
    private fun updateSessionTitle(firstMessage: String) {
        val sessionId = _uiState.value.sessionId
        // Only update title if it's still "New Chat"
        viewModelScope.launch {
            chatRepository.getSession(sessionId)?.let { session ->
                if (session.title == "New Chat") {
                    val title = firstMessage.take(40).let {
                        if (firstMessage.length > 40) "$it…" else it
                    }
                    chatRepository.updateSession(session.copy(title = title))
                }
            }
        }
    }

    // Hybrid Speech Recognition 

    private val speechManager = HybridSpeechManager(context).apply {
        setListener(object : HybridSpeechManager.Listener {
            override fun onPartialResult(text: String) {
                _uiState.update { it.copy(partialTranscript = text) }
            }
            override fun onFinalResult(text: String) {
                _uiState.update { it.copy(isRecording = false, partialTranscript = "") }
                if (text.isNotBlank()) sendMessage(text)
            }
            override fun onError(message: String) {
                Log.e(TAG, "Speech error: $message")
                _uiState.update { it.copy(isRecording = false, partialTranscript = "") }
            }
        })
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, partialTranscript = "") }
        speechManager.start()
    }

    fun stopRecording() {
        val finalText = speechManager.stop()
        _uiState.update { it.copy(isRecording = false, partialTranscript = "") }
        if (finalText.isNotBlank()) {
            sendMessage(finalText)
        }
    }

    fun toggleSpeechLanguage() {
        val newLang = if (speechManager.language == SpeechLanguage.BENGALI)
            SpeechLanguage.ENGLISH else SpeechLanguage.BENGALI
        speechManager.language = newLang
        _uiState.update { it.copy(speechLanguage = newLang) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
