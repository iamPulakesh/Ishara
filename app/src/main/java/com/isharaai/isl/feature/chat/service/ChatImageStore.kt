package com.isharaai.isl.feature.chat.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** Centralizes chat image persistence for gallery imports and camera captures. */
object ChatImageStore {

    private const val IMAGE_DIR = "chat_images"
    private const val JPEG_QUALITY = 85

    fun saveGalleryImage(context: Context, uri: Uri): String {
        val file = newImageFile(context, "gallery")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open selected image")

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    fun saveCameraImage(context: Context, bitmap: Bitmap): String {
        val file = newImageFile(context, "capture")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }
        return file.absolutePath
    }

    private fun newImageFile(context: Context, prefix: String): File {
        val imagesDir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        return File(imagesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    }
}
