package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Data Science & Research-Backed Habit Metrics Model.
 * Based on behavioral science research (Lally 2010, Gardner 2012, Wood & Neal 2009, Shannon Entropy).
 */
data class ResearchHabitMetrics(
    val exponentialDecayHalfLifeDays: Float = 3.5f,
    val recentWeightedScreenTimeMinutes: Int = 0,
    val baselineScreenTimeMinutes: Int = 0,
    val longitudinalScreenTimeDeltaPct: Float = 0f,
    val automaticityIndex: Int = 50, // 0-100 (Gardner Self-Report Behavioral Automaticity scale)
    val habitFormationPhase: String = "Linear Consolidation (Phase 2)", // Phase 1: Early Acquisition (1-18d), Phase 2: Consolidation (19-45d), Phase 3: Asymptotic (46-66d+)
    val habitFormationDayEstimate: Int = 24,
    val attentionFragmentationEntropy: Float = 1.85f, // Shannon Entropy H in bits (0.0 to 3.5+)
    val attentionEntropyRating: String = "Moderate Switching", // "Deep Flow", "Moderate Switching", "High Fragmented Residue"
    val circadianDisruptionIndex: Int = 35, // 0-100 (Risk of blue light / dopamine excitation within 90 min before bedtime)
    val circadianRiskRating: String = "Low-to-Moderate",
    val preBedtimeScreenMinutes: Int = 28, // Minutes logged within 90 min before bedtime
    val bedtimeWindowLabel: String = "9:30 PM - 11:00 PM",
    val flowStateIntegrityScore: Int = 68, // 0-100 (% of focus hours spent in sustained blocks >20min)
    val topDecayWeightedCategory: AppCategory = AppCategory.PRODUCTIVITY,
    val topDecayWeightedApp: String = "Chrome",
    val researchCitations: List<String> = listOf(
        "Lally et al. (2010) 'How are habits formed: Modelling habit formation in the real world' - European Journal of Social Psychology",
        "Gardner et al. (2012) 'Making health habitual: the Self-Report Habit Index' - Health Psychology Review",
        "Wood & Neal (2009) 'The habitual consumer' - Journal of Consumer Psychology"
    )
)

/**
 * Longitudinal Category Comparison comparing 30-day baseline vs recent decay-weighted behavior.
 */
data class LongitudinalCategoryComparison(
    val category: AppCategory,
    val baselineMinutes: Int,
    val recentWeightedMinutes: Int,
    val deltaMinutes: Int,
    val deltaPercent: Float,
    val zScore: Float, // Statistical standard deviation deviation
    val isPositiveShift: Boolean
)

/**
 * Token-efficient Incremental Memory State for the AI Engine.
 */
data class IncrementalAiMemorySummary(
    val baselineDateRange: String = "Past 30 Days",
    val lastAnalysisTimestamp: Long = System.currentTimeMillis(),
    val totalHistoricalDaysTracked: Int = 30,
    val estimatedTokensSavedPercent: Int = 82, // % reduction in prompt tokens due to incremental rollup
    val previousAnchorTakeaway: String = "Focus hours maintained with moderate late-night phone checks.",
    val activeInterventions: List<String> = listOf("Curfew at 11:00 PM", "One Sec friction on Social"),
    val longitudinalTrendDirection: String = "IMPROVING" // "IMPROVING", "STABLE", "DEVIATING"
)

/**
 * Room Entity storing periodic mathematical analytics snapshots for longitudinal baselines.
 */
@Entity(tableName = "analytics_snapshots")
data class HabitAnalyticsSnapshotEntity(
    @PrimaryKey val snapshotId: String, // e.g. "snapshot-2026-08-31" or UUID
    val userId: String = "current_user",
    val timestamp: Long = System.currentTimeMillis(),
    val periodStartStr: String,
    val periodEndStr: String,
    val totalDays: Int,
    val avgDailyMinutes: Int,
    val avgDailyOpens: Int,
    val avgCompulsiveOpens: Int,
    val avgDailyNotifications: Int,
    val categoryMinutesJson: String, // JSON map of category -> avg minutes
    val topAppsJson: String, // JSON list of top app averages
    val ewmaScreenTime: Float, // Exponentially Weighted Moving Average
    val shannonEntropy: Float,
    val automaticityScore: Int,
    val circadianRiskScore: Int,
    val isBaselineAnchor: Boolean = false
)

/**
 * Room Entity storing incremental AI memory state to prevent LLM token burn.
 */
@Entity(tableName = "incremental_ai_memory")
data class IncrementalAiAnalysisMemoryEntity(
    @PrimaryKey val memoryId: String = "active_user_memory",
    val userId: String = "current_user",
    val lastAnalyzedDateStr: String,
    val lastAnalysisTimestamp: Long = System.currentTimeMillis(),
    val historicalBaselineSnapshotId: String? = null,
    val baselineAnchorSummary: String, // Condensed summary of 30-day baseline habits
    val recentDeltaSummary: String, // What changed since the last snapshot
    val previousKeyRecommendations: String,
    val adherenceScore: Int = 75, // 0-100 score of how well user stuck to recommendations
    val totalIncrementalRuns: Int = 1,
    val cumulativeTokensSaved: Long = 18450L
)
