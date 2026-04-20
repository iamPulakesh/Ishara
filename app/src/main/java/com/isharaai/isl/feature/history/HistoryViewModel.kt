package com.isharaai.isl.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isharaai.isl.feature.chat.ChatRepository
import com.isharaai.isl.core.db.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val sessions: Flow<List<ChatSessionEntity>> = chatRepository.getAllSessions()

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            chatRepository.deleteAllSessions()
        }
    }
}
