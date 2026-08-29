package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity

@Database(
    entities = [
        AppInfoEntity::class,
        UsageEventEntity::class,
        DailyAggregateEntity::class,
        HabitInsightEntity::class,
        ChatMessageEntity::class,
        AppRecommendationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dailyAggregateDao(): DailyAggregateDao
    abstract fun habitInsightDao(): HabitInsightDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appRecommendationDao(): AppRecommendationDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "digital_habits_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
