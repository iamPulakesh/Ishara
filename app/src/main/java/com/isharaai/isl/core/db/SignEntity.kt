package com.isharaai.isl.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signs")
data class SignEntity(
    @PrimaryKey val signId: String,        // e.g. "DOG"
    val bengaliWord: String,               // e.g. "কুকুর"
    val videoResName: String,              // e.g. "isl_dog" (maps to R.raw.isl_dog)
    val ttsPhrase: String,                 // Bengali TTS confirmation phrase
    val category: String                   // e.g. "animals", "needs", "family"
)
