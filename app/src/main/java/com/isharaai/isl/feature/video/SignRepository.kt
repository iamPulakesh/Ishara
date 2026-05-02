package com.isharaai.isl.feature.video

import android.content.Context
import com.isharaai.isl.core.db.SignDao
import com.isharaai.isl.core.db.SignEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignRepository @Inject constructor(private val signDao: SignDao) {

    /**
     * Seeds the database from sign_index.json on first launch.
     * No-op if data already exists.
     */
    suspend fun seedIfEmpty(context: Context) {
        if (signDao.getAllSigns().isNotEmpty()) return

        try {
            val json = context.assets.open("sign_index.json").bufferedReader().readText()
            val type = object : TypeToken<List<SignEntity>>() {}.type
            val signs: List<SignEntity> = Gson().fromJson(json, type)
            signDao.insertAll(signs)
        } catch (e: Exception) {
            // Log but don't crash — app can still function with empty DB
            android.util.Log.e("SignRepository", "Failed to seed database", e)
        }
    }

    suspend fun getSign(signId: String): SignEntity? = signDao.getSign(signId)
}
