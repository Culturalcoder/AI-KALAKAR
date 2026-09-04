package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ArtisanProfileEntity
import com.example.data.model.ProductEntity

@Database(
    entities = [ProductEntity::class, ArtisanProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ArtisanDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun artisanProfileDao(): ArtisanProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ArtisanDatabase? = null

        fun getInstance(context: Context): ArtisanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArtisanDatabase::class.java,
                    "ai_kalakar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
