package com.isharaai.isl.core.inference

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Singleton loader for the LiteRT-LM Engine.
 * The `Engine` holds the massive LLM model weights in RAM (mapped natively via mmap).
 * Initializing it takes ~5-10 seconds so enforcing a Singleton pattern so that rotation, 
 * navigation and chat lifecycle events never accidentally reload the weights.
 */

object LiteRTModelLoader {

    @Volatile
    private var engine: Engine? = null

    // Mutex replaces synchronized{} — safe for suspend calls
    private val mutex = Mutex()

    suspend fun getOrLoad(context: Context): Engine = withContext(Dispatchers.IO) {
        engine ?: mutex.withLock {
            engine ?: createEngine(context).also { engine = it }
        }
    }
    // Create Engine
    private suspend fun createEngine(context: Context): Engine {
        val modelFile = ModelDownloadManager.getModelFile(context)
        require(modelFile.exists()) { "Model file not found at ${modelFile.absolutePath}. Download required." }

        android.util.Log.i("LiteRTLoader", "Loading model from: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")

        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            
            // Explicitly initialize the Vision Graph.
            // By default, LiteRT leaves the vision backend null to save RAM. If a user
            // sends a visual `Message.user(Contents.of(Content.ImageBytes...))` while it is null,
            // the C++ JNI bridge executes a null pointer dereference throwing a fatal SIGSEGV.
            visionBackend = Backend.CPU(),
            maxNumImages = 1
        )

        val eng = Engine(config)
        eng.initialize()
        android.util.Log.i("LiteRTLoader", "Engine initialized successfully")
        return eng
    }

    fun release() {
        engine?.close()
        engine = null
    }
}