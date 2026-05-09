package com.isharaai.isl.core.speech

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import kotlin.concurrent.thread

// Language supported by the Hybrid Speech Manager.

enum class SpeechLanguage(
    val googleLocale: String,
    val sherpaModelDir: String,
    val label: String
) {
    BENGALI("bn-IN", "sherpa-bn", "বাং"),
    ENGLISH("en-US", "sherpa-en", "EN");
}

/**
 * Hybrid Speech Manager: routes between online (Google) and offline (Sherpa-ONNX).
 *
 * If network is available it uses Android's built-in SpeechRecognizer (Google Cloud)
 * If offline it uses Sherpa-ONNX with the bundled model for the selected language
 *
 * Both paths call the same [Listener] callbacks.
 */
class HybridSpeechManager(private val context: Context) {

    companion object {
        private const val TAG = "HybridSpeech"
        private const val SAMPLE_RATE = 16000
    }

    interface Listener {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(message: String)
    }

    private var listener: Listener? = null
    private var isRunning = false
    var language: SpeechLanguage = SpeechLanguage.BENGALI

    // Online Path (Google SpeechRecognizer)
    private var speechRecognizer: SpeechRecognizer? = null
    private val onlineAccumulated = StringBuilder()
    @Volatile private var onlineContinuous = false

    // Offline Path (Sherpa-ONNX)
    private var sherpaRecognizer: OnlineRecognizer? = null
    private var sherpaLoadedLang: SpeechLanguage? = null   // track which model is loaded
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var sherpaRecording = false
    private val sherpaAccumulated = StringBuilder()

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        // Dynamic Model Routing based on network state
        if (isNetworkAvailable()) {
            Log.i(TAG, "Network available so using Google SpeechRecognizer (${language.googleLocale})")
            // Unload Sherpa from native RAM if network is solid
            releaseSherpa()
            startOnline()
        } else {
            Log.i(TAG, "No network so using Sherpa-ONNX offline (${language.sherpaModelDir})")
            startOffline()
        }
    }

    fun stop(): String {
        isRunning = false
        return if (onlineContinuous) {
            stopOnline()
        } else {
            stopOffline()
        }
    }

    // Release all resources
    fun release() {
        isRunning = false
        stopOnline()
        stopOffline()
        releaseSherpa()
    }

    // Unload Sherpa-ONNX model
    private fun releaseSherpa() {
        if (sherpaRecognizer != null) {
            Log.i(TAG, "Releasing Sherpa-ONNX model from memory")
            sherpaRecognizer?.release()
            sherpaRecognizer = null
            sherpaLoadedLang = null
        }
    }

    // Online Path (Google built-in Speech Recognizer)

    private fun startOnline() {
        onlineAccumulated.clear()
        onlineContinuous = true
        startOnlineSession()
    }

    private fun stopOnline(): String {
        onlineContinuous = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        return onlineAccumulated.toString().trim()
    }


    private fun startOnlineSession() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.googleLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.googleLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                val preview = (onlineAccumulated.toString() + " " + partial).trim()
                listener?.onPartialResult(preview)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    if (onlineAccumulated.isNotEmpty()) onlineAccumulated.append(" ")
                    onlineAccumulated.append(text)
                    listener?.onPartialResult(onlineAccumulated.toString())
                }
                // Restart if still recording
                if (onlineContinuous) startOnlineSession()
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (onlineContinuous) startOnlineSession()
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        // Network failed mid-recording. Fall back to offline
                        Log.w(TAG, "Network error during online recognition, falling back to offline")
                        speechRecognizer?.destroy()
                        speechRecognizer = null
                        onlineContinuous = false
                        startOffline()
                    }
                    else -> {
                        if (onlineContinuous) startOnlineSession()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    // Offline Path (Sherpa-ONNX + AudioRecord)

    private fun initSherpaIfNeeded() {
        // Hot-swapping mechanism: Unload previous language model and load the requested language
        // to prevent chanches of OOM errors holding two ONNX graphs in RAM.
        if (sherpaRecognizer != null && sherpaLoadedLang != language) {
            Log.i(TAG, "Language changed from ${sherpaLoadedLang?.name} to ${language.name}, reloading model")
            releaseSherpa()
        }
        if (sherpaRecognizer != null) return

        Log.i(TAG, "Initializing Sherpa-ONNX model: ${language.sherpaModelDir}")
        val modelDir = language.sherpaModelDir

        val config = when (language) {
            SpeechLanguage.BENGALI -> OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "$modelDir/encoder.onnx",
                        decoder = "$modelDir/decoder.onnx",
                        joiner = "$modelDir/joiner.onnx",
                    ),
                    tokens = "$modelDir/tokens.txt",
                    numThreads = 2,
                    modelType = "zipformer2",
                ),
                endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(false, 2.4f, 0.0f),
                    rule2 = EndpointRule(true, 1.4f, 0.0f),
                    rule3 = EndpointRule(false, 0.0f, 20.0f)
                ),
                enableEndpoint = true,
            )
            SpeechLanguage.ENGLISH -> OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "$modelDir/encoder.int8.onnx",
                        decoder = "$modelDir/decoder.onnx",
                        joiner = "$modelDir/joiner.int8.onnx",
                    ),
                    tokens = "$modelDir/tokens.txt",
                    numThreads = 2,
                    modelType = "zipformer",
                ),
                endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(false, 2.4f, 0.0f),
                    rule2 = EndpointRule(true, 1.4f, 0.0f),
                    rule3 = EndpointRule(false, 0.0f, 20.0f)
                ),
                enableEndpoint = true,
            )
        }

        sherpaRecognizer = OnlineRecognizer(
            assetManager = context.assets,
            config = config,
        )
        sherpaLoadedLang = language
        Log.i(TAG, "Sherpa-ONNX model initialized for ${language.name}")
    }

    private fun startOffline() {
        try {
            initSherpaIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Sherpa-ONNX", e)
            listener?.onError("Offline speech model failed to load")
            return
        }

        sherpaAccumulated.clear()
        sherpaRecording = true

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            audioRecord!!.startRecording()
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission denied", e)
            listener?.onError("Microphone permission required")
            return
        }

        recordingThread = thread(name = "SherpaRecording") {
            processAudioLoop()
        }
    }

    private fun stopOffline(): String {
        sherpaRecording = false
        recordingThread?.join(2000)
        recordingThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        return sherpaAccumulated.toString().trim()
    }

    // this is the main loop for offline speech recognition
    private fun processAudioLoop() {
        val recognizer = sherpaRecognizer ?: return
        val stream = recognizer.createStream()

        val interval = 0.1 // 100ms chunks
        val bufferSize = (interval * SAMPLE_RATE).toInt()
        val buffer = ShortArray(bufferSize)
        var lastText = ""

        while (sherpaRecording) {
            val ret = audioRecord?.read(buffer, 0, buffer.size) ?: break
            if (ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)

                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }

                val isEndpoint = recognizer.isEndpoint(stream)
                var text = recognizer.getResult(stream).text

                if (isEndpoint && text.isNotBlank()) {
                    if (sherpaAccumulated.isNotEmpty()) sherpaAccumulated.append(" ")
                    sherpaAccumulated.append(text)
                    lastText = sherpaAccumulated.toString()
                    recognizer.reset(stream)
                }

                // Show accumulated + current partial
                val display = if (text.isNotBlank() && !isEndpoint) {
                    (sherpaAccumulated.toString() + " " + text).trim()
                } else {
                    lastText
                }

                if (display.isNotBlank()) {
                    listener?.onPartialResult(display)
                }
            }
        }

        // Grab any remaining text
        val finalText = recognizer.getResult(stream).text
        if (finalText.isNotBlank()) {
            if (sherpaAccumulated.isNotEmpty()) sherpaAccumulated.append(" ")
            sherpaAccumulated.append(finalText)
        }

        stream.release()
    }

    // Network Check

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
