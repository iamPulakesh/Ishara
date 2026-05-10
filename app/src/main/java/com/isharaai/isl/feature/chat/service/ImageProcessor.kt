package com.isharaai.isl.feature.chat.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/** Scales camera images to avoid OOM in LiteRT native inference. */
object ImageProcessor {

    private const val MAX_DIM = 384

    fun scaleToBytes(path: String): ByteArray {
        val original = BitmapFactory.decodeFile(path)
            ?: throw IllegalStateException("Failed to decode image")
        try {
            val scale = MAX_DIM.toFloat() / maxOf(original.width, original.height)
            if (scale >= 1f) {
                val out = ByteArrayOutputStream()
                original.compress(Bitmap.CompressFormat.JPEG, 90, out)
                return out.toByteArray()
            }
            val scaled = Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
            try {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                return out.toByteArray()
            } finally { scaled.recycle() }
        } finally { original.recycle() }
    }
}
