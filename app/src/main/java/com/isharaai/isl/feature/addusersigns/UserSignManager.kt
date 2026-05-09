package com.isharaai.isl.feature.addusersigns

import android.content.Context
import java.io.File
import java.io.InputStream

/** Manages user-recorded ISL sign videos stored in internal storage. */
object UserSignManager {

    private fun signDir(context: Context): File =
        File(context.filesDir, "user_signs").apply { mkdirs() }

    fun getSignFile(context: Context, word: String): File? {
        val file = File(signDir(context), "sign_${word.lowercase()}.mp4")
        return if (file.exists()) file else null
    }

    fun bundledSignExists(context: Context, word: String): Boolean =
        context.resources.getIdentifier("sign_${word.lowercase()}", "raw", context.packageName) != 0

    fun userSignExists(context: Context, word: String): Boolean =
        getSignFile(context, word) != null

    fun saveSign(context: Context, word: String, inputStream: InputStream): Boolean {
        val dest = File(signDir(context), "sign_${word.lowercase()}.mp4")
        return try {
            inputStream.use { src -> dest.outputStream().use { src.copyTo(it) } }
            true
        } catch (_: Exception) { false }
    }

    fun deleteSign(context: Context, word: String): Boolean =
        File(signDir(context), "sign_${word.lowercase()}.mp4").delete()

    fun getAllUserSigns(context: Context): List<String> =
        signDir(context).listFiles()
            ?.filter { it.extension == "mp4" }
            ?.map { it.nameWithoutExtension.removePrefix("sign_").uppercase() }
            ?.sorted() ?: emptyList()
}
