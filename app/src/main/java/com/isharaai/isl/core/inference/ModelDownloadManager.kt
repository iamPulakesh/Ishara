package com.isharaai.isl.core.inference

import android.content.Context
import com.isharaai.isl.BuildConfig
import java.io.File
import java.security.MessageDigest

/**
 * Manages the on-device Gemma model file location, existence checks,
 * and SHA-256 checksum verification after download.
 */
object ModelDownloadManager {

    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    fun getModelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    // Check if model file exists and downloaded properly.

    fun isModelReady(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > 2_000_000_000L
    }

    // Full SHA-256 verification of the downloaded model file after download completes.

    fun verifyChecksum(file: File): Boolean {
        // Allow skipping checksum during development
        if (BuildConfig.MODEL_SHA256 == "SKIP") return true

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(65536).use { stream ->
            val buffer = ByteArray(65536)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val computed = digest.digest().joinToString("") { "%02x".format(it) }
        return computed == BuildConfig.MODEL_SHA256
    }
}
