package com.isharaai.isl.feature.video

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bengali Text-to-Speech manager using Android's built-in TTS engine.
 * Speaks Bengali text with callback support for completion.
 */
@Singleton
class BengaliTTSManager @Inject constructor(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("bn", "IN"))
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                           result != TextToSpeech.LANG_NOT_SUPPORTED

                // Fallback: try generic Bengali locale if India-specific is unavailable
                if (!isReady) {
                    val fallbackResult = tts?.setLanguage(Locale("bn"))
                    isReady = fallbackResult != TextToSpeech.LANG_MISSING_DATA &&
                               fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        }
    }

    // speak function
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            onDone?.invoke()
            return
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone?.invoke() }
            @Deprecated("Deprecated in API")
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {}
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ishara_tts_${System.currentTimeMillis()}")
    }

    fun stop() { tts?.stop() }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
