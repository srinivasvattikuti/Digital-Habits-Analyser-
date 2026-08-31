package com.example.data.sdui

import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Context provider that compiles live system metrics, user usage logs, and
 * allowable component schema into structured payloads for Gemini / backend.
 */
object UsageContextProvider {

    fun buildSystemTelemetryContext(
        profile: UserProfileEntity,
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        goals: List<HabitGoalEntity>,
        insight: HabitInsightEntity?
    ): SystemTelemetryContext {
        val totalMs = aggregates.sumOf { it.totalDurationMs }
        val totalMinutes = (totalMs / 60000).toInt()
        val totalOpens = aggregates.sumOf { it.openCount }
        val totalNotifications = aggregates.sumOf { it.notificationCount }
        val totalCompulsiveOpens = aggregates.sumOf { it.compulsiveOpens }
        val compulsiveScore = if (totalOpens > 0) ((totalCompulsiveOpens.toFloat() / totalOpens) * 100).toInt() else 50

        val topApps = aggregates.groupBy { it.appName }
            .mapValues { it.value.sumOf { agg -> agg.totalDurationMs } }
            .toList()
            .sortedByDescending { it.second }
            .take(6)
            .map { it.first }

        val topCategories = aggregates.groupBy { it.category }
            .mapValues { it.value.sumOf { agg -> agg.totalDurationMs } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }

        val nightMinutes = aggregates.sumOf { it.nightMinutes }
        val hasLateNight = nightMinutes > 30

        val hasHighNotifs = totalNotifications > 50

        return SystemTelemetryContext(
            userName = profile.name,
            userRole = profile.getRole().displayName,
            focusSchedule = "${profile.focusStartHour}:00 - ${profile.focusEndHour}:00",
            bedtimeHour = profile.bedtimeHour,
            totalScreenTimeMinutes = totalMinutes,
            totalOpens = totalOpens,
            totalNotifications = totalNotifications,
            compulsiveScore = compulsiveScore,
            topApps = topApps,
            topCategories = topCategories,
            primaryGoal = profile.getGoalsList().firstOrNull() ?: "Reduce Screen Time",
            hasLateNightUsage = hasLateNight,
            hasHighNotificationDisruption = hasHighNotifs
        )
    }

    fun getAllowableComponentsSchema(): List<ComponentSchemaInfo> {
        return CardType.entries.map { card ->
            val params = when (card) {
                CardType.SUMMARY_METRICS -> listOf("showScreenTime", "showOpens", "showCompulsiveRatio", "showNotifications", "showSteps", "layoutStyle")
                CardType.TOP_APPS -> listOf("maxAppsCount", "categoryFilter")
                CardType.CATEGORY_DISTRIBUTION -> listOf("timeRange", "highlightMetric")
                CardType.WEEKLY_TRENDS -> listOf("highlightMetric")
                CardType.GOALS_TRACKER -> listOf("categoryFilter")
                CardType.RESEARCH_HABIT_SCIENCE -> listOf("timeRange", "layoutStyle")
                CardType.LONGITUDINAL_BASELINE_COMPARISON -> listOf("timeRange", "highlightMetric")
                CardType.CIRCADIAN_SLEEP_IMPACT -> listOf("highlightMetric")
                CardType.AI_INCREMENTAL_MEMORY -> listOf("layoutStyle")
                else -> emptyList()
            }
            ComponentSchemaInfo(
                cardType = card.name,
                displayName = card.displayName,
                description = card.description,
                supportedParameters = params
            )
        }
    }

    fun generateUsageLogsSummary(
        profile: UserProfileEntity,
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>
    ): String {
        val totalMinutes = aggregates.sumOf { it.totalDurationMs } / 60000
        val totalOpens = aggregates.sumOf { it.openCount }
        val compulsiveOpens = aggregates.sumOf { it.compulsiveOpens }

        val appDurations = aggregates.groupBy { it.appName }
            .mapValues { it.value.sumOf { agg -> agg.totalDurationMs } / 60000 }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        return buildString {
            appendLine("=== STRUCTURED TELEMETRY SNAPSHOT ===")
            appendLine("User: ${profile.name} (${profile.getRole().displayName})")
            appendLine("Daily Target Budget: ${profile.dailyScreenTimeTargetMinutes} mins | Focus Hours: ${profile.focusStartHour}:00 - ${profile.focusEndHour}:00")
            appendLine("Total Screen Time: ${totalMinutes}m ($totalOpens launches, $compulsiveOpens impulsive micro-checks)")
            appendLine("Top Used Apps: ${appDurations.joinToString { "${it.first}: ${it.second}m" }}")
            appendLine("Late Night (22:00-05:00): ${aggregates.sumOf { it.nightMinutes }}m | Morning: ${aggregates.sumOf { it.morningMinutes }}m")
        }
    }
}
