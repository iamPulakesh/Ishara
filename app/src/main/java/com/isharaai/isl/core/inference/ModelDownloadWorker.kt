package com.isharaai.isl.core.inference

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.isharaai.isl.BuildConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * WorkManager worker that downloads the Gemma LiteRT-LM model file
 * in the background with resume support.
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROGRESS = "download_progress"
        const val KEY_ERROR = "download_error"
        const val WORK_NAME = "model_download"

        // Build a work request for downloading the model
        fun buildRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                // Expedited = highest priority, bypasses battery/background throttling
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
    }

    // Required for expedited workers on Android 12+
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "model_download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Model Download", NotificationManager.IMPORTANCE_LOW
            )
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("IsharaAI")
            .setContentText("AI মডেল ডাউনলোড হচ্ছে...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1, notification)
    }

    override suspend fun doWork(): Result {
        val modelFile = ModelDownloadManager.getModelFile(applicationContext)
        modelFile.parentFile?.mkdirs()

        val tempFile = File(modelFile.parent, "${ModelDownloadManager.MODEL_FILENAME}.tmp")
        val downloadUrl = BuildConfig.MODEL_DOWNLOAD_URL

        android.util.Log.i("ModelDownload", "Starting download from: $downloadUrl")

        return try {
            val request = Request.Builder()
                .url(downloadUrl)
                .apply {
                    // Resume support — send Range header if partial download exists
                    if (tempFile.exists() && tempFile.length() > 0) {
                        header("Range", "bytes=${tempFile.length()}-")
                        android.util.Log.i("ModelDownload", "Resuming from byte ${tempFile.length()}")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            android.util.Log.i("ModelDownload", "HTTP response: ${response.code} ${response.message}")

            if (!response.isSuccessful && response.code != 206) {
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                android.util.Log.e("ModelDownload", "Download failed: $errorMsg")
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

            val totalBytes = BuildConfig.MODEL_SIZE_BYTES
            var downloadedBytes = if (tempFile.exists() && response.code == 206) tempFile.length() else 0L

            // If server doesn't support Range, restart from scratch
            if (response.code == 200 && tempFile.exists()) {
                tempFile.delete()
                downloadedBytes = 0L
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile, response.code == 206).use { output ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) return Result.retry()
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = ((downloadedBytes.toFloat() / totalBytes) * 100).toInt()
                            .coerceIn(0, 100)
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
            } ?: run {
                android.util.Log.e("ModelDownload", "Response body is null")
                return Result.failure(workDataOf(KEY_ERROR to "Empty response from server"))
            }

            android.util.Log.i("ModelDownload", "Download complete. Verifying checksum...")

            // Verify checksum after full download
            if (!ModelDownloadManager.verifyChecksum(tempFile)) {
                android.util.Log.e("ModelDownload", "Checksum verification failed")
                tempFile.delete()
                return Result.failure(workDataOf(KEY_ERROR to "Checksum verification failed"))
            }

            // Atomically rename temp file to final model file
            tempFile.renameTo(modelFile)
            android.util.Log.i("ModelDownload", "Model ready at ${modelFile.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ModelDownload", "Download error: ${e.message}", e)
            // Network errors → retry with backoff (up to WorkManager's limit)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to "Download failed after retries: ${e.message}"))
            }
        }
    }
}
