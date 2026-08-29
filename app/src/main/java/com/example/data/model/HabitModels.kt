package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    ENTERTAINMENT("Entertainment"),
    SHOPPING("Shopping & Retail"),
    FINANCE("Finance & Payments"),
    COMMUNICATION("Communication"),
    UTILITIES("Utilities"),
    HEALTH("Health & Fitness"),
    GAMES("Games"),
    OTHER("Other");

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
