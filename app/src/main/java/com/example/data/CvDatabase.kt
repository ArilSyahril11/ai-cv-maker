package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters as RoomTypeConverters

@Database(entities = [CvDraft::class], version = 1, exportSchema = false)
@RoomTypeConverters(TypeConverters::class)
abstract class CvDatabase : RoomDatabase() {
    abstract fun cvDao(): CvDao

    companion object {
        @Volatile
        private var INSTANCE: CvDatabase? = null

        fun getDatabase(context: Context): CvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CvDatabase::class.java,
                    "cv_maker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
