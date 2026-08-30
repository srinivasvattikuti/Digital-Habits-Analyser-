package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiHabitsPipeline
import com.example.data.collector.HealthSensorCollector
import com.example.data.collector.SampleDataGenerator
import com.example.data.collector.UsageStatsCollector
import com.example.data.db.HabitDatabase
import com.example.data.firebase.AuthUserInfo
import com.example.data.firebase.CloudBackupData
import com.example.data.firebase.CloudSyncStatus
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.firebase.HabitAnalyticsManager
import com.example.data.firebase.SyncSummary
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitRepository(private val context: Context) {

    private val db = HabitDatabase.getDatabase(context)
    val usageCollector = UsageStatsCollector(context)
    val healthCollector = HealthSensorCollector(context)
    private val sampleGenerator = SampleDataGenerator(context)
    private val aiPipeline = AiHabitsPipeline()

    val analyticsManager = HabitAnalyticsManager(context)
    val authManager = FirebaseAuthManager(context, analyticsManager)
    val firestoreSync = FirestoreSyncManager(context, analyticsManager)

    val installedApps: Flow<List<AppInfoEntity>> = db.appInfoDao().getAllApps()
    val recentEvents: Flow<List<UsageEventEntity>> = db.usageEventDao().getRecentEvents(150)
    val allAggregates: Flow<List<DailyAggregateEntity>> = db.dailyAggregateDao().getAllAggregates()
    val latestInsight: Flow<HabitInsightEntity?> = db.habitInsightDao().getLatestInsight()
    val allInsights: Flow<List<HabitInsightEntity>> = db.habitInsightDao().getAllInsights()
    val chatMessages: Flow<List<ChatMessageEntity>> = db.chatMessageDao().getAllMessages()
    val recommendations: Flow<List<AppRecommendationEntity>> = db.appRecommendationDao().getAllRecommendations()
    val habitGoals: Flow<List<HabitGoalEntity>> = db.habitGoalDao().getAllGoals()
    val userProfile: Flow<UserProfileEntity?> = db.userProfileDao().getUserProfile()

    fun getAggregatesSince(startDate: String): Flow<List<DailyAggregateEntity>> =
        db.dailyAggregateDao().getAggregatesSince(startDate)

    fun getEventsForDate(dateStr: String): Flow<List<UsageEventEntity>> =
        db.usageEventDao().getEventsForDate(dateStr)

    suspend fun initializeDataIfEmpty() = withContext(Dispatchers.IO) {
        // Initialize default user profile if none exists
        if (db.userProfileDao().getUserProfileSync() == null) {
            db.userProfileDao().insertUserProfile(UserProfileEntity())
        }

        // Collect installed apps
        try {
            usageCollector.collectInstalledApps()
        } catch (e: Exception) {
            // Ignore
        }

        // Try collecting real usage
        val collectedReal = if (usageCollector.hasUsageStatsPermission()) {
            usageCollector.collectUsageEventsAndAggregates(daysBack = 7)
        } else false

        // If no real data available or empty, populate realistic demo time-series
        val count = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01").size
        if (count == 0) {
            sampleGenerator.populateRichDemoData(forceOverwrite = false)
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        db.userProfileDao().insertUserProfile(profile)
    }

    suspend fun getUserProfileSync(): UserProfileEntity = withContext(Dispatchers.IO) {
        db.userProfileDao().getUserProfileSync() ?: UserProfileEntity()
    }

    suspend fun refreshUsageTelemetry(): Boolean = withContext(Dispatchers.IO) {
        if (usageCollector.hasUsageStatsPermission()) {
            usageCollector.collectInstalledApps()
            usageCollector.collectUsageEventsAndAggregates(daysBack = 3)
            true
        } else {
            false
        }
    }

    suspend fun loadRichDemoData() = withContext(Dispatchers.IO) {
        sampleGenerator.populateRichDemoData(forceOverwrite = true)
        runAiHabitPipeline()
    }

    suspend fun runAiHabitPipeline(): HabitInsightEntity = withContext(Dispatchers.IO) {
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val events = db.usageEventDao().getEventsSince(System.currentTimeMillis() - 7 * 86400000L)
        val profile = getUserProfileSync()

        val result = aiPipeline.runHabitAnalysisPipeline(aggregates, events, profile)
        db.habitInsightDao().insertInsight(result.insight)
        if (result.recommendations.isNotEmpty()) {
            db.appRecommendationDao().insertRecommendations(result.recommendations)
        }
        result.insight
    }

    suspend fun sendChatMessage(userMessageText: String): String = withContext(Dispatchers.IO) {
        val userMsg = ChatMessageEntity(
            sender = "USER",
            message = userMessageText,
            timestamp = System.currentTimeMillis()
        )
        db.chatMessageDao().insertMessage(userMsg)

        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val events = db.usageEventDao().getEventsSince(System.currentTimeMillis() - 3 * 86400000L)
        val profile = getUserProfileSync()

        // Get past messages for context
        val messagesList = mutableListOf<ChatMessageEntity>()

        val aiAnswer = aiPipeline.answerConversationalQuery(
            query = userMessageText,
            chatHistory = messagesList,
            aggregates = aggregates,
            events = events,
            userProfile = profile
        )

        val aiMsg = ChatMessageEntity(
            sender = "AI",
            message = aiAnswer,
            timestamp = System.currentTimeMillis()
        )
        db.chatMessageDao().insertMessage(aiMsg)

        aiAnswer
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        db.chatMessageDao().clearAll()
    }

    suspend fun updateGoalTarget(goalId: String, newTarget: Int) = withContext(Dispatchers.IO) {
        db.habitGoalDao().updateGoalTarget(goalId, newTarget)
        analyticsManager.logGoalUpdated(goalId, "TARGET_ADJUST", newTarget)
    }

    suspend fun updateGoalEnabled(goalId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        db.habitGoalDao().updateGoalEnabled(goalId, isEnabled)
        analyticsManager.logGoalUpdated(goalId, if (isEnabled) "ENABLED" else "DISABLED", 0)
    }

    suspend fun insertGoal(goal: HabitGoalEntity) = withContext(Dispatchers.IO) {
        db.habitGoalDao().insertGoal(goal)
        analyticsManager.logGoalUpdated(goal.id, goal.goalType, goal.targetValue)
    }

    suspend fun backupToCloud(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val uid = authManager.currentUserId
        if (uid == null) {
            return@withContext Result.failure(IllegalStateException("Must be signed in to back up data to Firebase Firestore."))
        }

        val profile = getUserProfileSync()
        val goals = db.habitGoalDao().getAllGoalsSync()
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val insights = db.habitInsightDao().getAllInsightsSync()
        val chats = db.chatMessageDao().getAllMessagesSync()

        firestoreSync.backupLocalDataToCloud(
            userId = uid,
            profile = profile,
            goals = goals,
            aggregates = aggregates,
            insights = insights,
            chats = chats
        )
    }

    suspend fun restoreFromCloud(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val uid = authManager.currentUserId
        if (uid == null) {
            return@withContext Result.failure(IllegalStateException("Must be signed in to restore data from Firebase Firestore."))
        }

        val result = firestoreSync.restoreDataFromCloud(uid)
        if (result.isSuccess) {
            val data = result.getOrNull()
            if (data != null) {
                // Restore profile
                if (data.profile != null) {
                    db.userProfileDao().insertUserProfile(data.profile)
                }
                // Restore goals
                if (data.goals.isNotEmpty()) {
                    data.goals.forEach { db.habitGoalDao().insertGoal(it) }
                }
                // Restore aggregates
                if (data.aggregates.isNotEmpty()) {
                    db.dailyAggregateDao().insertAggregates(data.aggregates)
                }
                // Restore insights
                if (data.insights.isNotEmpty()) {
                    data.insights.forEach { db.habitInsightDao().insertInsight(it) }
                }
                // Restore chats
                if (data.chats.isNotEmpty()) {
                    data.chats.forEach { db.chatMessageDao().insertMessage(it) }
                }

                val summary = SyncSummary(
                    aggregatesCount = data.aggregates.size,
                    goalsCount = data.goals.size,
                    insightsCount = data.insights.size,
                    chatsCount = data.chats.size
                )
                Result.success(summary)
            } else {
                Result.failure(Exception("Cloud data payload was empty"))
            }
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Cloud restore failed"))
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.usageEventDao().clearAll()
        db.dailyAggregateDao().clearAll()
        db.habitInsightDao().clearAll()
        db.chatMessageDao().clearAll()
        db.appRecommendationDao().clearAll()
        db.userProfileDao().clearAll()
    }
}
