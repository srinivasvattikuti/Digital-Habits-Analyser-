package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitAnalyticsSnapshotEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.IncrementalAiAnalysisMemoryEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import com.example.data.sdui.DashboardLayoutEntity
import com.example.data.sdui.DashboardSduiDao

@Database(
    entities = [
        AppInfoEntity::class,
        UsageEventEntity::class,
        DailyAggregateEntity::class,
        HabitInsightEntity::class,
        ChatMessageEntity::class,
        AppRecommendationEntity::class,
        HabitGoalEntity::class,
        UserProfileEntity::class,
        DashboardLayoutEntity::class,
        HabitAnalyticsSnapshotEntity::class,
        IncrementalAiAnalysisMemoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dailyAggregateDao(): DailyAggregateDao
    abstract fun habitInsightDao(): HabitInsightDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appRecommendationDao(): AppRecommendationDao
    abstract fun habitGoalDao(): HabitGoalDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dashboardSduiDao(): DashboardSduiDao
    abstract fun analyticsSnapshotDao(): HabitAnalyticsSnapshotDao
    abstract fun incrementalAiMemoryDao(): IncrementalAiMemoryDao

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
