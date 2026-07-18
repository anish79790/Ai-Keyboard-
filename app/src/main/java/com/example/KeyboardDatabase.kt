package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SemanticMemory::class, PredictionEntity::class], version = 2, exportSchema = false)
abstract class KeyboardDatabase : RoomDatabase() {

    abstract fun semanticMemoryDao(): SemanticMemoryDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        @Volatile
        private var INSTANCE: KeyboardDatabase? = null

        fun getInstance(context: Context): KeyboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyboardDatabase::class.java,
                    "keyboard_cognitive_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
