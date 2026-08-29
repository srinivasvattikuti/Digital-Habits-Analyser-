package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM installed_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getApp(pkg: String): AppInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfoEntity>)

    @Query("SELECT COUNT(*) FROM installed_apps")
    suspend fun getAppCount(): Int
}

@Dao
interface UsageEventDao {
    @Query("SELECT * FROM usage_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<UsageEventEntity>>

    @Query("SELECT * FROM usage_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<UsageEventEntity>

    @Query("SELECT * FROM usage_events WHERE dateStr = :dateStr ORDER BY timestamp ASC")
    fun getEventsForDate(dateStr: String): Flow<List<UsageEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: UsageEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<UsageEventEntity>)

    @Query("DELETE FROM usage_events")
    suspend fun clearAll()
}

@Dao
interface DailyAggregateDao {
    @Query("SELECT * FROM daily_aggregates ORDER BY dateStr DESC, totalDurationMs DESC")
    fun getAllAggregates(): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates WHERE dateStr = :dateStr ORDER BY totalDurationMs DESC")
    fun getAggregatesForDate(dateStr: String): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates WHERE dateStr >= :startDate ORDER BY dateStr ASC")
    fun getAggregatesSince(startDate: String): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates WHERE dateStr >= :startDate ORDER BY dateStr ASC")
    suspend fun getAggregatesSinceSync(startDate: String): List<DailyAggregateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAggregates(aggregates: List<DailyAggregateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAggregate(aggregate: DailyAggregateEntity)

    @Query("DELETE FROM daily_aggregates")
    suspend fun clearAll()
}

@Dao
interface HabitInsightDao {
    @Query("SELECT * FROM habit_insights ORDER BY timestamp DESC LIMIT 1")
    fun getLatestInsight(): Flow<HabitInsightEntity?>

    @Query("SELECT * FROM habit_insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<HabitInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: HabitInsightEntity)

    @Query("DELETE FROM habit_insights")
    suspend fun clearAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface AppRecommendationDao {
    @Query("SELECT * FROM app_recommendations ORDER BY id ASC")
    fun getAllRecommendations(): Flow<List<AppRecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<AppRecommendationEntity>)

    @Query("DELETE FROM app_recommendations")
    suspend fun clearAll()
}
