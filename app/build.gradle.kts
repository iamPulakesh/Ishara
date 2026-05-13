import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URI
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Auto-download Sherpa-ONNX offline speech models from GitHub Releases.

val sherpaRelease = "https://github.com/iamPulakesh/Ishara/releases/download/Sherpa-ONNX-models"

data class RemoteFile(val targetPath: String, val downloadName: String, val sizeBytes: Long)

val filesToDownload = listOf(
    // AAR Library
    RemoteFile("libs/sherpa-onnx.aar", "sherpa-onnx.aar", 56_469_869),
    
    // English Models (sherpa-en)
    RemoteFile("src/main/assets/sherpa-en/decoder.onnx",       "sherpa-en--decoder.onnx",       2_092_272),
    RemoteFile("src/main/assets/sherpa-en/encoder.int8.onnx",  "sherpa-en--encoder.int8.onnx",  42_845_182),
    RemoteFile("src/main/assets/sherpa-en/joiner.int8.onnx",   "sherpa-en--joiner.int8.onnx",   259_572),
    RemoteFile("src/main/assets/sherpa-en/tokens.txt",         "sherpa-en--tokens.txt",         5_048),
    // Bengali Models (sherpa-bn)
    RemoteFile("src/main/assets/sherpa-bn/decoder.onnx",       "sherpa-bn--decoder.onnx",       2_093_080),
    RemoteFile("src/main/assets/sherpa-bn/encoder.onnx",       "sherpa-bn--encoder.onnx",       90_994_145),
    RemoteFile("src/main/assets/sherpa-bn/joiner.onnx",        "sherpa-bn--joiner.onnx",        1_026_462),
    RemoteFile("src/main/assets/sherpa-bn/tokens.txt",         "sherpa-bn--tokens.txt",         6_252),
)

tasks.register("downloadSherpaFiles") {
    group = "setup"
    description = "Downloads Sherpa-ONNX models and AAR library."

    // Capture everything into local vals
    val baseUrl = sherpaRelease
    val downloads = filesToDownload.map { remote ->
        Triple(file(remote.targetPath), remote.downloadName, remote.sizeBytes)
    }

    outputs.upToDateWhen {
        downloads.all { (dest, _, expectedSize) ->
            dest.exists() && dest.length() == expectedSize
        }
    }

    doLast {
        downloads.forEach { (dest, downloadName, expectedSize) ->
            if (dest.exists() && dest.length() == expectedSize) {
                logger.lifecycle(" ${dest.name} already exists.. skipping.")
                return@forEach
            }
            dest.parentFile.mkdirs()
            val url = "$baseUrl/$downloadName"
            logger.lifecycle(" Downloading ${dest.name} (${expectedSize / 1_048_576} MB)…")

            URI(url).toURL().openStream().use { input: java.io.InputStream ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }

            check(dest.length() == expectedSize) {
                "Size mismatch for $downloadName: expected $expectedSize, got ${dest.length()}"
            }
            logger.lifecycle(" ${dest.name} downloaded successfully.")
        }
    }
}

// ISL Sign Language video clips — downloaded interactively from GitHub Releases.

val islRelease = "https://github.com/iamPulakesh/Ishara/releases/download/ISL-sign-videos"
val islZipFile = file("src/main/res/raw/isl-sign-videos.zip")
val islRawDir  = file("src/main/res/raw")
val islExpectedSize = 174_839_808L   // ~174 MB zip
val islDownloadFlag = providers.gradleProperty("isl.download").orNull

tasks.register("downloadISLVideos") {
    group = "setup"
    description = "Downloads ISL sign language video clips (interactive — asks before downloading)."
    notCompatibleWithConfigurationCache("Uses System.console() for interactive input")

    outputs.upToDateWhen {
        // Skip if at least some sign videos already exist
        islRawDir.exists() && (islRawDir.listFiles()?.count { it.extension == "mp4" } ?: 0) > 10
    }

    doLast {
        // Check if videos already exist
        val existingCount = islRawDir.listFiles()?.count { it.extension == "mp4" } ?: 0
        if (existingCount > 10) {
            logger.lifecycle(" ISL videos already present ($existingCount clips). Skipping.")
            return@doLast
        }

        // Check for -Pisl.download=true flag first
        if (islDownloadFlag?.trim()?.lowercase()?.startsWith("y") == true ||
            islDownloadFlag?.trim()?.lowercase() == "true") {
            // Flag provided, proceed to download
        } else {
            // Interactive prompt via stdin
            print("\n Do you want to download default ISL sign videos? [y/N]: ")
            System.out.flush()
            val answer = try {
                BufferedReader(InputStreamReader(System.`in`)).readLine()
            } catch (_: Exception) { null }

            if (answer?.trim()?.lowercase()?.startsWith("y") != true) {
                logger.lifecycle(" Skipping ISL video download...")
                logger.lifecycle(" Run later with: ./gradlew downloadISLVideos -Pisl.download=true")
                return@doLast
            }
        }

        // Download
        islRawDir.mkdirs()
        val zipDest = islZipFile
        val url = "$islRelease/isl-sign-videos.zip"
        logger.lifecycle(" Downloading ISL sign videos…")

        URI(url).toURL().openStream().use { input ->
            FileOutputStream(zipDest).use { output ->
                input.copyTo(output)
            }
        }
        logger.lifecycle(" Download complete. Extracting…")

        // Extract zip
        ZipFile(zipDest).use { zip ->
            zip.entries().asSequence().forEach { entry: ZipEntry ->
                if (!entry.isDirectory && entry.name.endsWith(".mp4")) {
                    val outFile = File(islRawDir, File(entry.name).name)
                    zip.getInputStream(entry).use { src ->
                        outFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                }
            }
        }
        zipDest.delete()

        val count = islRawDir.listFiles()?.count { it.extension == "mp4" } ?: 0
        logger.lifecycle(" Extracted $count ISL sign videos successfully.")
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("downloadSherpaFiles", "downloadISLVideos")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
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
            "\"ab7838cdfc8f77e54d8ca45eadceb20452d9f01e4bfade03e5dce27911b27e42\"")
        buildConfigField("long", "MODEL_SIZE_BYTES", "2772275200L")

        // Don't compress ONNX model files in assets (saves RAM at runtime)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    androidResources {
        noCompress += listOf("onnx", "txt")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_STORE_FILE", "ishara-release.jks"))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "ishara")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    // WorkManager (background model download)
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Sherpa-ONNX (offline speech recognition) - auto-downloaded locally
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
