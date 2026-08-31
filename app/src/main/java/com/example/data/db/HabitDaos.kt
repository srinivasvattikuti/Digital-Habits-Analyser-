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
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.HabitAnalyticsSnapshotEntity
import com.example.data.model.IncrementalAiAnalysisMemoryEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
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

    @Query("SELECT * FROM daily_aggregates ORDER BY dateStr DESC")
    suspend fun getAllAggregatesSync(): List<DailyAggregateEntity>

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

    @Query("SELECT * FROM habit_insights ORDER BY timestamp DESC")
    suspend fun getAllInsightsSync(): List<HabitInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: HabitInsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<HabitInsightEntity>)

    @Query("DELETE FROM habit_insights")
    suspend fun clearAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

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

@Dao
interface HabitGoalDao {
    @Query("SELECT * FROM habit_goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<HabitGoalEntity>>

    @Query("SELECT * FROM habit_goals ORDER BY id ASC")
    suspend fun getAllGoalsSync(): List<HabitGoalEntity>

    @Query("SELECT * FROM habit_goals WHERE userId = :userId ORDER BY id ASC")
    fun getGoalsForUser(userId: String): Flow<List<HabitGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<HabitGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: HabitGoalEntity)

    @Query("UPDATE habit_goals SET targetValue = :targetValue WHERE id = :id")
    suspend fun updateGoalTarget(id: String, targetValue: Int)

    @Query("UPDATE habit_goals SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateGoalEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM habit_goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("DELETE FROM habit_goals")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isActiveProfile = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isActiveProfile = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun getUserProfileByUserId(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfileByUserIdSync(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    suspend fun getAllProfilesSync(): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(profiles: List<UserProfileEntity>)

    @Query("UPDATE user_profiles SET isActiveProfile = CASE WHEN id = :profileId THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(profileId: Int)

    @Query("DELETE FROM user_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Int)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAll()
}

@Dao
interface HabitAnalyticsSnapshotDao {
    @Query("SELECT * FROM analytics_snapshots WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSnapshots(userId: String = "current_user"): Flow<List<HabitAnalyticsSnapshotEntity>>

    @Query("SELECT * FROM analytics_snapshots WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(userId: String = "current_user"): Flow<HabitAnalyticsSnapshotEntity?>

    @Query("SELECT * FROM analytics_snapshots WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshotSync(userId: String = "current_user"): HabitAnalyticsSnapshotEntity?

    @Query("SELECT * FROM analytics_snapshots WHERE userId = :userId AND isBaselineAnchor = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getBaselineAnchorSnapshot(userId: String = "current_user"): HabitAnalyticsSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: HabitAnalyticsSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<HabitAnalyticsSnapshotEntity>)

    @Query("DELETE FROM analytics_snapshots WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String = "current_user")
}

@Dao
interface IncrementalAiMemoryDao {
    @Query("SELECT * FROM incremental_ai_memory WHERE userId = :userId LIMIT 1")
    fun getMemory(userId: String = "current_user"): Flow<IncrementalAiAnalysisMemoryEntity?>

    @Query("SELECT * FROM incremental_ai_memory WHERE userId = :userId LIMIT 1")
    suspend fun getMemorySync(userId: String = "current_user"): IncrementalAiAnalysisMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: IncrementalAiAnalysisMemoryEntity)

    @Query("DELETE FROM incremental_ai_memory WHERE userId = :userId")
    suspend fun clearMemory(userId: String = "current_user")
}

