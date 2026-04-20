package com.isharaai.isl.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SignEntity::class, ChatSessionEntity::class, ChatMessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SignDatabase : RoomDatabase() {

    abstract fun signDao(): SignDao
    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): SignDatabase {
            return Room.databaseBuilder(
                context,
                SignDatabase::class.java,
                "ishara_signs.db"
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }
    }
}
