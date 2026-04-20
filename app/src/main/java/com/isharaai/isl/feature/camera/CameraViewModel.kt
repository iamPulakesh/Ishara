package com.isharaai.isl.feature.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isharaai.isl.feature.camera.CameraCaptureManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

// this is the view model for the camera screen
@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private val cameraManager = CameraCaptureManager(context)

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.bindToLifecycle(lifecycleOwner, previewView) { e ->
            _uiState.update { it.copy(error = "Camera error: ${e.message}") }
        }
    }

    // Capture a photo and return the Bitmap via callback
    fun capturePhoto(onCaptured: (Bitmap) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val bitmap = cameraManager.capturePhoto()
                onCaptured(bitmap)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Capture failed: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.shutdown()
    }
}
