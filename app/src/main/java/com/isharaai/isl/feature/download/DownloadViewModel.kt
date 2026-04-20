package com.isharaai.isl.feature.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.isharaai.isl.core.inference.ModelDownloadManager
import com.isharaai.isl.core.inference.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadUiState(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val isComplete: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    init {
        if (ModelDownloadManager.isModelReady(application)) {
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    fun startDownload() {
        // Cancel any previous failed/stuck work before re-enqueuing
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_NAME)

        val request = ModelDownloadWorker.buildRequest()
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        _uiState.update { it.copy(isDownloading = true, isError = false, errorMessage = null, progress = 0) }

        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.ENQUEUED -> {
                            // Worker is queued — still waiting to start
                            _uiState.update { it.copy(isDownloading = true) }
                        }
                        WorkInfo.State.RUNNING -> {
                            val progress = info.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                            _uiState.update { it.copy(progress = progress, isDownloading = true) }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.update { it.copy(isComplete = true, isDownloading = false) }
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMsg = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                                ?: "ডাউনলোড ব্যর্থ হয়েছে।"
                            _uiState.update {
                                it.copy(isError = true, isDownloading = false, errorMessage = errorMsg)
                            }
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.update {
                                it.copy(isError = false, isDownloading = false)
                            }
                        }
                        else -> {}
                    }
                }
        }
    }
}
