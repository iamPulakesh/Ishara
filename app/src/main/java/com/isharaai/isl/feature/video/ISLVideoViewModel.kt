package com.isharaai.isl.feature.video

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.isharaai.isl.feature.video.SignRepository
import com.isharaai.isl.core.db.SignEntity
import com.isharaai.isl.feature.video.BengaliTTSManager
import com.isharaai.isl.feature.video.ISLVideoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI state for video screen
data class ISLVideoUiState(
    val signEntity: SignEntity? = null,
    val player: ExoPlayer? = null
)

@HiltViewModel
class ISLVideoViewModel @Inject constructor(
    private val signRepository: SignRepository,
    private val islVideoPlayer: ISLVideoPlayer,
    private val ttsManager: BengaliTTSManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ISLVideoUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSign(signId: String) {
        viewModelScope.launch {
            val sign = signRepository.getSign(signId)
            _uiState.update { it.copy(signEntity = sign) }
            sign?.let { ttsManager.speak(it.ttsPhrase) }
        }
    }

    fun playVideo(context: Context, resId: Int) {
        islVideoPlayer.prepareAndPlay(resId) {
            // Video playback complete — could show replay prompt
        }
        _uiState.update { it.copy(player = islVideoPlayer.getPlayer()) }
    }

    override fun onCleared() {
        super.onCleared()
        islVideoPlayer.release()
        ttsManager.stop()
    }
}
