package com.example.data.collector

import android.content.Context
import com.example.data.db.HabitDatabase
import com.example.data.model.AppCategory
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.DailyAggregateEntity
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

        // Generate past 7 days of realistic time-series usage
        for (dayOffset in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.DAY_OF_YEAR, -dayOffset)
            }
            val dateStr = dateFormat.format(dayCal.time)
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            // Profile for Instagram (High night use, high compulsive opens)
            val instaMins = if (isWeekend) 95 else (50 + Random.nextInt(40))
            val instaOpens = 24 + Random.nextInt(15)
            val instaNotifs = 18 + Random.nextInt(12)
            val instaNight = (instaMins * 0.45).toInt()
            val instaEvening = (instaMins * 0.35).toInt()
            val instaAfternoon = (instaMins * 0.15).toInt()
            val instaMorning = instaMins - instaNight - instaEvening - instaAfternoon
            val instaCompulsive = (instaOpens * 0.6).toInt()

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
                    stepsCount = 6500 + Random.nextInt(3000)
                )
            )

            // Profile for YouTube (Long sessions, evening & late night)
            val ytMins = if (isWeekend) 110 else (45 + Random.nextInt(35))
            val ytOpens = 6 + Random.nextInt(5)
            val ytNotifs = 4 + Random.nextInt(4)
            val ytNight = (ytMins * 0.50).toInt()
            val ytEvening = (ytMins * 0.40).toInt()
            val ytAfternoon = (ytMins * 0.10).toInt()
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
                    afternoonMinutes = ytAfternoon,
                    eveningMinutes = ytEvening,
                    nightMinutes = ytNight,
                    compulsiveOpens = 1
                )
            )

            // Profile for Slack (Work hours, weekdays heavy)
            val slackMins = if (isWeekend) 5 else (65 + Random.nextInt(30))
            val slackOpens = if (isWeekend) 2 else (20 + Random.nextInt(10))
            val slackNotifs = if (isWeekend) 3 else (35 + Random.nextInt(20))
            val slackMorning = (slackMins * 0.45).toInt()
            val slackAfternoon = (slackMins * 0.50).toInt()
            val slackEvening = slackMins - slackMorning - slackAfternoon
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
                    compulsiveOpens = (slackOpens * 0.3).toInt()
                )
            )

            // Profile for Notion (Deep intentional work)
            val notionMins = if (isWeekend) 15 else (55 + Random.nextInt(25))
            val notionOpens = 4 + Random.nextInt(3)
            val notionMorning = (notionMins * 0.60).toInt()
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

            // Profile for Reddit (Evening doomscroll)
            val redditMins = 30 + Random.nextInt(35)
            val redditOpens = 12 + Random.nextInt(8)
            val redditNotifs = 6 + Random.nextInt(4)
            val redditNight = (redditMins * 0.60).toInt()
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
                    compulsiveOpens = (redditOpens * 0.7).toInt()
                )
            )

            // Profile for Amazon (Shopping)
            val amazonMins = 15 + Random.nextInt(20)
            val amazonOpens = 5 + Random.nextInt(4)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.amazon.mShop.android.shopping",
                    dateStr = dateStr,
                    packageName = "com.amazon.mShop.android.shopping",
                    appName = "Amazon Shopping",
                    category = AppCategory.SHOPPING.name,
                    totalDurationMs = amazonMins * 60000L,
                    openCount = amazonOpens,
                    notificationCount = 5,
                    morningMinutes = 0,
                    afternoonMinutes = (amazonMins * 0.3).toInt(),
                    eveningMinutes = (amazonMins * 0.7).toInt(),
                    nightMinutes = 0,
                    compulsiveOpens = (amazonOpens * 0.4).toInt()
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
                    totalDurationMs = (4 + Random.nextInt(6)) * 60000L,
                    openCount = 2 + Random.nextInt(2),
                    notificationCount = 2,
                    morningMinutes = 2,
                    afternoonMinutes = 2,
                    eveningMinutes = 1,
                    nightMinutes = 0,
                    compulsiveOpens = 0
                )
            )

            // Profile for Duolingo (Productivity streak)
            aggregates.add(
                DailyAggregateEntity(
                    id = "$dateStr-com.duolingo",
                    dateStr = dateStr,
                    packageName = "com.duolingo",
                    appName = "Duolingo",
                    category = AppCategory.PRODUCTIVITY.name,
                    totalDurationMs = 12 * 60000L,
                    openCount = 1,
                    notificationCount = 2,
                    morningMinutes = 12,
                    afternoonMinutes = 0,
                    eveningMinutes = 0,
                    nightMinutes = 0,
                    compulsiveOpens = 0
                )
            )

            // Generate fine-grained timestamp events for the current / most recent days
            if (dayOffset <= 2) {
                // Morning events
                generateEventsForApp(events, "notion.id", "Notion", dayCal, 8, 30, 25 * 60000L, false)
                generateEventsForApp(events, "com.duolingo", "Duolingo", dayCal, 9, 15, 12 * 60000L, false)
                generateEventsForApp(events, "com.Slack", "Slack", dayCal, 10, 0, 15 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 11, 45, 20000L, true)

                // Afternoon events
                generateEventsForApp(events, "com.Slack", "Slack", dayCal, 14, 10, 30 * 60000L, false)
                generateEventsForApp(events, "com.amazon.mShop.android.shopping", "Amazon Shopping", dayCal, 15, 20, 8 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 16, 50, 15000L, true)

                // Evening & Late Night events (Compulsive Doomscroll spike)
                generateEventsForApp(events, "com.google.android.youtube", "YouTube", dayCal, 20, 15, 45 * 60000L, false)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 22, 10, 35 * 60000L, false)
                generateEventsForApp(events, "com.reddit.frontpage", "Reddit", dayCal, 23, 15, 25 * 60000L, true)
                generateEventsForApp(events, "com.instagram.android", "Instagram", dayCal, 23, 50, 18000L, true)
                generateEventsForApp(events, "com.reddit.frontpage", "Reddit", dayCal, 0, 25, 20 * 60000L, true)
            }
        }

        db.dailyAggregateDao().insertAggregates(aggregates)
        db.usageEventDao().insertEvents(events)

        // Seed smart Initial AI Insight
        val sampleInsight = HabitInsightEntity(
            timestamp = System.currentTimeMillis(),
            periodLabel = "Past 7 Days AI Synthesis",
            dominantAppsJson = "Instagram (26%), YouTube (24%), Slack (21%), Notion (15%), Reddit (14%)",
            peakActiveHours = "22:00 - 01:00 (Night Owl Doomscrolling peak)",
            compulsiveScore = 64,
            compulsiveSummary = "64% of Instagram & Reddit opens are rapid reflex checks (<30s), heavily concentrated between 10 PM and 1 AM after notification triggers.",
            productivityTrend = "Productivity sessions (Notion + Duolingo) are strong in the mornings (8-10 AM), but evening recovery time is dominated by passive infinite feeds.",
            keyTakeaway = "Your morning routine is highly intentional and focused, but late-night fatigue triggers compulsive social media micro-checks. Setting a 10 PM downtime boundary could reclaim ~75 mins daily.",
            fullAnalysisText = """
                ### Comprehensive Digital Habit Synthesis
                - **Dominant Apps**: Instagram and YouTube capture 50% of your total device screen time.
                - **Time of Day Hotspot**: Peak usage is shifted late at night (22:00 to 01:00), accounting for 48% of recreational usage.
                - **Compulsive vs Intentional**:
                  - *Intentional*: Morning Notion and Duolingo sessions average 22 minutes of sustained focus.
                  - *Compulsive*: 68% of Instagram launches last under 30 seconds, indicating subconscious reflexive checking rather than deliberate browsing.
                - **Shift Analysis**: Weekday work hours maintain healthy boundaries with Slack, but post-9 PM usage creates a sleep-displacement loop.
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
                suggestedAppName = "BeReal / Minimalist Feed",
                suggestedPackageName = "com.bereal.ft",
                reason = "Replaces infinite algorithmic doomscrolling with a single daily intentional snapshot, reducing late-night screen time by up to 80%.",
                efficiencyBadge = "Intentional Socializing",
                keyBenefits = "• 1 notification per day instead of 25+\n• No infinite reels algorithm\n• Genuine peer connection over passive consumption"
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
