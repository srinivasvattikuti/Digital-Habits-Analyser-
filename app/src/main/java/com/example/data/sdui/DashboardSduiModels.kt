package com.example.data.sdui

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Supported native card types that can be dynamically rendered in the Android client.
 */
enum class CardType(val displayName: String, val defaultPosition: Int, val description: String) {
    @Json(name = "HERO_USAGE")
    HERO_USAGE("Daily Usage Hero", 1, "Prominent screen time banner with category breakdown"),

    @Json(name = "SUMMARY_METRICS")
    SUMMARY_METRICS("Summary Metric Cards", 2, "Grid of key metrics: Screen Time, Opens, Compulsive Ratio, Notifications"),

    @Json(name = "PROACTIVE_NUDGES")
    PROACTIVE_NUDGES("Proactive Habit Nudges", 3, "Real-time actionable nudges, warnings, and streak highlights"),

    @Json(name = "HOURLY_HEATMAP")
    HOURLY_HEATMAP("24-Hour Usage Heatmap", 4, "Hourly activity density heatmap and time-of-day analyzer"),

    @Json(name = "GOALS_TRACKER")
    GOALS_TRACKER("Habit Goals & Limits", 5, "Interactive progress on daily screen time and category limits"),

    @Json(name = "WEEKLY_TRENDS")
    WEEKLY_TRENDS("Weekly Trend Chart", 6, "Vico bar chart of 7-day screen time versus daily budget"),

    @Json(name = "NOTIFICATION_LEADERBOARD")
    NOTIFICATION_LEADERBOARD("Interruption Leaderboard", 7, "Top notifying apps and open conversion frequency"),

    @Json(name = "WEEK_OVER_WEEK")
    WEEK_OVER_WEEK("Week-over-Week Comparison", 8, "Averages and percentage shifts across categories"),

    @Json(name = "BEHAVIOR_FORECAST")
    BEHAVIOR_FORECAST("Behavioral Forecast", 9, "Pacing projector and late-night doomscroll risk predictor"),

    @Json(name = "RADAR_DIMENSIONS")
    RADAR_DIMENSIONS("5-Pillar Equilibrium Radar", 10, "Holistic radar chart across Intentionality, Sleep, Focus, Physical, Balance"),

    @Json(name = "CATEGORY_DISTRIBUTION")
    CATEGORY_DISTRIBUTION("Category Breakdown", 11, "Distribution of time across Social, Productivity, Entertainment, etc."),

    @Json(name = "COMPULSIVE_GAUGE")
    COMPULSIVE_GAUGE("Compulsive vs Intentional Gauge", 12, "Circular visual gauge tracking impulse checks (<30s)"),

    @Json(name = "TOP_APPS")
    TOP_APPS("Top Dominating Apps", 13, "Ranked list of most-used installed applications"),

    @Json(name = "USAGE_OVER_TIME")
    USAGE_OVER_TIME("Historical Usage Trends", 14, "Multi-day chronological usage line/bar graph"),

    @Json(name = "RECOMMENDATION_BANNER")
    RECOMMENDATION_BANNER("AI Recommendation Banner", 15, "Actionable suggestions for healthier app alternatives"),

    @Json(name = "AI_INSIGHT_BANNER")
    AI_INSIGHT_BANNER("AI Behavioral Insights", 16, "Key takeaways and analytical summary from Gemini behavioral engine"),

    @Json(name = "RESEARCH_HABIT_SCIENCE")
    RESEARCH_HABIT_SCIENCE("Habit Science & Entropy", 17, "Gardner Automaticity Index, Shannon Entropy, and Recency Half-Life"),

    @Json(name = "LONGITUDINAL_BASELINE_COMPARISON")
    LONGITUDINAL_BASELINE_COMPARISON("Longitudinal Baseline vs Recent", 18, "30-day baseline vs decay-weighted comparison with Z-scores"),

    @Json(name = "CIRCADIAN_SLEEP_IMPACT")
    CIRCADIAN_SLEEP_IMPACT("Circadian & Pre-Bedtime Sleep Risk", 19, "Pre-bedtime 90-minute screen exposure and melatonin suppression index"),

    @Json(name = "AI_INCREMENTAL_MEMORY")
    AI_INCREMENTAL_MEMORY("Incremental AI Memory", 20, "Token-efficient incremental synthesis and SQLite baseline anchors");

    companion object {
        fun fromString(value: String): CardType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SUMMARY_METRICS
        }
    }
}

/**
 * Fine-grained parameter filters and visual toggles for a card component.
 */
@JsonClass(generateAdapter = true)
data class ComponentParameters(
    @Json(name = "showScreenTime") val showScreenTime: Boolean = true,
    @Json(name = "showOpens") val showOpens: Boolean = true,
    @Json(name = "showCompulsiveRatio") val showCompulsiveRatio: Boolean = true,
    @Json(name = "showNotifications") val showNotifications: Boolean = true,
    @Json(name = "showSteps") val showSteps: Boolean = true,
    @Json(name = "maxAppsCount") val maxAppsCount: Int = 8,
    @Json(name = "layoutStyle") val layoutStyle: String = "GRID_2X2", // "GRID_2X2", "HORIZONTAL_ROW", "COMPACT_LIST"
    @Json(name = "highlightMetric") val highlightMetric: String? = null, // "SCREEN_TIME", "COMPULSIVE_RATIO", "NOTIFICATIONS"
    @Json(name = "categoryFilter") val categoryFilter: String? = null, // e.g. "SOCIAL", "PRODUCTIVITY"
    @Json(name = "timeRange") val timeRange: String = "WEEK" // "TODAY", "WEEK", "MONTH"
)

/**
 * Individual dynamic UI card configuration in the Server-Driven UI pipeline.
 */
@JsonClass(generateAdapter = true)
data class DynamicComponentConfig(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: CardType,
    @Json(name = "position") val position: Int,
    @Json(name = "visible") val visible: Boolean = true,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "accentColorHex") val accentColorHex: String? = null,
    @Json(name = "cardStyle") val cardStyle: String = "CARD", // "CARD", "MINIMAL", "HIGHLIGHT", "COMPACT", "OUTLINED"
    @Json(name = "parameters") val parameters: ComponentParameters = ComponentParameters()
)

/**
 * Complete Server-Driven Dashboard Layout Schema.
 */
@JsonClass(generateAdapter = true)
data class DashboardLayoutConfig(
    @Json(name = "layoutId") val layoutId: String = "default_layout",
    @Json(name = "userId") val userId: String = "current_user",
    @Json(name = "layoutName") val layoutName: String = "Default Overview",
    @Json(name = "description") val description: String = "Standard holistic digital wellness dashboard",
    @Json(name = "themeColor") val themeColor: String = "DEFAULT", // "DEFAULT", "INDIGO", "EMERALD", "MIDNIGHT", "AMBER", "ROSE", "CRIMSON"
    @Json(name = "density") val density: String = "COMFORTABLE", // "COMPACT", "COMFORTABLE", "SPACIOUS"
    @Json(name = "headerTitle") val headerTitle: String = "Dashboard",
    @Json(name = "headerSubtitle") val headerSubtitle: String = "HABIT INSIGHTS",
    @Json(name = "components") val components: List<DynamicComponentConfig> = emptyList(),
    @Json(name = "generatedFromPrompt") val generatedFromPrompt: String? = null,
    @Json(name = "version") val version: Int = 1,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Telemetry Context sent with the prompt to the backend or Gemini model.
 */
@JsonClass(generateAdapter = true)
data class SystemTelemetryContext(
    @Json(name = "userName") val userName: String = "User",
    @Json(name = "userRole") val userRole: String = "Working Professional",
    @Json(name = "focusSchedule") val focusSchedule: String = "9:00 AM - 5:00 PM",
    @Json(name = "bedtimeHour") val bedtimeHour: Int = 23,
    @Json(name = "totalScreenTimeMinutes") val totalScreenTimeMinutes: Int = 0,
    @Json(name = "totalOpens") val totalOpens: Int = 0,
    @Json(name = "totalNotifications") val totalNotifications: Int = 0,
    @Json(name = "compulsiveScore") val compulsiveScore: Int = 50,
    @Json(name = "topApps") val topApps: List<String> = emptyList(),
    @Json(name = "topCategories") val topCategories: List<String> = emptyList(),
    @Json(name = "primaryGoal") val primaryGoal: String = "Reduce Screen Time",
    @Json(name = "hasLateNightUsage") val hasLateNightUsage: Boolean = false,
    @Json(name = "hasHighNotificationDisruption") val hasHighNotificationDisruption: Boolean = false
)

/**
 * Information on an allowable native component for LLM context schema.
 */
@JsonClass(generateAdapter = true)
data class ComponentSchemaInfo(
    @Json(name = "cardType") val cardType: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "description") val description: String,
    @Json(name = "supportedParameters") val supportedParameters: List<String>
)

/**
 * Request payload sent to /api/v1/dashboard/customize
 */
@JsonClass(generateAdapter = true)
data class DashboardCustomizeRequest(
    @Json(name = "userPrompt") val userPrompt: String,
    @Json(name = "userId") val userId: String = "current_user",
    @Json(name = "currentLayout") val currentLayout: DashboardLayoutConfig? = null,
    @Json(name = "telemetryContext") val telemetryContext: SystemTelemetryContext = SystemTelemetryContext(),
    @Json(name = "availableComponents") val availableComponents: List<ComponentSchemaInfo> = emptyList()
)

/**
 * Response payload returned from /api/v1/dashboard/customize
 */
@JsonClass(generateAdapter = true)
data class DashboardCustomizeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "layout") val layout: DashboardLayoutConfig? = null,
    @Json(name = "explanation") val explanation: String = "",
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "requiresNativeUpdate") val requiresNativeUpdate: Boolean = false
)

/**
 * Room Database entity storing user-specific layout configurations indexed by userId.
 */
@Entity(tableName = "dashboard_layouts")
data class DashboardLayoutEntity(
    @PrimaryKey val userId: String,
    val layoutId: String,
    val layoutName: String,
    val description: String,
    val layoutJson: String,
    val generatedFromPrompt: String?,
    val updatedAt: Long = System.currentTimeMillis()
)
