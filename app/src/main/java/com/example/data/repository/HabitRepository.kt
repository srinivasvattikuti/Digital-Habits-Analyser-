package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiHabitsPipeline
import com.example.data.ai.IncrementalAiHabitEngine
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
import com.example.data.model.HabitAnalyticsSnapshotEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.IncrementalAiAnalysisMemoryEntity
import com.example.data.model.LongitudinalCategoryComparison
import com.example.data.model.ResearchHabitMetrics
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import com.example.data.science.HabitResearchEngine
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
    val researchEngine = HabitResearchEngine()
    val incrementalAiEngine = IncrementalAiHabitEngine()
    val sduiPipeline = com.example.data.sdui.ServerDrivenUiPipeline()

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
    val allProfiles: Flow<List<UserProfileEntity>> = db.userProfileDao().getAllProfiles()
    val incrementalMemory: Flow<IncrementalAiAnalysisMemoryEntity?> = db.incrementalAiMemoryDao().getMemory("current_user")
    val analyticsSnapshots: Flow<List<HabitAnalyticsSnapshotEntity>> = db.analyticsSnapshotDao().getAllSnapshots("current_user")

    fun getAggregatesSince(startDate: String): Flow<List<DailyAggregateEntity>> =
        db.dailyAggregateDao().getAggregatesSince(startDate)

    fun getEventsForDate(dateStr: String): Flow<List<UsageEventEntity>> =
        db.usageEventDao().getEventsForDate(dateStr)

    suspend fun initializeDataIfEmpty() = withContext(Dispatchers.IO) {
        // Initialize default user profile if none exists
        if (db.userProfileDao().getUserProfileSync() == null) {
            db.userProfileDao().insertUserProfile(UserProfileEntity(id = 1, isActiveProfile = true))
        }

        // Collect all real installed apps on the device
        try {
            usageCollector.collectInstalledApps()
        } catch (e: Exception) {
            // Ignore
        }

        // Collect real usage events and aggregates if permission is granted
        if (usageCollector.hasUsageStatsPermission()) {
            usageCollector.collectUsageEventsAndAggregates(daysBack = 7)
        }

        // If no aggregates exist (e.g., fresh install, tests, or no permission), populate rich baseline demo data
        val existing = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        if (existing.isEmpty()) {
            sampleGenerator.populateRichDemoData(forceOverwrite = false)
        }

        try {
            runAiHabitPipeline()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        val currentUserId = authManager.currentUserId ?: "current_user"
        val updated = profile.copy(
            userId = currentUserId,
            isActiveProfile = true,
            updatedAt = System.currentTimeMillis()
        )
        db.userProfileDao().insertUserProfile(updated)
        analyticsManager.logUserPersonaSelected(updated.roleKey, updated.scheduleType)
        try {
            runAiHabitPipeline()
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    suspend fun applyRolePreset(role: UserRole, targetProfileId: Int = 1) = withContext(Dispatchers.IO) {
        val existing = db.userProfileDao().getUserProfileSync() ?: UserProfileEntity()
        val currentUserId = authManager.currentUserId ?: "current_user"
        val updatedProfile = existing.copy(
            id = targetProfileId,
            userId = currentUserId,
            roleKey = role.name,
            occupationTitle = role.displayName.substringBefore(" ("),
            scheduleType = when (role) {
                UserRole.KID_STUDENT, UserRole.TEEN_STUDENT -> "SCHOOL_HOURS"
                UserRole.COLLEGE_STUDENT -> "COLLEGE_SCHEDULE"
                UserRole.REMOTE_FREELANCER -> "FLEXIBLE_REMOTE"
                UserRole.HOMEMAKER_PARENT -> "HOMEMAKER_ROUTINE"
                UserRole.RETIRED -> "LEISURE_WELLNESS"
                else -> "STANDARD_WORK"
            },
            focusStartHour = role.defaultFocusStart,
            focusEndHour = role.defaultFocusEnd,
            bedtimeHour = role.defaultBedtime,
            dailyScreenTimeTargetMinutes = role.defaultScreenLimitMinutes,
            isKidMode = role.defaultIsKid,
            isActiveProfile = true,
            updatedAt = System.currentTimeMillis()
        )
        db.userProfileDao().insertUserProfile(updatedProfile)
        analyticsManager.logUserPersonaSelected(role.name, updatedProfile.scheduleType)

        // Seed role-appropriate custom goals dynamically into Room database
        val roleGoals = when (role) {
            UserRole.KID_STUDENT -> listOf(
                HabitGoalEntity("GOAL_KID_STUDY", "Study & Homework Time", "Protected schoolwork focus without mobile distractions", "MIN_TARGET", "PRODUCTIVITY", 45, "min", true, "school", "#10B981", currentUserId),
                HabitGoalEntity("GOAL_KID_SCREEN", "Daily Screen Limit", "Maximum recreational device time allowed per day", "MAX_LIMIT", "SOCIAL", 120, "min", true, "timer", "#EC4899", currentUserId),
                HabitGoalEntity("GOAL_KID_BEDTIME", "Kids Bedtime Cutoff", "Device put away by 9:00 PM for healthy sleep", "MAX_LIMIT", "BEDTIME", 21, "PM", true, "bedtime", "#6366F1", currentUserId),
                HabitGoalEntity("GOAL_KID_STEPS", "Active Play Steps", "Daily outdoor physical activity and movement", "MIN_TARGET", "STEPS", 8000, "steps", true, "steps", "#F59E0B", currentUserId)
            )
            UserRole.TEEN_STUDENT -> listOf(
                HabitGoalEntity("GOAL_TEEN_STUDY", "Deep Study & Revision", "Daily focus blocks for coursework and exam prep", "MIN_TARGET", "PRODUCTIVITY", 60, "min", true, "school", "#10B981", currentUserId),
                HabitGoalEntity("GOAL_TEEN_SOCIAL", "Social Feeds Cap", "Daily combined TikTok, Instagram & Snapchat screen limit", "MAX_LIMIT", "SOCIAL", 90, "min", true, "social", "#EC4899", currentUserId),
                HabitGoalEntity("GOAL_TEEN_BEDTIME", "Night Downtime", "Screen off by 10:00 PM to protect melatonin cycle", "MAX_LIMIT", "BEDTIME", 22, "PM", true, "bedtime", "#6366F1", currentUserId),
                HabitGoalEntity("GOAL_TEEN_OPENS", "Impulse Phone Checks", "Limit unconscious pocket checks during classes", "MAX_LIMIT", "COMPULSIVE_OPENS", 25, "opens", true, "touch", "#EF4444", currentUserId)
            )
            UserRole.REMOTE_FREELANCER -> listOf(
                HabitGoalEntity("GOAL_FREE_DEEPWORK", "Deep Work Sprint", "Uninterrupted client deliverables and coding blocks", "MIN_TARGET", "PRODUCTIVITY", 180, "min", true, "work", "#10B981", currentUserId),
                HabitGoalEntity("GOAL_FREE_NOTIFS", "Distraction Guard", "Limit social & chat checks during work sprints", "MAX_LIMIT", "SOCIAL", 45, "min", true, "social", "#EC4899", currentUserId),
                HabitGoalEntity("GOAL_FREE_STEPS", "Ergonomic Walk Breaks", "Active steps to break prolonged sitting", "MIN_TARGET", "STEPS", 7500, "steps", true, "steps", "#F59E0B", currentUserId),
                HabitGoalEntity("GOAL_FREE_BEDTIME", "Work-Life Boundary", "Close laptop and phones before 11:00 PM", "MAX_LIMIT", "BEDTIME", 23, "PM", true, "bedtime", "#6366F1", currentUserId)
            )
            else -> listOf(
                HabitGoalEntity("GOAL_SOCIAL_MAX", "Social Media Cap", "Maximum daily combined social scrolling (Instagram & Reddit)", "MAX_LIMIT", "SOCIAL", 60, "min", true, "social", "#EC4899", currentUserId),
                HabitGoalEntity("GOAL_PROD_MIN", "Deep Focus & Study", "Daily target for intentional learning and focus (Notion & Duolingo)", "MIN_TARGET", "PRODUCTIVITY", 60, "min", true, "work", "#10B981", currentUserId),
                HabitGoalEntity("GOAL_BEDTIME_CUTOFF", "Bedtime Downtime", "Cease recreational screen engagement before 10:30 PM", "MAX_LIMIT", "BEDTIME", 22, "PM", true, "bedtime", "#6366F1", currentUserId),
                HabitGoalEntity("GOAL_COMPULSIVE_LIMIT", "Reflex Opens Guard", "Cap unconscious micro-checks (<30s unlocks) per day", "MAX_LIMIT", "COMPULSIVE_OPENS", 20, "opens", true, "touch", "#EF4444", currentUserId),
                HabitGoalEntity("GOAL_STEPS_MIN", "Physical Movement", "Daily active step count target for mental wellness balance", "MIN_TARGET", "STEPS", 8000, "steps", true, "steps", "#F59E0B", currentUserId)
            )
        }
        db.habitGoalDao().insertGoals(roleGoals)

        // Select matching SDUI Preset layout
        val presetId = when (role) {
            UserRole.KID_STUDENT -> "STUDENT_MODE"
            UserRole.TEEN_STUDENT -> "STUDENT_MODE"
            UserRole.REMOTE_FREELANCER -> "DIGITAL_DETOX"
            UserRole.HOMEMAKER_PARENT -> "DEFAULT_BALANCED"
            UserRole.RETIRED -> "DEFAULT_BALANCED"
            else -> "DEFAULT_BALANCED"
        }
        applyPresetLayout(presetId, currentUserId)

        try {
            runAiHabitPipeline()
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    suspend fun switchActiveProfile(profileId: Int) = withContext(Dispatchers.IO) {
        db.userProfileDao().setActiveProfile(profileId)
        try {
            runAiHabitPipeline()
        } catch (e: Exception) {
            // Non-blocking
        }
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

        // 1. Compute Data Science Research Indicators
        val researchMetrics = researchEngine.computeResearchMetrics(aggregates, events, profile, halfLifeDays = 3.5f)
        val categoryComparisons = researchEngine.computeLongitudinalCategoryComparisons(aggregates, halfLifeDays = 3.5f)

        // 2. Fetch existing longitudinal baseline & incremental memory from Room
        val existingMemory = db.incrementalAiMemoryDao().getMemorySync("current_user")
        val baselineSnapshot = db.analyticsSnapshotDao().getBaselineAnchorSnapshot("current_user")

        // 3. Execute Token-Efficient Incremental Synthesis
        val output = incrementalAiEngine.runIncrementalAnalysis(
            aggregates = aggregates,
            events = events,
            userProfile = profile,
            existingMemory = existingMemory,
            baselineSnapshot = baselineSnapshot,
            researchMetrics = researchMetrics,
            categoryComparisons = categoryComparisons
        )

        // 4. Persist insight, snapshot, recommendations, and memory to SQLite
        db.habitInsightDao().insertInsight(output.insight)
        db.incrementalAiMemoryDao().insertMemory(output.updatedMemory)
        db.analyticsSnapshotDao().insertSnapshot(output.newSnapshot)

        if (output.recommendations.isNotEmpty()) {
            db.appRecommendationDao().insertRecommendations(output.recommendations)
        }

        output.insight
    }

    suspend fun getResearchHabitMetrics(halfLifeDays: Float = 3.5f): ResearchHabitMetrics = withContext(Dispatchers.IO) {
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val events = db.usageEventDao().getEventsSince(System.currentTimeMillis() - 7 * 86400000L)
        val profile = getUserProfileSync()
        researchEngine.computeResearchMetrics(aggregates, events, profile, halfLifeDays)
    }

    suspend fun getLongitudinalCategoryComparisons(halfLifeDays: Float = 3.5f): List<LongitudinalCategoryComparison> = withContext(Dispatchers.IO) {
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        researchEngine.computeLongitudinalCategoryComparisons(aggregates, halfLifeDays)
    }

    suspend fun getIncrementalMemorySync(): IncrementalAiAnalysisMemoryEntity? = withContext(Dispatchers.IO) {
        db.incrementalAiMemoryDao().getMemorySync("current_user")
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
        val currentUserId = authManager.currentUserId ?: "current_user"
        val goalWithUser = goal.copy(userId = currentUserId)
        db.habitGoalDao().insertGoal(goalWithUser)
        analyticsManager.logGoalUpdated(goalWithUser.id, goalWithUser.goalType, goalWithUser.targetValue)
    }

    suspend fun deleteGoal(goalId: String) = withContext(Dispatchers.IO) {
        db.habitGoalDao().deleteGoal(goalId)
        analyticsManager.logGoalUpdated(goalId, "DELETED", 0)
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
        db.dashboardSduiDao().clearAllLayouts()
    }

    // ==========================================
    // SERVER-DRIVEN UI CUSTOMIZATION PIPELINE
    // ==========================================
    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val layoutAdapter = moshi.adapter(com.example.data.sdui.DashboardLayoutConfig::class.java)

    fun getSavedLayoutForUserFlow(userId: String): Flow<com.example.data.sdui.DashboardLayoutEntity?> =
        db.dashboardSduiDao().getLayoutForUser(userId)

    suspend fun getSavedLayoutForUser(userId: String): com.example.data.sdui.DashboardLayoutConfig = withContext(Dispatchers.IO) {
        val entity = db.dashboardSduiDao().getLayoutForUserSync(userId)
        if (entity != null) {
            try {
                layoutAdapter.fromJson(entity.layoutJson) ?: sduiPipeline.getDefaultLayout(userId)
            } catch (e: Exception) {
                sduiPipeline.getDefaultLayout(userId)
            }
        } else {
            sduiPipeline.getDefaultLayout(userId)
        }
    }

    suspend fun saveDashboardLayout(userId: String, layout: com.example.data.sdui.DashboardLayoutConfig) = withContext(Dispatchers.IO) {
        val json = layoutAdapter.toJson(layout)
        val entity = com.example.data.sdui.DashboardLayoutEntity(
            userId = userId,
            layoutId = layout.layoutId,
            layoutName = layout.layoutName,
            description = layout.description,
            layoutJson = json,
            generatedFromPrompt = layout.generatedFromPrompt,
            updatedAt = System.currentTimeMillis()
        )
        db.dashboardSduiDao().insertOrUpdateLayout(entity)
    }

    suspend fun customizeDashboardWithPrompt(
        prompt: String,
        userId: String = "current_user",
        currentLayout: com.example.data.sdui.DashboardLayoutConfig? = null
    ): com.example.data.sdui.DashboardCustomizeResponse = withContext(Dispatchers.IO) {
        val profile = getUserProfileSync()
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val events = db.usageEventDao().getEventsSince(System.currentTimeMillis() - 7 * 86400000L)
        val goals = db.habitGoalDao().getAllGoalsSync()
        val latestInsight = db.habitInsightDao().getAllInsightsSync().firstOrNull()

        val telemetryContext = com.example.data.sdui.UsageContextProvider.buildSystemTelemetryContext(
            profile = profile,
            aggregates = aggregates,
            events = events,
            goals = goals,
            insight = latestInsight
        )

        val request = com.example.data.sdui.DashboardCustomizeRequest(
            userPrompt = prompt,
            userId = userId,
            currentLayout = currentLayout ?: getSavedLayoutForUser(userId),
            telemetryContext = telemetryContext,
            availableComponents = com.example.data.sdui.UsageContextProvider.getAllowableComponentsSchema()
        )

        val response = sduiPipeline.customizeDashboard(request)

        if (response.success && response.layout != null) {
            saveDashboardLayout(userId, response.layout)
        }

        response
    }

    suspend fun resetDashboardLayoutToDefault(userId: String = "current_user"): com.example.data.sdui.DashboardLayoutConfig = withContext(Dispatchers.IO) {
        val defaultLayout = sduiPipeline.getDefaultLayout(userId)
        saveDashboardLayout(userId, defaultLayout)
        defaultLayout
    }

    suspend fun applyPresetLayout(presetId: String, userId: String = "current_user"): com.example.data.sdui.DashboardLayoutConfig = withContext(Dispatchers.IO) {
        val preset = sduiPipeline.getPresetLayout(presetId, userId)
        saveDashboardLayout(userId, preset)
        preset
    }

    suspend fun getStructuredUsageLogsContext(): String = withContext(Dispatchers.IO) {
        val profile = getUserProfileSync()
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val events = db.usageEventDao().getEventsSince(System.currentTimeMillis() - 7 * 86400000L)
        com.example.data.sdui.UsageContextProvider.generateUsageLogsSummary(profile, aggregates, events)
    }
}
