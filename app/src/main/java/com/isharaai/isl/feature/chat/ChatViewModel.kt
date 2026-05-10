package com.isharaai.isl.feature.chat

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import com.isharaai.isl.R
import com.isharaai.isl.core.model.ModelDownloadManager
import com.isharaai.isl.core.speech.HybridSpeechManager
import com.isharaai.isl.core.speech.SpeechLanguage
import com.isharaai.isl.feature.chat.service.ImageProcessor
import com.isharaai.isl.feature.chat.service.InferenceService
import com.isharaai.isl.feature.chat.service.SessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionService: SessionService,
    private val inferenceService: InferenceService
) : ViewModel() {

    companion object { private const val TAG = "ChatViewModel" }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startNewSession()
        refreshModelStatus()
        preloadEngine()
    }

    private fun preloadEngine() {
        if (!ModelDownloadManager.isModelReady(context)) return
        viewModelScope.launch { try { inferenceService.ensureReady() } catch (_: Exception) {} }
    }

    fun refreshModelStatus() {
        _uiState.update { it.copy(isModelReady = ModelDownloadManager.isModelReady(context)) }
    }

    // Session management

    fun startNewSession(title: String = SessionService.DEFAULT_SESSION_TITLE) {
        inferenceService.close()
        _uiState.update { ChatUiState(isModelReady = ModelDownloadManager.isModelReady(context)) }

        viewModelScope.launch {
            val id = sessionService.createSession(title)
            _uiState.update { it.copy(sessionId = id) }
        }

        addSystemMessage(
            if (ModelDownloadManager.isModelReady(context)) R.string.chat_welcome
            else R.string.chat_welcome_no_model
        )
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val messages = sessionService.loadMessages(sessionId)
            _uiState.update { it.copy(sessionId = sessionId, messages = messages) }
        }
    }

    // Messaging

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        addUserMessage(trimmed)
        viewModelScope.launch { sessionService.updateTitleIfNew(_uiState.value.sessionId, trimmed) }
        if (!_uiState.value.isModelReady) { addSystemMessage(R.string.chat_model_needed); return }
        streamResponse(Message.user(trimmed))
    }

    fun sendImage(imagePath: String) {
        val msg = ChatMessage(imagePath = imagePath, isUser = true)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
        if (!_uiState.value.isModelReady) { addSystemMessage(R.string.chat_model_needed); return }

        val scaledBytes = ImageProcessor.scaleToBytes(imagePath)
        val prompt = "Identify the main object or action in this image. " +
            "Reply ONLY with the exact ISL sign using this tag format: [[ISL: OBJECT_NAME]]. " +
            "Do not include any other text, natural description, or explanation."
        streamResponse(Message.user(Contents.of(Content.ImageBytes(scaledBytes), Content.Text(prompt))))
    }

    private fun streamResponse(message: Message) {
        val msgId = UUID.randomUUID().toString()
        _uiState.update { it.copy(messages = it.messages + ChatMessage(id = msgId, text = "", isUser = false), isGenerating = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                inferenceService.streamResponse(message).collect { snapshot ->
                    withContext(Dispatchers.Main) {
                        _uiState.update { s -> s.copy(messages = s.messages.map { if (it.id == msgId) it.copy(text = snapshot) else it }) }
                    }
                }
                val finalText = _uiState.value.messages.find { it.id == msgId }?.text?.ifBlank { "..." } ?: "..."
                withContext(Dispatchers.Main) {
                    persistMessage(ChatMessage(id = msgId, text = finalText, isUser = false))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { s -> s.copy(messages = s.messages.filter { it.id != msgId }) }
                    addSystemMessage(R.string.chat_error)
                }
            } finally {
                withContext(Dispatchers.Main) { _uiState.update { it.copy(isGenerating = false) } }
            }
        }
    }

    // Helper: add messages to UI + persist

    private fun addUserMessage(text: String) {
        val msg = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
    }

    private fun addSystemMessage(@StringRes resId: Int) {
        val msg = ChatMessage(stringResId = resId, isUser = false)
        _uiState.update { it.copy(messages = it.messages + msg) }
        persistMessage(msg)
    }

    private fun persistMessage(msg: ChatMessage) {
        val sessionId = _uiState.value.sessionId
        viewModelScope.launch { sessionService.persistMessage(sessionId, msg) }
    }

    // Speech recognition

    private val speechManager = HybridSpeechManager(context).apply {
        setListener(object : HybridSpeechManager.Listener {
            override fun onPartialResult(text: String) { _uiState.update { it.copy(partialTranscript = text) } }
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
        if (finalText.isNotBlank()) sendMessage(finalText)
    }

    override fun onCleared() {
        super.onCleared()
        inferenceService.close()
        speechManager.release()
    }
}
