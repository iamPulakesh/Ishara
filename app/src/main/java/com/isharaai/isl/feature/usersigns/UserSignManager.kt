package com.isharaai.isl.feature.usersigns

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

/** Manages user-recorded ISL sign videos stored in internal storage. */
object UserSignManager {

    private const val SIGN_PREFIX = "sign_"
    private const val SIGN_EXTENSION = "mp4"

    private fun signDir(context: Context): File =
        File(context.filesDir, "user_signs").apply { mkdirs() }

    private fun signKey(word: String): String =
        word.lowercase()

    private fun signResourceName(word: String): String =
        "$SIGN_PREFIX${signKey(word)}"

    private fun signFile(context: Context, word: String): File =
        File(signDir(context), "${signResourceName(word)}.$SIGN_EXTENSION")

    fun getSignFile(context: Context, word: String): File? {
        val file = signFile(context, word)
        return if (file.exists()) file else null
    }

    fun bundledSignExists(context: Context, word: String): Boolean =
        getBundledSignResourceId(context, word) != 0

    fun userSignExists(context: Context, word: String): Boolean =
        getSignFile(context, word) != null

    fun resolveSignUri(context: Context, word: String): String {
        val resId = getBundledSignResourceId(context, word)
        if (resId != 0) return "android.resource://${context.packageName}/$resId"
        return getSignFile(context, word)?.let { Uri.fromFile(it).toString() }.orEmpty()
    }

    fun saveSign(context: Context, word: String, inputStream: InputStream): Boolean {
        val dest = signFile(context, word)
        return try {
            inputStream.use { src -> dest.outputStream().use { src.copyTo(it) } }
            true
        } catch (_: Exception) { false }
    }

    fun deleteSign(context: Context, word: String): Boolean =
        signFile(context, word).delete()

    fun getAllUserSigns(context: Context): List<String> =
        signDir(context).listFiles()
            ?.filter { it.extension == SIGN_EXTENSION }
            ?.map { it.nameWithoutExtension.removePrefix(SIGN_PREFIX).uppercase() }
            ?.sorted() ?: emptyList()

    private fun getBundledSignResourceId(context: Context, word: String): Int =
        context.resources.getIdentifier(signResourceName(word), "raw", context.packageName)
}
