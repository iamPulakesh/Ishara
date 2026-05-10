package com.isharaai.isl.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 3,
    exportSchema = false
)
abstract class IsharaDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): IsharaDatabase {
            return Room.databaseBuilder(
                context,
                IsharaDatabase::class.java,
                "ishara_signs.db"
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }
    }
}
