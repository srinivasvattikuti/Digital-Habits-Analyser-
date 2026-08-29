package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiHabitsPipeline
import com.example.data.collector.HealthSensorCollector
import com.example.data.collector.SampleDataGenerator
import com.example.data.collector.UsageStatsCollector
import com.example.data.db.HabitDatabase
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
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

    val installedApps: Flow<List<AppInfoEntity>> = db.appInfoDao().getAllApps()
    val recentEvents: Flow<List<UsageEventEntity>> = db.usageEventDao().getRecentEvents(150)
    val allAggregates: Flow<List<DailyAggregateEntity>> = db.dailyAggregateDao().getAllAggregates()
    val latestInsight: Flow<HabitInsightEntity?> = db.habitInsightDao().getLatestInsight()
    val allInsights: Flow<List<HabitInsightEntity>> = db.habitInsightDao().getAllInsights()
    val chatMessages: Flow<List<ChatMessageEntity>> = db.chatMessageDao().getAllMessages()
    val recommendations: Flow<List<AppRecommendationEntity>> = db.appRecommendationDao().getAllRecommendations()

    fun getAggregatesSince(startDate: String): Flow<List<DailyAggregateEntity>> =
        db.dailyAggregateDao().getAggregatesSince(startDate)

    fun getEventsForDate(dateStr: String): Flow<List<UsageEventEntity>> =
        db.usageEventDao().getEventsForDate(dateStr)

    suspend fun initializeDataIfEmpty() = withContext(Dispatchers.IO) {
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

        val result = aiPipeline.runHabitAnalysisPipeline(aggregates, events)
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

        // Get past messages for context
        val messagesList = mutableListOf<ChatMessageEntity>()
        // Fetch up to 6 recent messages

        val aiAnswer = aiPipeline.answerConversationalQuery(
            query = userMessageText,
            chatHistory = messagesList,
            aggregates = aggregates,
            events = events
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

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.usageEventDao().clearAll()
        db.dailyAggregateDao().clearAll()
        db.habitInsightDao().clearAll()
        db.chatMessageDao().clearAll()
        db.appRecommendationDao().clearAll()
    }
}
