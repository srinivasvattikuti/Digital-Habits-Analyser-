package com.example.data.collector

import android.content.Context
import com.example.data.db.HabitDatabase
import com.example.data.model.AppCategory
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SampleDataGenerator(private val context: Context) {

    private val db = HabitDatabase.getDatabase(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun populateRichDemoData(forceOverwrite: Boolean = false) = withContext(Dispatchers.IO) {
        val existingCount = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01").size
        if (existingCount > 0 && !forceOverwrite) return@withContext

        // 1. Preset Apps
        val sampleApps = listOf(
            AppInfoEntity("com.instagram.android", "Instagram", AppCategory.SOCIAL.name, false, System.currentTimeMillis() - 86400000L * 30, "#EC4899"),
            AppInfoEntity("com.google.android.youtube", "YouTube", AppCategory.ENTERTAINMENT.name, false, System.currentTimeMillis() - 86400000L * 60, "#EF4444"),
            AppInfoEntity("com.Slack", "Slack", AppCategory.COMMUNICATION.name, false, System.currentTimeMillis() - 86400000L * 45, "#3B82F6"),
            AppInfoEntity("notion.id", "Notion", AppCategory.PRODUCTIVITY.name, false, System.currentTimeMillis() - 86400000L * 20, "#10B981"),
            AppInfoEntity("com.amazon.mShop.android.shopping", "Amazon Shopping", AppCategory.SHOPPING.name, false, System.currentTimeMillis() - 86400000L * 40, "#F59E0B"),
            AppInfoEntity("com.chase.sig.android", "Chase Mobile", AppCategory.FINANCE.name, false, System.currentTimeMillis() - 86400000L * 90, "#06B6D4"),
            AppInfoEntity("com.spotify.music", "Spotify", AppCategory.ENTERTAINMENT.name, false, System.currentTimeMillis() - 86400000L * 100, "#22C55E"),
            AppInfoEntity("com.reddit.frontpage", "Reddit", AppCategory.SOCIAL.name, false, System.currentTimeMillis() - 86400000L * 15, "#F97316"),
            AppInfoEntity("com.duolingo", "Duolingo", AppCategory.PRODUCTIVITY.name, false, System.currentTimeMillis() - 86400000L * 25, "#84CC16"),
            AppInfoEntity("com.google.android.gm", "Gmail", AppCategory.COMMUNICATION.name, true, System.currentTimeMillis() - 86400000L * 120, "#EA4335")
        )
        db.appInfoDao().insertApps(sampleApps)

        val calendar = Calendar.getInstance()
        val aggregates = mutableListOf<DailyAggregateEntity>()
        val events = mutableListOf<UsageEventEntity>()

        // Generate past 14 days of realistic time-series usage (Week 1 = days 7..13, Week 2 = days 0..6)
        for (dayOffset in 13 downTo 0) {
            val isPreviousWeek = dayOffset >= 7
            val dayCal = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.DAY_OF_YEAR, -dayOffset)
            }
            val dateStr = dateFormat.format(dayCal.time)
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            // Profile for Instagram (Previous week was higher social, current week reduced by ~20%)
            val baseInsta = if (isWeekend) 95 else 65
            val instaMins = if (isPreviousWeek) (baseInsta + 20 + Random.nextInt(15)) else (baseInsta - 15 + Random.nextInt(15))
            val instaOpens = if (isPreviousWeek) (32 + Random.nextInt(10)) else (20 + Random.nextInt(8))
            val instaNotifs = 18 + Random.nextInt(10)
            val instaNight = (instaMins * (if (isPreviousWeek) 0.50 else 0.35)).toInt()
            val instaEvening = (instaMins * 0.35).toInt()
            val instaAfternoon = (instaMins * 0.20).toInt()
            val instaMorning = (instaMins - instaNight - instaEvening - instaAfternoon).coerceAtLeast(0)
            val instaCompulsive = (instaOpens * (if (isPreviousWeek) 0.70 else 0.50)).toInt()

            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.instagram.android",
                    dateStr = dateStr,
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    category = AppCategory.SOCIAL.name,
                    totalDurationMs = instaMins * 60000L,
                    openCount = instaOpens,
                    notificationCount = instaNotifs,
                    morningMinutes = instaMorning,
                    afternoonMinutes = instaAfternoon,
                    eveningMinutes = instaEvening,
                    nightMinutes = instaNight,
                    compulsiveOpens = instaCompulsive,
                    stepsCount = if (isPreviousWeek) 5800 + Random.nextInt(2000) else 7800 + Random.nextInt(2500)
                )
            )

            // Profile for YouTube (Entertainment)
            val baseYt = if (isWeekend) 100 else 55
            val ytMins = if (isPreviousWeek) (baseYt + 15) else baseYt
            val ytOpens = 6 + Random.nextInt(4)
            val ytNotifs = 4 + Random.nextInt(3)
            val ytNight = (ytMins * 0.45).toInt()
            val ytEvening = (ytMins * 0.40).toInt()
            val ytAfternoon = ytMins - ytNight - ytEvening
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.google.android.youtube",
                    dateStr = dateStr,
                    packageName = "com.google.android.youtube",
                    appName = "YouTube",
                    category = AppCategory.ENTERTAINMENT.name,
                    totalDurationMs = ytMins * 60000L,
                    openCount = ytOpens,
                    notificationCount = ytNotifs,
                    morningMinutes = 0,
                    afternoonMinutes = ytAfternoon.coerceAtLeast(0),
                    eveningMinutes = ytEvening,
                    nightMinutes = ytNight,
                    compulsiveOpens = 1
                )
            )

            // Profile for Slack (Communication)
            val slackMins = if (isWeekend) 5 else (60 + Random.nextInt(25))
            val slackOpens = if (isWeekend) 2 else (18 + Random.nextInt(8))
            val slackNotifs = if (isWeekend) 3 else (30 + Random.nextInt(15))
            val slackMorning = (slackMins * 0.45).toInt()
            val slackAfternoon = (slackMins * 0.50).toInt()
            val slackEvening = (slackMins - slackMorning - slackAfternoon).coerceAtLeast(0)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.Slack",
                    dateStr = dateStr,
                    packageName = "com.Slack",
                    appName = "Slack",
                    category = AppCategory.COMMUNICATION.name,
                    totalDurationMs = slackMins * 60000L,
                    openCount = slackOpens,
                    notificationCount = slackNotifs,
                    morningMinutes = slackMorning,
                    afternoonMinutes = slackAfternoon,
                    eveningMinutes = slackEvening,
                    nightMinutes = 0,
                    compulsiveOpens = (slackOpens * 0.25).toInt()
                )
            )

            // Profile for Notion (Productivity - increased by ~30% in current week)
            val baseNotion = if (isWeekend) 15 else 45
            val notionMins = if (isPreviousWeek) baseNotion else (baseNotion + 25 + Random.nextInt(15))
            val notionOpens = 5 + Random.nextInt(3)
            val notionMorning = (notionMins * 0.65).toInt()
            val notionAfternoon = notionMins - notionMorning
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-notion.id",
                    dateStr = dateStr,
                    packageName = "notion.id",
                    appName = "Notion",
                    category = AppCategory.PRODUCTIVITY.name,
                    totalDurationMs = notionMins * 60000L,
                    openCount = notionOpens,
                    notificationCount = 1,
                    morningMinutes = notionMorning,
                    afternoonMinutes = notionAfternoon,
                    eveningMinutes = 0,
                    nightMinutes = 0,
                    compulsiveOpens = 0
                )
            )

            // Profile for Reddit (Social - decreased in current week)
            val baseReddit = if (isPreviousWeek) 55 else 28
            val redditMins = baseReddit + Random.nextInt(15)
            val redditOpens = if (isPreviousWeek) 16 else 8
            val redditNotifs = 5
            val redditNight = (redditMins * 0.55).toInt()
            val redditEvening = redditMins - redditNight
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.reddit.frontpage",
                    dateStr = dateStr,
                    packageName = "com.reddit.frontpage",
                    appName = "Reddit",
                    category = AppCategory.SOCIAL.name,
                    totalDurationMs = redditMins * 60000L,
                    openCount = redditOpens,
                    notificationCount = redditNotifs,
                    morningMinutes = 0,
                    afternoonMinutes = 0,
                    eveningMinutes = redditEvening,
                    nightMinutes = redditNight,
                    compulsiveOpens = (redditOpens * 0.6).toInt()
                )
            )

            // Profile for Amazon (Shopping)
            val amazonMins = 12 + Random.nextInt(15)
            val amazonOpens = 4 + Random.nextInt(3)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.amazon.mShop.android.shopping",
                    dateStr = dateStr,
                    packageName = "com.amazon.mShop.android.shopping",
                    appName = "Amazon Shopping",
                    category = AppCategory.SHOPPING.name,
                    totalDurationMs = amazonMins * 60000L,
                    openCount = amazonOpens,
                    notificationCount = 4,
                    morningMinutes = 0,
                    afternoonMinutes = (amazonMins * 0.4).toInt(),
                    eveningMinutes = (amazonMins * 0.6).toInt(),
                    nightMinutes = 0,
                    compulsiveOpens = (amazonOpens * 0.3).toInt()
                )
            )

            // Profile for Chase (Finance)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.chase.sig.android",
                    dateStr = dateStr,
                    packageName = "com.chase.sig.android",
                    appName = "Chase Mobile",
                    category = AppCategory.FINANCE.name,
                    totalDurationMs = (4 + Random.nextInt(4)) * 60000L,
                    openCount = 2,
                    notificationCount = 2,
                    morningMinutes = 2,
                    afternoonMinutes = 2,
                    eveningMinutes = 0,
                    nightMinutes = 0,
                    compulsiveOpens = 0
                )
            )

            // Profile for Duolingo (Productivity)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.duolingo",
                    dateStr = dateStr,
                    packageName = "com.duolingo",
                    appName = "Duolingo",
                    category = AppCategory.PRODUCTIVITY.name,
                    totalDurationMs = (if (isPreviousWeek) 10 else 18) * 60000L,
                    openCount = 1,
                    notificationCount = 2,
                    morningMinutes = if (isPreviousWeek) 10 else 18,
                    afternoonMinutes = 0,
                    eveningMinutes = 0,
                    nightMinutes = 0,
                    compulsiveOpens = 0
                )
            )

            // Generate fine-grained timestamp events for the current / most recent days
            if (dayOffset <= 3) {
                // Morning events
                generateEventsForApp(events, "notion.id", "Notion", dayCal, 8, 30, 30 * 60000L, false)
                generateEventsForApp(events, "com.duolingo", "Duolingo", dayCal, 9, 15, 18 * 60000L, false)
                generateEventsForApp(events, "com.Slack", "Slack", dayCal, 10, 0, 20 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 11, 45, 20000L, true)

                // Afternoon events
                generateEventsForApp(events, "com.Slack", "Slack", dayCal, 14, 10, 30 * 60000L, false)
                generateEventsForApp(events, "com.amazon.mShop.android.shopping", "Amazon Shopping", dayCal, 15, 20, 8 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 16, 50, 15000L, true)

                // Evening & Late Night events
                generateEventsForApp(events, "com.google.android.youtube", "YouTube", dayCal, 20, 15, 45 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 22, 10, 25 * 60000L, false)
                generateEventsForApp(events, "com.reddit.frontpage", "Reddit", dayCal, 23, 15, 15 * 60000L, true)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 23, 50, 15000L, true)
            }
        }

        db.dailyAggregateDao().insertAggregates(aggregates)
        db.usageEventDao().insertEvents(events)

        // Seed Habit Goals
        val defaultGoals = listOf(
            HabitGoalEntity(
                id = "GOAL_SOCIAL_MAX",
                title = "Social Media Cap",
                description = "Maximum daily combined social scrolling (Instagram & Reddit)",
                goalType = "MAX_LIMIT",
                category = "SOCIAL",
                targetValue = 60,
                unit = "min",
                isEnabled = true,
                iconKey = "social"
            ),
            HabitGoalEntity(
                id = "GOAL_PROD_MIN",
                title = "Deep Focus & Study",
                description = "Daily target for intentional learning and focus (Notion & Duolingo)",
                goalType = "MIN_TARGET",
                category = "PRODUCTIVITY",
                targetValue = 60,
                unit = "min",
                isEnabled = true,
                iconKey = "work"
            ),
            HabitGoalEntity(
                id = "GOAL_BEDTIME_CUTOFF",
                title = "Bedtime Downtime",
                description = "Cease recreational screen engagement before 10:30 PM",
                goalType = "MAX_LIMIT",
                category = "BEDTIME",
                targetValue = 22, // 10 PM
                unit = "PM",
                isEnabled = true,
                iconKey = "bedtime"
            ),
            HabitGoalEntity(
                id = "GOAL_COMPULSIVE_LIMIT",
                title = "Reflex Opens Guard",
                description = "Cap unconscious micro-checks (<30s unlocks) per day",
                goalType = "MAX_LIMIT",
                category = "COMPULSIVE_OPENS",
                targetValue = 20,
                unit = "opens",
                isEnabled = true,
                iconKey = "touch"
            ),
            HabitGoalEntity(
                id = "GOAL_STEPS_MIN",
                title = "Physical Movement",
                description = "Daily active step count target for mental wellness balance",
                goalType = "MIN_TARGET",
                category = "STEPS",
                targetValue = 8000,
                unit = "steps",
                isEnabled = true,
                iconKey = "steps"
            )
        )
        db.habitGoalDao().insertGoals(defaultGoals)

        // Seed smart Initial AI Insight
        val sampleInsight = HabitInsightEntity(
            timestamp = System.currentTimeMillis(),
            periodLabel = "Past 14 Days AI Synthesis",
            dominantAppsJson = "Instagram (24%), Notion (22%), YouTube (21%), Slack (19%), Reddit (10%)",
            peakActiveHours = "21:30 - 23:30 (Shifting 45 mins earlier from previous week)",
            compulsiveScore = 52,
            compulsiveSummary = "Compulsive micro-opens decreased by 18% compared to last week, driven by reduced midnight Instagram triggers.",
            productivityTrend = "+32% increase in morning deep-focus blocks (Notion + Duolingo), showing significant intentional habit gains.",
            keyTakeaway = "Your digital habit profile shows clear week-over-week improvement: Social screen time is down 21%, deep work is up 32%, and late-night screen cut-off is trending 45 minutes earlier.",
            fullAnalysisText = """
                ### Comprehensive Digital Habit Synthesis & Forecast
                - **Week-over-Week Momentum**:
                  - *Social & Media*: Decreased from 118 min/day average to 92 min/day (-22%).
                  - *Deep Work & Productivity*: Increased from 48 min/day to 74 min/day (+54%).
                  - *Compulsive Checks*: Reduced from 38 daily reflex opens to 24 daily opens (-37%).
                - **Predictive Behavioral Forecast**:
                  - At current usage pacing, you are on track to meet your 60-minute daily social cap 5 out of 7 days this week.
                  - Late-night doomscroll risk is evaluated at **MODERATE**: Evening alerts after 9 PM still trigger 42% of recreational opens.
                - **Proactive Suggestions**:
                  - Maintain your morning 8:30-10:00 AM Notion focus window—it has become your most resilient deep work routine.
                  - Consider enabling Do Not Disturb at 9:45 PM to lock in your bedtime downtime goal.
            """.trimIndent(),
            isSyncedWithBackend = true
        )
        db.habitInsightDao().insertInsight(sampleInsight)

        // Seed App Recommendations
        val recommendations = listOf(
            AppRecommendationEntity(
                targetPackageName = "com.amazon.mShop.android.shopping",
                targetAppName = "Amazon Shopping",
                targetCategory = "Shopping & Retail",
                suggestedAppName = "Shop (by Shopify)",
                suggestedPackageName = "com.shopify.arrive",
                reason = "Consolidates package tracking and curated local stores without algorithmic push notifications that trigger compulsive impulse buying.",
                efficiencyBadge = "Reduced Impulse Triggers",
                keyBenefits = "• Unified package tracking across all stores\n• Zero intrusive flash sale notifications\n• Clean, distraction-free checkout"
            ),
            AppRecommendationEntity(
                targetPackageName = "com.instagram.android",
                targetAppName = "Instagram",
                targetCategory = "Social",
                suggestedAppName = "One Sec / Mindful Pause",
                suggestedPackageName = "app.one.sec",
                reason = "Interposes an animated 2-second breathing pause before launch, breaking reflexive unlock triggers and reducing unconscious screen time by 57%.",
                efficiencyBadge = "Friction Nudge Engine",
                keyBenefits = "• Scientifically proven 57% usage drop\n• Guided micro-breathing prompts\n• Intentional unlock confirmation"
            ),
            AppRecommendationEntity(
                targetPackageName = "com.reddit.frontpage",
                targetAppName = "Reddit",
                targetCategory = "Social & News",
                suggestedAppName = "Matter / Artifact Reader",
                suggestedPackageName = "com.matter.reader",
                reason = "Curated deep-reading environment with text-to-speech that encourages long-form thoughtful learning instead of chaotic comment thread rabbit holes.",
                efficiencyBadge = "High Signal-to-Noise",
                keyBenefits = "• Clean typography and read-it-later sync\n• AI key takeaways for long articles\n• Offline readability with no ad clutter"
            ),
            AppRecommendationEntity(
                targetPackageName = "com.chase.sig.android",
                targetAppName = "Chase Mobile",
                targetCategory = "Finance",
                suggestedAppName = "Copilot Money / Lunch Money",
                suggestedPackageName = "com.copilot.money",
                reason = "Streamlined multi-bank budgeting with automated categorization, saving you multiple daily app checks to verify transactions.",
                efficiencyBadge = "Automated Insights",
                keyBenefits = "• Single view of all accounts and cards\n• Smart recurring subscription detection\n• Clean, privacy-first interface"
            )
        )
        db.appRecommendationDao().insertRecommendations(recommendations)
    }

    private fun generateEventsForApp(
        events: MutableList<UsageEventEntity>,
        pkg: String,
        appName: String,
        dayCal: Calendar,
        hour: Int,
        minute: Int,
        durationMs: Long,
        isCompulsive: Boolean
    ) {
        val cal = Calendar.getInstance().apply {
            time = dayCal.time
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        val timestamp = cal.timeInMillis
        val dateStr = dateFormat.format(cal.time)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        events.add(
            UsageEventEntity(
                packageName = pkg,
                appName = appName,
                eventType = if (durationMs > 0) "SESSION" else "OPEN",
                timestamp = timestamp,
                durationMs = durationMs,
                hourOfDay = hour,
                dayOfWeek = dayOfWeek,
                dateStr = dateStr,
                isCompulsiveTrigger = isCompulsive
            )
        )
    }
}
