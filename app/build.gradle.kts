plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Auto-download Sherpa-ONNX offline speech models from GitHub Releases.

val sherpaRelease = "https://github.com/iamPulakesh/Ishara/releases/download/Sherpa-ONNX-models"

data class ModelFile(val subDir: String, val name: String, val sizeBytes: Long)

val sherpaModels = listOf(
    // English (sherpa-en)
    ModelFile("sherpa-en", "decoder.onnx",       2_092_272),
    ModelFile("sherpa-en", "encoder.int8.onnx",  42_845_182),
    ModelFile("sherpa-en", "joiner.int8.onnx",   259_572),
    ModelFile("sherpa-en", "tokens.txt",         5_048),
    // Bengali (sherpa-bn)
    ModelFile("sherpa-bn", "decoder.onnx",       2_093_080),
    ModelFile("sherpa-bn", "encoder.onnx",       90_994_145),
    ModelFile("sherpa-bn", "joiner.onnx",        1_026_462),
    ModelFile("sherpa-bn", "tokens.txt",         6_252),
)

tasks.register("downloadSherpaModels") {
    group = "setup"
    description = "Downloads Sherpa-ONNX offline speech models if not already present."

    val assetsDir = file("src/main/assets")

    // Only run if at least one file is missing
    outputs.upToDateWhen {
        sherpaModels.all { model ->
            val f = File(assetsDir, "${model.subDir}/${model.name}")
            f.exists() && f.length() == model.sizeBytes
        }
    }

    doLast {
        sherpaModels.forEach { model ->
            val dest = File(assetsDir, "${model.subDir}/${model.name}")
            if (dest.exists() && dest.length() == model.sizeBytes) {
                logger.lifecycle(" ${model.subDir}/${model.name} already exists.. skipping.")
                return@forEach
            }
            dest.parentFile.mkdirs()
            val url = "$sherpaRelease/${model.subDir}--${model.name}" // e.g. sherpa-en--decoder.onnx
            logger.lifecycle(" Downloading ${model.subDir}/${model.name} (${model.sizeBytes / 1_048_576} MB) …")
            ant.invokeMethod("get", mapOf("src" to url, "dest" to dest, "verbose" to true))
            // Verify file size
            check(dest.length() == model.sizeBytes) {
                "Size mismatch for ${model.name}: expected ${model.sizeBytes}, got ${dest.length()}"
            }
            logger.lifecycle(" ${model.subDir}/${model.name} downloaded successfully.")
        }
    }
}

// Hook the download task into the build so it runs automatically
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("downloadSherpaModels")
}

android {
    namespace = "com.isharaai.isl"
    compileSdk = 35
    @Suppress("UnstableApiUsage")

    defaultConfig {
        applicationId = "com.isharaai.isl"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Model download (Gemma 4 E2B IT) — hosted on Cloudflare R2
        buildConfigField("String", "MODEL_DOWNLOAD_URL",
            "\"https://pub-aa44556322c4453ca4f838e7f610ab58.r2.dev/gemma-4-E2B-it.litertlm\"")
        buildConfigField("String", "MODEL_SHA256",
            "\"SKIP\"")  // Skip checksum until we compute the real SHA256
        buildConfigField("long", "MODEL_SIZE_BYTES", "2772275200L")

        // Don't compress ONNX model files in assets (saves RAM at runtime)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    aaptOptions {
        noCompress += listOf("onnx", "txt")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // Compose BOM — manages all Compose library versions
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.navigation)
    implementation(libs.compose.lifecycle)

    // Core
    implementation(libs.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // LiteRT-LM (on-device Gemma inference)
    implementation(libs.litertlm.android)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt Dependency Injection for
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)

    // Network (model download)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // WorkManager (background model download)
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // JSON parsing
    implementation(libs.gson)

    // Sherpa-ONNX (offline speech recognition) — from Maven Central
    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-android:1.10.39")
}
