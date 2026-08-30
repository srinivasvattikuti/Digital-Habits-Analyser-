package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

data class SyncSummary(
    val aggregatesCount: Int = 0,
    val goalsCount: Int = 0,
    val insightsCount: Int = 0,
    val chatsCount: Int = 0,
    val syncedAt: Long = System.currentTimeMillis()
)

data class CloudBackupData(
    val profile: UserProfileEntity?,
    val goals: List<HabitGoalEntity>,
    val aggregates: List<DailyAggregateEntity>,
    val insights: List<HabitInsightEntity>,
    val chats: List<ChatMessageEntity>
)

/**
 * Production-Grade Cloud Firestore Synchronizer.
 * Provides offline-first caching, batch writes, and structured cloud backup & restore.
 */
class FirestoreSyncManager(
    private val context: Context,
    private val analyticsManager: HabitAnalyticsManager? = null
) {
    companion object {
        private const val TAG = "FirestoreSyncManager"
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024 * 1024) // 100MB cache
                        .build()
                )
                .build()
            db.firestoreSettings = settings
            db
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization notice (Check google-services.json): ${e.message}")
            null
        }
    }

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.IDLE)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val lastSyncSummary: StateFlow<SyncSummary?> = _lastSyncSummary.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    fun clearStatus() {
        _syncStatus.value = CloudSyncStatus.IDLE
        _syncErrorMessage.value = null
    }

    /**
     * Backs up local Room database entities to Cloud Firestore under users/{userId}.
     */
    suspend fun backupLocalDataToCloud(
        userId: String,
        profile: UserProfileEntity?,
        goals: List<HabitGoalEntity>,
        aggregates: List<DailyAggregateEntity>,
        insights: List<HabitInsightEntity>,
        chats: List<ChatMessageEntity>
    ): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized. Please check Firebase configuration.")
        )
        if (userId.isBlank()) {
            _syncStatus.value = CloudSyncStatus.ERROR
            _syncErrorMessage.value = "User is not signed in to Firebase."
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }

        _syncStatus.value = CloudSyncStatus.SYNCING
        _syncErrorMessage.value = null

        try {
            val userDoc = db.collection("users").document(userId)

            // 1. Sync User Profile
            if (profile != null) {
                val profileMap = hashMapOf(
                    "id" to profile.id,
                    "name" to profile.name,
                    "age" to profile.age,
                    "gender" to profile.gender,
                    "roleKey" to profile.roleKey,
                    "occupationTitle" to profile.occupationTitle,
                    "scheduleType" to profile.scheduleType,
                    "focusStartHour" to profile.focusStartHour,
                    "focusEndHour" to profile.focusEndHour,
                    "daysOffCsv" to profile.daysOffCsv,
                    "bedtimeHour" to profile.bedtimeHour,
                    "wakeHour" to profile.wakeHour,
                    "primaryGoalsCsv" to profile.primaryGoalsCsv,
                    "dailyScreenTimeTargetMinutes" to profile.dailyScreenTimeTargetMinutes,
                    "isKidMode" to profile.isKidMode,
                    "isProfileCompleted" to profile.isProfileCompleted,
                    "updatedAt" to profile.updatedAt,
                    "lastCloudBackupTime" to System.currentTimeMillis()
                )
                userDoc.collection("profile").document("main")
                    .set(profileMap, SetOptions.merge())
                    .await()
            }

            // 2. Batch Sync Habit Goals
            val goalBatch = db.batch()
            for (goal in goals) {
                val goalDoc = userDoc.collection("goals").document(goal.id)
                val goalMap = hashMapOf(
                    "id" to goal.id,
                    "title" to goal.title,
                    "description" to goal.description,
                    "goalType" to goal.goalType,
                    "category" to goal.category,
                    "targetValue" to goal.targetValue,
                    "unit" to goal.unit,
                    "isEnabled" to goal.isEnabled,
                    "iconKey" to goal.iconKey,
                    "colorHex" to goal.colorHex
                )
                goalBatch.set(goalDoc, goalMap, SetOptions.merge())
            }
            goalBatch.commit().await()

            // 3. Batch Sync Daily Aggregates (Chunked into 250 writes per batch)
            aggregates.chunked(250).forEach { chunk ->
                val aggBatch = db.batch()
                for (agg in chunk) {
                    val safeKey = "${agg.dateStr}_${agg.packageName.replace(".", "_")}"
                    val aggDoc = userDoc.collection("daily_aggregates").document(safeKey)
                    val aggMap = hashMapOf(
                        "id" to agg.id,
                        "dateStr" to agg.dateStr,
                        "packageName" to agg.packageName,
                        "appName" to agg.appName,
                        "category" to agg.category,
                        "totalDurationMs" to agg.totalDurationMs,
                        "openCount" to agg.openCount,
                        "notificationCount" to agg.notificationCount,
                        "morningMinutes" to agg.morningMinutes,
                        "afternoonMinutes" to agg.afternoonMinutes,
                        "eveningMinutes" to agg.eveningMinutes,
                        "nightMinutes" to agg.nightMinutes,
                        "compulsiveOpens" to agg.compulsiveOpens,
                        "stepsCount" to agg.stepsCount
                    )
                    aggBatch.set(aggDoc, aggMap, SetOptions.merge())
                }
                aggBatch.commit().await()
            }

            // 4. Sync Latest AI Insights
            val insightBatch = db.batch()
            for (insight in insights.take(20)) {
                val insightDoc = userDoc.collection("insights").document("${insight.id}")
                val insightMap = hashMapOf(
                    "id" to insight.id,
                    "timestamp" to insight.timestamp,
                    "periodLabel" to insight.periodLabel,
                    "dominantAppsJson" to insight.dominantAppsJson,
                    "peakActiveHours" to insight.peakActiveHours,
                    "compulsiveScore" to insight.compulsiveScore,
                    "compulsiveSummary" to insight.compulsiveSummary,
                    "productivityTrend" to insight.productivityTrend,
                    "keyTakeaway" to insight.keyTakeaway,
                    "fullAnalysisText" to insight.fullAnalysisText,
                    "isSyncedWithBackend" to true
                )
                insightBatch.set(insightDoc, insightMap, SetOptions.merge())
            }
            insightBatch.commit().await()

            // 5. Sync Chat History
            val chatBatch = db.batch()
            for (chat in chats.take(50)) {
                val chatDoc = userDoc.collection("chats").document("${chat.id}")
                val chatMap = hashMapOf(
                    "id" to chat.id,
                    "sender" to chat.sender,
                    "message" to chat.message,
                    "timestamp" to chat.timestamp,
                    "groundedDataSummary" to chat.groundedDataSummary
                )
                chatBatch.set(chatDoc, chatMap, SetOptions.merge())
            }
            chatBatch.commit().await()

            val now = System.currentTimeMillis()
            val summary = SyncSummary(
                aggregatesCount = aggregates.size,
                goalsCount = goals.size,
                insightsCount = insights.size,
                chatsCount = chats.size,
                syncedAt = now
            )

            _syncStatus.value = CloudSyncStatus.SUCCESS
            _lastSyncTimestamp.value = now
            _lastSyncSummary.value = summary
            analyticsManager?.logCloudSync("backup", success = true, itemsCount = aggregates.size + goals.size)

            Result.success(summary)
        } catch (e: Exception) {
            val error = e.localizedMessage ?: "Cloud sync failed"
            Log.e(TAG, "Cloud backup error: $error", e)
            _syncStatus.value = CloudSyncStatus.ERROR
            _syncErrorMessage.value = error
            analyticsManager?.logCloudSync("backup", success = false)
            analyticsManager?.recordNonFatalException(TAG, error, e)
            Result.failure(e)
        }
    }

    /**
     * Restores backed-up telemetry and habit configurations from Cloud Firestore.
     */
    suspend fun restoreDataFromCloud(userId: String): Result<CloudBackupData> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized.")
        )
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID is required"))
        }

        _syncStatus.value = CloudSyncStatus.SYNCING
        _syncErrorMessage.value = null

        try {
            val userDoc = db.collection("users").document(userId)

            // 1. Fetch Profile
            val profileSnap = userDoc.collection("profile").document("main").get().await()
            val profile = if (profileSnap.exists()) {
                UserProfileEntity(
                    id = 1,
                    name = profileSnap.getString("name") ?: "You",
                    age = (profileSnap.getLong("age") ?: 26L).toInt(),
                    gender = profileSnap.getString("gender") ?: "Prefer not to say",
                    roleKey = profileSnap.getString("roleKey") ?: "WORKING_PROFESSIONAL",
                    occupationTitle = profileSnap.getString("occupationTitle") ?: "Working Professional",
                    scheduleType = profileSnap.getString("scheduleType") ?: "STANDARD_WORK",
                    focusStartHour = (profileSnap.getLong("focusStartHour") ?: 9L).toInt(),
                    focusEndHour = (profileSnap.getLong("focusEndHour") ?: 17L).toInt(),
                    daysOffCsv = profileSnap.getString("daysOffCsv") ?: "SATURDAY,SUNDAY",
                    bedtimeHour = (profileSnap.getLong("bedtimeHour") ?: 23L).toInt(),
                    wakeHour = (profileSnap.getLong("wakeHour") ?: 7L).toInt(),
                    primaryGoalsCsv = profileSnap.getString("primaryGoalsCsv") ?: "REDUCE_BEDTIME_SCROLL,PROTECT_FOCUS,LIMIT_SOCIAL_UNLOCKS,PHYSICAL_ACTIVITY",
                    dailyScreenTimeTargetMinutes = (profileSnap.getLong("dailyScreenTimeTargetMinutes") ?: 210L).toInt(),
                    isKidMode = profileSnap.getBoolean("isKidMode") ?: false,
                    isProfileCompleted = profileSnap.getBoolean("isProfileCompleted") ?: true,
                    updatedAt = profileSnap.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            } else null

            // 2. Fetch Goals
            val goalsSnap = userDoc.collection("goals").get().await()
            val goals = goalsSnap.documents.mapNotNull { doc ->
                try {
                    HabitGoalEntity(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "Habit Goal",
                        description = doc.getString("description") ?: "",
                        goalType = doc.getString("goalType") ?: "MAX_LIMIT",
                        category = doc.getString("category") ?: "SOCIAL",
                        targetValue = (doc.getLong("targetValue") ?: 60L).toInt(),
                        unit = doc.getString("unit") ?: "min",
                        isEnabled = doc.getBoolean("isEnabled") ?: true,
                        iconKey = doc.getString("iconKey") ?: "timer",
                        colorHex = doc.getString("colorHex") ?: "#BA1A1A"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Fetch Daily Aggregates
            val aggSnap = userDoc.collection("daily_aggregates").get().await()
            val aggregates = aggSnap.documents.mapNotNull { doc ->
                try {
                    val dStr = doc.getString("dateStr") ?: ""
                    val pName = doc.getString("packageName") ?: ""
                    DailyAggregateEntity(
                        id = doc.getString("id") ?: "$dStr-$pName",
                        dateStr = dStr,
                        packageName = pName,
                        appName = doc.getString("appName") ?: "App",
                        category = doc.getString("category") ?: "OTHER",
                        totalDurationMs = doc.getLong("totalDurationMs") ?: 0L,
                        openCount = (doc.getLong("openCount") ?: 0L).toInt(),
                        notificationCount = (doc.getLong("notificationCount") ?: 0L).toInt(),
                        morningMinutes = (doc.getLong("morningMinutes") ?: 0L).toInt(),
                        afternoonMinutes = (doc.getLong("afternoonMinutes") ?: 0L).toInt(),
                        eveningMinutes = (doc.getLong("eveningMinutes") ?: 0L).toInt(),
                        nightMinutes = (doc.getLong("nightMinutes") ?: 0L).toInt(),
                        compulsiveOpens = (doc.getLong("compulsiveOpens") ?: 0L).toInt(),
                        stepsCount = (doc.getLong("stepsCount") ?: 0L).toInt()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 4. Fetch Insights
            val insightsSnap = userDoc.collection("insights").get().await()
            val insights = insightsSnap.documents.mapNotNull { doc ->
                try {
                    HabitInsightEntity(
                        id = doc.getLong("id") ?: 0L,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        periodLabel = doc.getString("periodLabel") ?: "Past 7 Days",
                        dominantAppsJson = doc.getString("dominantAppsJson") ?: "",
                        peakActiveHours = doc.getString("peakActiveHours") ?: "",
                        compulsiveScore = (doc.getLong("compulsiveScore") ?: 50L).toInt(),
                        compulsiveSummary = doc.getString("compulsiveSummary") ?: "",
                        productivityTrend = doc.getString("productivityTrend") ?: "",
                        keyTakeaway = doc.getString("keyTakeaway") ?: "",
                        fullAnalysisText = doc.getString("fullAnalysisText") ?: "",
                        isSyncedWithBackend = true
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 5. Fetch Chat Messages
            val chatSnap = userDoc.collection("chats").get().await()
            val chats = chatSnap.documents.mapNotNull { doc ->
                try {
                    ChatMessageEntity(
                        id = doc.getLong("id") ?: 0L,
                        sender = doc.getString("sender") ?: "USER",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        groundedDataSummary = doc.getString("groundedDataSummary")
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val resultData = CloudBackupData(
                profile = profile,
                goals = goals,
                aggregates = aggregates,
                insights = insights,
                chats = chats
            )

            val summary = SyncSummary(
                aggregatesCount = aggregates.size,
                goalsCount = goals.size,
                insightsCount = insights.size,
                chatsCount = chats.size,
                syncedAt = System.currentTimeMillis()
            )

            _syncStatus.value = CloudSyncStatus.SUCCESS
            _lastSyncSummary.value = summary
            analyticsManager?.logCloudSync("restore", success = true, itemsCount = aggregates.size + goals.size)

            Result.success(resultData)
        } catch (e: Exception) {
            val error = e.localizedMessage ?: "Cloud restore failed"
            Log.e(TAG, "Cloud restore error: $error", e)
            _syncStatus.value = CloudSyncStatus.ERROR
            _syncErrorMessage.value = error
            analyticsManager?.logCloudSync("restore", success = false)
            analyticsManager?.recordNonFatalException(TAG, error, e)
            Result.failure(e)
        }
    }
}
