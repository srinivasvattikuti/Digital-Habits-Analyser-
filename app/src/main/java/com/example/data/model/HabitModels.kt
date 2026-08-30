package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppCategory(val displayName: String, val colorHex: String) {
    SOCIAL("Social", "#E11D48"),
    PRODUCTIVITY("Productivity", "#2563EB"),
    ENTERTAINMENT("Entertainment", "#8B5CF6"),
    SHOPPING("Shopping & Retail", "#F59E0B"),
    FINANCE("Finance & Payments", "#10B981"),
    COMMUNICATION("Communication", "#06B6D4"),
    UTILITIES("Utilities", "#64748B"),
    HEALTH("Health & Fitness", "#14B8A6"),
    GAMES("Games", "#F97316"),
    OTHER("Other", "#94A3B8");

    companion object {
        fun fromPackage(pkg: String, appName: String = ""): AppCategory {
            val lower = (pkg + " " + appName).lowercase()
            return when {
                lower.contains("instagram") || lower.contains("tiktok") || lower.contains("twitter") ||
                        lower.contains("x.com") || lower.contains("facebook") || lower.contains("threads") ||
                        lower.contains("reddit") || lower.contains("snapchat") || lower.contains("linkedin") -> SOCIAL

                lower.contains("youtube") || lower.contains("netflix") || lower.contains("spotify") ||
                        lower.contains("disney") || lower.contains("hulu") || lower.contains("twitch") ||
                        lower.contains("prime video") || lower.contains("media") || lower.contains("player") -> ENTERTAINMENT

                lower.contains("amazon") || lower.contains("ebay") || lower.contains("walmart") ||
                        lower.contains("target") || lower.contains("temu") || lower.contains("shein") ||
                        lower.contains("shopping") || lower.contains("cart") || lower.contains("store") -> SHOPPING

                lower.contains("bank") || lower.contains("paypal") || lower.contains("venmo") ||
                        lower.contains("cash") || lower.contains("wallet") || lower.contains("crypto") ||
                        lower.contains("finance") || lower.contains("mint") || lower.contains("chase") -> FINANCE

                lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("signal") ||
                        lower.contains("messages") || lower.contains("messenger") || lower.contains("slack") ||
                        lower.contains("discord") || lower.contains("teams") || lower.contains("gmail") ||
                        lower.contains("mail") || lower.contains("dialer") || lower.contains("contacts") -> COMMUNICATION

                lower.contains("notion") || lower.contains("docs") || lower.contains("sheets") ||
                        lower.contains("drive") || lower.contains("keep") || lower.contains("calendar") ||
                        lower.contains("todo") || lower.contains("tasks") || lower.contains("office") ||
                        lower.contains("notes") || lower.contains("obsidian") -> PRODUCTIVITY

                lower.contains("fit") || lower.contains("health") || lower.contains("strava") ||
                        lower.contains("workout") || lower.contains("calm") || lower.contains("headspace") ||
                        lower.contains("running") || lower.contains("diet") -> HEALTH

                lower.contains("game") || lower.contains("play.games") || lower.contains("roblox") ||
                        lower.contains("candy") || lower.contains("puzzle") || lower.contains("clash") -> GAMES

                lower.contains("chrome") || lower.contains("browser") || lower.contains("settings") ||
                        lower.contains("camera") || lower.contains("clock") || lower.contains("calculator") ||
                        lower.contains("maps") || lower.contains("weather") -> UTILITIES

                else -> OTHER
            }
        }
    }
}

@Entity(tableName = "installed_apps")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val isSystemApp: Boolean = false,
    val installedTimestamp: Long = System.currentTimeMillis(),
    val iconColorHex: String = "#6366F1"
)

@Entity(
    tableName = "usage_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["dateStr"])
    ]
)
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val eventType: String, // "OPEN", "NOTIFICATION", "SESSION"
    val timestamp: Long,
    val durationMs: Long = 0L,
    val hourOfDay: Int, // 0 - 23
    val dayOfWeek: Int, // 1 (Sun) - 7 (Sat)
    val dateStr: String, // "YYYY-MM-DD"
    val isCompulsiveTrigger: Boolean = false
)

@Entity(
    tableName = "daily_aggregates",
    indices = [
        Index(value = ["dateStr"]),
        Index(value = ["packageName"])
    ]
)
data class DailyAggregateEntity(
    @PrimaryKey val id: String, // "$dateStr-$packageName"
    val dateStr: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val totalDurationMs: Long,
    val openCount: Int,
    val notificationCount: Int,
    val morningMinutes: Int = 0,    // 05:00 - 11:59
    val afternoonMinutes: Int = 0,  // 12:00 - 16:59
    val eveningMinutes: Int = 0,    // 17:00 - 21:59
    val nightMinutes: Int = 0,      // 22:00 - 04:59
    val compulsiveOpens: Int = 0,
    val stepsCount: Int = 0
)

@Entity(tableName = "habit_insights")
data class HabitInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val periodLabel: String = "Past 7 Days",
    val dominantAppsJson: String = "",
    val peakActiveHours: String = "",
    val compulsiveScore: Int = 50, // 0-100 scale (0 = highly intentional, 100 = highly compulsive)
    val compulsiveSummary: String = "",
    val productivityTrend: String = "",
    val keyTakeaway: String = "",
    val fullAnalysisText: String = "",
    val isSyncedWithBackend: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "AI"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val groundedDataSummary: String? = null
)

@Entity(tableName = "app_recommendations")
data class AppRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetPackageName: String,
    val targetAppName: String,
    val targetCategory: String,
    val suggestedAppName: String,
    val suggestedPackageName: String,
    val reason: String,
    val efficiencyBadge: String,
    val keyBenefits: String
)

@Entity(tableName = "habit_goals")
data class HabitGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val goalType: String, // "MAX_LIMIT" (stay below) or "MIN_TARGET" (achieve at least)
    val category: String, // "SOCIAL", "PRODUCTIVITY", "BEDTIME", "STEPS", "COMPULSIVE_OPENS"
    val targetValue: Int, // e.g. 60 (min), 45 (min), 22 (10 PM), 8000 (steps), 20 (opens)
    val unit: String, // "min", "steps", "opens", "PM"
    val isEnabled: Boolean = true,
    val iconKey: String = "timer",
    val colorHex: String = "#BA1A1A"
)

enum class UserRole(
    val displayName: String,
    val defaultSchedule: String,
    val defaultFocusStart: Int,
    val defaultFocusEnd: Int,
    val defaultBedtime: Int,
    val defaultScreenLimitMinutes: Int,
    val defaultIsKid: Boolean
) {
    KID_STUDENT("Kid / School Student (Ages 6-12)", "School Hours (8:00 AM - 3:00 PM)", 8, 15, 21, 120, true),
    TEEN_STUDENT("Teen Student (Ages 13-17)", "School & Study (8:30 AM - 4:00 PM)", 8, 16, 22, 180, false),
    COLLEGE_STUDENT("College / University", "Lectures & Study (9:00 AM - 5:00 PM)", 9, 17, 23, 240, false),
    WORKING_PROFESSIONAL("Working Professional", "Standard Business (9:00 AM - 5:00 PM)", 9, 17, 23, 210, false),
    REMOTE_FREELANCER("Freelancer / Remote Work", "Flexible Deep Work (10:00 AM - 6:00 PM)", 10, 18, 23, 240, false),
    HOMEMAKER_PARENT("Homemaker / Parent", "Daily Routine (7:00 AM - 9:00 PM)", 7, 21, 22, 180, false),
    RETIRED("Retired / Senior", "Leisure & Wellness", 9, 21, 22, 180, false),
    OTHER("Custom Persona", "Personalized Schedule", 9, 17, 23, 210, false);

    companion object {
        fun fromKey(key: String): UserRole = entries.find { it.name == key } ?: WORKING_PROFESSIONAL
    }
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "You",
    val age: Int = 26,
    val gender: String = "Prefer not to say", // "Female", "Male", "Non-binary", "Prefer not to say"
    val roleKey: String = "WORKING_PROFESSIONAL",
    val occupationTitle: String = "Working Professional", // e.g. "5th Grade Student", "Software Engineer"
    val scheduleType: String = "STANDARD_WORK", // "SCHOOL_HOURS", "STANDARD_WORK", "FLEXIBLE", "CUSTOM"
    val focusStartHour: Int = 9, // e.g. 9 for 9:00 AM
    val focusEndHour: Int = 17, // e.g. 17 for 5:00 PM
    val daysOffCsv: String = "SATURDAY,SUNDAY",
    val bedtimeHour: Int = 23, // 23 = 11:00 PM
    val wakeHour: Int = 7, // 7 = 7:00 AM
    val primaryGoalsCsv: String = "REDUCE_BEDTIME_SCROLL,PROTECT_FOCUS,LIMIT_SOCIAL_UNLOCKS,PHYSICAL_ACTIVITY",
    val dailyScreenTimeTargetMinutes: Int = 210,
    val isKidMode: Boolean = false,
    val isProfileCompleted: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getRole(): UserRole = UserRole.fromKey(roleKey)

    fun getDaysOffList(): List<String> = daysOffCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun isTodayDayOff(calendar: java.util.Calendar = java.util.Calendar.getInstance()): Boolean {
        val dayOfWeek = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "MONDAY"
            java.util.Calendar.TUESDAY -> "TUESDAY"
            java.util.Calendar.WEDNESDAY -> "WEDNESDAY"
            java.util.Calendar.THURSDAY -> "THURSDAY"
            java.util.Calendar.FRIDAY -> "FRIDAY"
            java.util.Calendar.SATURDAY -> "SATURDAY"
            java.util.Calendar.SUNDAY -> "SUNDAY"
            else -> "SUNDAY"
        }
        return getDaysOffList().contains(dayOfWeek)
    }

    fun isHourInFocusSchedule(hour: Int): Boolean {
        return if (focusStartHour <= focusEndHour) {
            hour in focusStartHour until focusEndHour
        } else {
            hour >= focusStartHour || hour < focusEndHour
        }
    }

    fun getGoalsList(): List<String> = primaryGoalsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

data class GoalProgressItem(
    val goal: HabitGoalEntity,
    val currentValue: Int,
    val progressFraction: Float, // 0.0f to 1.0f+
    val isAchieved: Boolean,
    val statusText: String
)

data class WeekOverWeekCategoryStat(
    val category: AppCategory,
    val currentWeekMinutes: Int,
    val previousWeekMinutes: Int,
    val currentWeekDailyAvgMinutes: Int,
    val previousWeekDailyAvgMinutes: Int,
    val percentChange: Float, // e.g. -15.4f or +25.0f
    val isPositiveTrend: Boolean, // e.g. reducing social is positive, increasing productivity is positive
    val deltaMinutes: Int
)

data class WeekOverWeekSummary(
    val currentWeekTotalMinutes: Int,
    val previousWeekTotalMinutes: Int,
    val currentWeekDailyAvgMinutes: Int,
    val previousWeekDailyAvgMinutes: Int,
    val totalPercentChange: Float,
    val currentWeekCompulsiveOpens: Int,
    val previousWeekCompulsiveOpens: Int,
    val compulsivePercentChange: Float,
    val currentWeekAvgSteps: Int,
    val previousWeekAvgSteps: Int,
    val stepsPercentChange: Float,
    val categoryChanges: List<WeekOverWeekCategoryStat>,
    val headlineInsight: String,
    val topImprovedCategory: String,
    val topWatchCategory: String
)

data class BehaviorForecast(
    val projectedTodayMinutes: Int,
    val pacingPacePercent: Int, // e.g. +18% vs usual at this time
    val pacingStatus: String, // "ON_TRACK", "PACING_HIGH", "OPTIMAL"
    val bedtimeDoomscrollRisk: String, // "LOW", "MODERATE", "HIGH"
    val bedtimeRiskReason: String,
    val projectedWeeklyMinutes: Int,
    val confidenceScore: Int, // 0-100%
    val recommendedMicroHabit: String
)

data class ProactiveNudge(
    val id: String,
    val type: String, // "MISSED_HABIT", "GOAL_WARNING", "MOMENTUM_STREAK", "RECOVERY_OPPORTUNITY"
    val title: String,
    val message: String,
    val severity: String, // "INFO", "WARNING", "ALERT", "SUCCESS"
    val actionText: String? = null,
    val categoryTag: String = "WELLNESS"
)

data class HabitDimensionScore(
    val name: String,
    val score: Int, // 0-100
    val ratingLabel: String,
    val description: String,
    val statusColorHex: String
)

data class DayTrendData(
    val dateStr: String,
    val dayName: String, // "Mon", "Tue", "Wed", etc.
    val fullDateLabel: String, // "Aug 28"
    val screenTimeMinutes: Int,
    val notificationCount: Int,
    val openCount: Int,
    val compulsiveOpens: Int,
    val socialMinutes: Int,
    val productivityMinutes: Int,
    val entertainmentMinutes: Int,
    val otherMinutes: Int,
    val topApp: String,
    val topAppMinutes: Int,
    val topNotifyingApp: String,
    val topNotifyingAppCount: Int,
    val isToday: Boolean = false
)

data class AppNotificationFrequencyStat(
    val appName: String,
    val packageName: String,
    val category: String,
    val totalNotifications: Int,
    val percentOfTotal: Int,
    val openConversionRate: Float,
    val dailyCounts: List<Int> = emptyList()
)

data class WeeklyChartTrendsState(
    val dayTrends: List<DayTrendData> = emptyList(),
    val totalWeeklyScreenTimeMinutes: Int = 0,
    val avgDailyScreenTimeMinutes: Int = 0,
    val totalWeeklyNotifications: Int = 0,
    val avgDailyNotifications: Int = 0,
    val peakNotificationDay: String = "",
    val peakNotificationHourRange: String = "7:00 PM - 9:00 PM",
    val peakNotificationCount: Int = 0,
    val topNotifyingApps: List<AppNotificationFrequencyStat> = emptyList(),
    val notificationToOpenConversionRate: Int = 42,
    val screenTimeVersusNotificationInsight: String = "",
    val weeklyScreenTimeTrendDeltaPct: Float = 0f,
    val weeklyNotificationTrendDeltaPct: Float = 0f
)


