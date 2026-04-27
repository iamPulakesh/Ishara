package com.isharaai.isl.feature.chat

import android.content.Context
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
    val partialTranscript: String = ""
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
            2. ONLY when the user explicitly asks for "sign language", "sign bhasa", "ISL", a "sign" for a word/alphabet, "সাইন ভাষা", "সাইন", or "ইশারা", you MUST include an ISL translation tag in your response using this exact format (SPACE SEPARATED, NO COMMAS):
               [[ISL: WORD1 WORD2 WORD3]]
            3. ISL uses Subject-Object-Verb (SOV) grammar, which is DIFFERENT from English (SVO).
               Examples:
               - "How do I say 'What is your name' in sign language?" → Here is the sign: [[ISL: YOUR NAME WHAT]]
               - "sign bhasa te 'I want water' ki hobe?" → [[ISL: I WATER WANT]]
               - "Hospital kothay ISL e ki hobe?" → [[ISL: HOSPITAL WHERE]]
            4. For individual alphabets (A-Z) or numbers, NEVER give long explanations. ALWAYS provide the direct sign tag immediately.
               For numbers, ALWAYS use the numeric digit (0-9) inside the tag, NEVER the spelled-out English word.
               Examples:
               - "Just A give me the sign or alphabet A" → [[ISL: A]]
               - "Sign for 0" → [[ISL: 0]]
               - "Sign for zero" → [[ISL: 0]]
            5. Always use UPPERCASE English words inside the ISL tags, even if the user speaks Bengali.
            6. Keep responses extremely concise and helpful. Do not lecture about how ISL works.
            7. Reply in the same language the user used (English or Bengali).
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

        // Create a placeholder AI message that will be streamed into
        val streamingMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(id = streamingMsgId, text = "", isUser = false)
        _uiState.update { it.copy(messages = it.messages + placeholderMsg, isGenerating = true) }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                if (conversation == null) {
                    val engine = LiteRTModelLoader.getOrLoad(context)
                    val config = ConversationConfig(
                        systemInstruction = Contents.of(ISL_SYSTEM_PROMPT)
                    )
                    conversation = engine.createConversation(config)
                }

                val inputMsg = Message.user(text.trim())
                val accumulated = StringBuilder()

                // Stream tokens as they are generated
                conversation!!.sendMessageAsync(inputMsg).collect { partialMessage ->
                    val delta = partialMessage.contents.contents.joinToString("") { part ->
                        when (part) {
                            is Content.Text -> part.text
                            else -> ""
                        }
                    }
                    accumulated.append(delta)

                    // Update the placeholder message with accumulated text
                    val currentText = accumulated.toString()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == streamingMsgId) msg.copy(text = currentText) else msg
                                }
                            )
                        }
                    }
                }

                // Persist the final complete message
                val finalText = accumulated.toString().ifBlank { "..." }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == streamingMsgId) msg.copy(text = finalText) else msg
                            }
                        )
                    }
                    persistMessage(ChatMessage(id = streamingMsgId, text = finalText, isUser = false))
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Inference failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Remove the empty placeholder and show error
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != streamingMsgId })
                    }
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

        // Create a placeholder AI message that will be streamed into
        val streamingMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(id = streamingMsgId, text = "", isUser = false)
        _uiState.update { it.copy(messages = it.messages + placeholderMsg, isGenerating = true) }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
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
                    "Reply ONLY with the exact ISL sign using this tag format: [[ISL: OBJECT_NAME]]. " +
                    "Do not include any other text, natural description, or explanation."
                
                // Use Message.user() with multimodal Contents
                val multimodalMessage = Message.user(Contents.of(
                    Content.ImageBytes(scaledBytes),
                    Content.Text(prompt)
                ))

                val accumulated = StringBuilder()

                // Stream tokens as they are generated
                conversation!!.sendMessageAsync(multimodalMessage).collect { partialMessage ->
                    val delta = partialMessage.contents.contents.joinToString("") { part ->
                        when (part) {
                            is Content.Text -> part.text
                            else -> ""
                        }
                    }
                    accumulated.append(delta)

                    val currentText = accumulated.toString()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == streamingMsgId) msg.copy(text = currentText) else msg
                                }
                            )
                        }
                    }
                }

                // Persist the final complete message
                val finalText = accumulated.toString().ifBlank { "..." }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == streamingMsgId) msg.copy(text = finalText) else msg
                            }
                        )
                    }
                    persistMessage(ChatMessage(id = streamingMsgId, text = finalText, isUser = false))
                }
            } catch (e: Exception) {
            
                Log.e(TAG, "Image inference failed natively", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Remove the empty placeholder and show error
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != streamingMsgId })
                    }
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

    fun startRecording(language: SpeechLanguage) {
        speechManager.language = language
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

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
