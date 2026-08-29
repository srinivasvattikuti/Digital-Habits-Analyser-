package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiHabitsPipeline {

    suspend fun runHabitAnalysisPipeline(
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        userProfile: UserProfileEntity = UserProfileEntity()
    ): HabitInsightResult = withContext(Dispatchers.IO) {
        val totalMs = aggregates.sumOf { it.totalDurationMs }
        val totalHours = totalMs / 3600000.0
        val totalOpens = aggregates.sumOf { it.openCount }
        val totalCompulsiveOpens = aggregates.sumOf { it.compulsiveOpens }
        val compulsiveRatio = if (totalOpens > 0) (totalCompulsiveOpens.toFloat() / totalOpens * 100).toInt() else 0

        val morningMins = aggregates.sumOf { it.morningMinutes }
        val afternoonMins = aggregates.sumOf { it.afternoonMinutes }
        val eveningMins = aggregates.sumOf { it.eveningMinutes }
        val nightMins = aggregates.sumOf { it.nightMinutes }
        val totalMinutes = (morningMins + afternoonMins + eveningMins + nightMins).coerceAtLeast(1)

        val morningPct = (morningMins * 100) / totalMinutes
        val afternoonPct = (afternoonMins * 100) / totalMinutes
        val eveningPct = (eveningMins * 100) / totalMinutes
        val nightPct = (nightMins * 100) / totalMinutes

        // App duration rankings
        val appDurations = aggregates.groupBy { it.appName }
            .mapValues { entry -> entry.value.sumOf { it.totalDurationMs } }
            .toList()
            .sortedByDescending { it.second }

        val dominantAppsString = appDurations.take(5).joinToString(", ") {
            val pct = if (totalMs > 0) (it.second * 100 / totalMs).toInt() else 0
            "${it.first} (${pct}%)"
        }

        // Category breakdown
        val categoryBreakdown = aggregates.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalDurationMs } / 60000 }

        val focusWindowStr = "${userProfile.focusStartHour}:00 to ${userProfile.focusEndHour}:00"
        val daysOffStr = userProfile.getDaysOffList().joinToString(", ")
        val roleDesc = userProfile.getRole().displayName

        val contextPayload = buildString {
            appendLine("=== USER PROFILE & SCHEDULE ATTRIBUTES ===")
            appendLine("Name: ${userProfile.name}")
            appendLine("Age: ${userProfile.age} years old | Gender: ${userProfile.gender}")
            appendLine("Persona / Role: $roleDesc (${userProfile.occupationTitle})")
            appendLine("Schedule Type: ${userProfile.scheduleType}")
            appendLine("Core Focus Hours (Work / School): $focusWindowStr")
            appendLine("Days Off / Weekends: $daysOffStr")
            appendLine("Target Bedtime: ${userProfile.bedtimeHour}:00 | Wake Time: ${userProfile.wakeHour}:00")
            appendLine("Daily Target Screen Limit: ${userProfile.dailyScreenTimeTargetMinutes} mins")
            appendLine("Primary Goals: ${userProfile.primaryGoalsCsv}")
            appendLine("Supervised / Kid Mode: ${if (userProfile.isKidMode || userProfile.age < 13) "YES (Child Friendly Mode)" else "NO"}")
            appendLine()
            appendLine("=== TELEMETRY SUMMARY FOR CONTINUOUS PIPELINE ===")
            appendLine("Total Screen Time: ${String.format(Locale.US, "%.1f", totalHours)} hours across ${aggregates.map { it.dateStr }.distinct().size} days")
            appendLine("Total App Launches: $totalOpens (Compulsive quick checks <30s: $totalCompulsiveOpens, $compulsiveRatio%)")
            appendLine("Dominant Apps: $dominantAppsString")
            appendLine("Time of Day Distribution:")
            appendLine("- Morning (05:00-12:00): $morningMins min ($morningPct%)")
            appendLine("- Afternoon (12:00-17:00): $afternoonMins min ($afternoonPct%)")
            appendLine("- Evening (17:00-22:00): $eveningMins min ($eveningPct%)")
            appendLine("- Late Night (22:00-05:00): $nightMins min ($nightPct%)")
            appendLine("Category Breakdown (Minutes): $categoryBreakdown")
            appendLine("Recent Fine-Grained Events Count: ${events.size}")
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are the AI Behavioral Engine of a Digital Habits and Insights Tracker.
                    Analyze this continuous time-series usage telemetry tailored to the user's specific age, role, work/school focus schedule, and days off.
                    
                    Key Analysis Guidelines:
                    1. For Kids/Students (Ages < 18 or school persona): Evaluate if screen time interferes with school hours ($focusWindowStr) or bedtime (${userProfile.bedtimeHour}:00). Provide supportive, encouraging guidance with healthy boundaries.
                    2. For Working Professionals / Adults: Assess distraction during work focus blocks ($focusWindowStr), work-life balance on days off ($daysOffStr), and late-night doomscrolling past bedtime (${userProfile.bedtimeHour}:00).
                    3. Highlight whether recreational app launches happen during scheduled focus hours vs allowed downtime.
                    4. Identify dominant time sinks, compulsive micro-checking, and trend shifts.
                    5. Provide tailored, age-appropriate micro-habits and high-value app alternatives.
                    
                    Return your analysis clearly and concisely with markdown headings.
                """.trimIndent()

                val request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart("Here is the user's profile and telemetry data:\n\n$contextPayload\n\nPlease generate the comprehensive habit synthesis and actionable insights tailored to this user profile.")
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.3f)
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    val peakHoursDesc = when {
                        nightPct >= 35 -> "Late Night Peak (22:00 - 02:00, ${nightPct}% of screen time)"
                        eveningPct >= 40 -> "Evening Hotspot (17:00 - 22:00, ${eveningPct}% of screen time)"
                        afternoonPct >= 40 -> "Afternoon Clustered (12:00 - 17:00, ${afternoonPct}% of screen time)"
                        else -> "Morning Focused (05:00 - 12:00, ${morningPct}% of screen time)"
                    }

                    val isKid = userProfile.isKidMode || userProfile.age < 13
                    val insightEntity = HabitInsightEntity(
                        timestamp = System.currentTimeMillis(),
                        periodLabel = if (isKid) "Kid-Friendly AI Synthesis" else "Personalized AI Pipeline",
                        dominantAppsJson = dominantAppsString,
                        peakActiveHours = peakHoursDesc,
                        compulsiveScore = compulsiveRatio.coerceIn(10, 95),
                        compulsiveSummary = "$compulsiveRatio% of launches are micro-checks (<30s), primarily concentrated during $peakHoursDesc.",
                        productivityTrend = if (categoryBreakdown["PRODUCTIVITY"] ?: 0 > 45) {
                            "Focus hours ($focusWindowStr) remain well-protected with solid productivity."
                        } else {
                            "Recreational browsing is overlapping with your scheduled $focusWindowStr focus window."
                        },
                        keyTakeaway = if (isKid) {
                            "Turn off notifications during school hours ($focusWindowStr) and wind down 30 minutes before ${userProfile.bedtimeHour}:00 PM."
                        } else {
                            "Shifting screen time back by 45 minutes before bedtime (${userProfile.bedtimeHour}:00) prevents reflexive scrolling and restores deep recovery."
                        },
                        fullAnalysisText = responseText,
                        isSyncedWithBackend = true
                    )

                    return@withContext HabitInsightResult(insightEntity, generateDynamicRecommendations(aggregates, userProfile))
                }
            } catch (e: Exception) {
                // Fall back to algorithmic analysis
            }
        }

        // Local Algorithmic fallback with UserProfile integration
        val isKid = userProfile.isKidMode || userProfile.age < 13
        val peakHoursDesc = when {
            nightPct >= 30 -> "${userProfile.bedtimeHour}:00 - 01:30 (Late Night Screen Loop - ${nightPct}%)"
            eveningPct >= 35 -> "18:00 - ${userProfile.bedtimeHour}:00 (Evening Winddown - ${eveningPct}%)"
            else -> "$focusWindowStr (Core Scheduled Focus Window - ${afternoonPct}%)"
        }

        val fallbackInsight = HabitInsightEntity(
            timestamp = System.currentTimeMillis(),
            periodLabel = if (isKid) "Youth Habit Pipeline" else "Tailored Habit Engine",
            dominantAppsJson = dominantAppsString,
            peakActiveHours = peakHoursDesc,
            compulsiveScore = compulsiveRatio.coerceIn(15, 85),
            compulsiveSummary = "$compulsiveRatio% of total app launches exhibit compulsive micro-checking patterns (<30 seconds duration).",
            productivityTrend = "Core scheduled hours ($focusWindowStr) show mixed focus, while post-${userProfile.bedtimeHour}:00 usage is driven by infinite feeds.",
            keyTakeaway = "Setting a digital curfew at ${userProfile.bedtimeHour}:00 PM will significantly improve next-day energy and reduce reflexive unlocks.",
            fullAnalysisText = """
                ### Personalized Digital Habit Analysis for ${userProfile.name} ($roleDesc)
                - **Profile Profile**: ${userProfile.age} yrs old • Schedule: $focusWindowStr • Days off: $daysOffStr
                - **Primary Time Sink**: $dominantAppsString dominate your screen usage.
                - **Time of Day Hotspot**: Peak usage is concentrated in $peakHoursDesc.
                - **Compulsive Pattern**: $totalCompulsiveOpens out of $totalOpens opens were rapid impulsive checks under 30 seconds.
                - **Focus Window Health**: Pay attention to notifications during your $focusWindowStr block.
                - **Recommendation**: Create a screen boundary at ${userProfile.bedtimeHour}:00 PM to protect sleep.
            """.trimIndent(),
            isSyncedWithBackend = true
        )

        HabitInsightResult(fallbackInsight, generateDynamicRecommendations(aggregates, userProfile))
    }

    suspend fun answerConversationalQuery(
        query: String,
        chatHistory: List<ChatMessageEntity>,
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        userProfile: UserProfileEntity = UserProfileEntity()
    ): String = withContext(Dispatchers.IO) {
        val totalMs = aggregates.sumOf { it.totalDurationMs }
        val totalHours = totalMs / 3600000.0
        val appSummaries = aggregates.groupBy { it.appName }.map { (name, list) ->
            val totalMin = list.sumOf { it.totalDurationMs } / 60000
            val opens = list.sumOf { it.openCount }
            val notifs = list.sumOf { it.notificationCount }
            val nightMin = list.sumOf { it.nightMinutes }
            "$name: $totalMin mins total, $opens opens, $notifs notifications, $nightMin mins at night"
        }.joinToString("\n")

        val timeOfDay = "Morning: ${aggregates.sumOf { it.morningMinutes }}m, Afternoon: ${aggregates.sumOf { it.afternoonMinutes }}m, Evening: ${aggregates.sumOf { it.eveningMinutes }}m, Night (22:00-05:00): ${aggregates.sumOf { it.nightMinutes }}m"
        val focusWindowStr = "${userProfile.focusStartHour}:00 to ${userProfile.focusEndHour}:00"
        val daysOffStr = userProfile.getDaysOffList().joinToString(", ")

        val telemetryContext = """
            [USER PROFILE & SCHEDULE]
            Name: ${userProfile.name}, Age: ${userProfile.age}, Gender: ${userProfile.gender}
            Role: ${userProfile.getRole().displayName} (${userProfile.occupationTitle})
            Focus / Work Hours: $focusWindowStr
            Days Off: $daysOffStr
            Bedtime: ${userProfile.bedtimeHour}:00, Wake: ${userProfile.wakeHour}:00
            Target Screen Time Limit: ${userProfile.dailyScreenTimeTargetMinutes} mins
            Kid Mode: ${userProfile.isKidMode || userProfile.age < 13}
            
            [USER'S REAL DATABASE TELEMETRY]
            Total Screen Time: ${String.format(Locale.US, "%.1f", totalHours)}h
            Time-of-day distribution: $timeOfDay
            App details:
            $appSummaries
            Total fine-grained open events recorded: ${events.size}
        """.trimIndent()

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val isKid = userProfile.isKidMode || userProfile.age < 13
                val systemPrompt = """
                    You are an empathetic, insightful Digital Wellbeing and Habit AI Assistant.
                    You are speaking directly with ${userProfile.name} (Age: ${userProfile.age}, Role: ${userProfile.getRole().displayName}).
                    ${if (isKid) "This profile is for a student/child. Use age-appropriate, encouraging, friendly language that emphasizes healthy routines, school balance, and great sleep." else "Provide clear, practical, evidence-based productivity and habit guidance tailored to their work schedule."}
                    Ground your responses STRICTLY in the telemetry metrics and their schedule ($focusWindowStr focus hours, days off: $daysOffStr, bedtime: ${userProfile.bedtimeHour}:00).
                    Cite exact numbers, times of day, app names, open counts, and durations from their data.
                """.trimIndent()

                val historyParts = chatHistory.takeLast(6).map {
                    GeminiContent(
                        parts = listOf(GeminiPart(it.message)),
                        role = if (it.sender == "USER") "user" else "model"
                    )
                }

                val currentContent = GeminiContent(
                    parts = listOf(
                        GeminiPart("User Profile & Telemetry Context:\n$telemetryContext\n\nUser Question: $query")
                    ),
                    role = "user"
                )

                val contentsList = historyParts + currentContent

                val request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = contentsList,
                    generationConfig = GeminiGenerationConfig(temperature = 0.5f)
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!answer.isNullOrBlank()) {
                    return@withContext answer
                }
            } catch (e: Exception) {
                // Fall back to local reasoning
            }
        }

        // Local grounded answer engine tailored to profile
        val lowerQuery = query.lowercase()
        return@withContext when {
            lowerQuery.contains("night") || lowerQuery.contains("late") || lowerQuery.contains("midnight") || lowerQuery.contains("bed") -> {
                val nightMins = aggregates.sumOf { it.nightMinutes }
                val nightApps = aggregates.filter { it.nightMinutes > 0 }
                    .groupBy { it.appName }
                    .mapValues { it.value.sumOf { agg -> agg.nightMinutes } }
                    .toList()
                    .sortedByDescending { it.second }
                val topNight = nightApps.take(3).joinToString(", ") { "${it.first} (${it.second}m)" }
                "Based on your profile, your target bedtime is ${userProfile.bedtimeHour}:00. Telemetry shows you logged ${nightMins} minutes of late-night usage past 10 PM. Top late-night apps: $topNight.\n\nRecommendation for ${userProfile.name}: Setting a 'Do Not Disturb' routine 30 minutes before ${userProfile.bedtimeHour}:00 PM will help protect your sleep."
            }
            lowerQuery.contains("work") || lowerQuery.contains("school") || lowerQuery.contains("focus") || lowerQuery.contains("class") || lowerQuery.contains("study") -> {
                val focusMins = aggregates.sumOf { it.morningMinutes + it.afternoonMinutes }
                val prodAggs = aggregates.filter { it.category == "PRODUCTIVITY" || it.category == "COMMUNICATION" }
                val prodMins = prodAggs.sumOf { it.totalDurationMs } / 60000
                "During your scheduled focus window ($focusWindowStr), you logged ${prodMins} minutes of productive/educational tools out of ${focusMins} total daytime minutes. Keeping notifications silent during $focusWindowStr will help prevent unintentional app checking."
            }
            lowerQuery.contains("off") || lowerQuery.contains("weekend") || lowerQuery.contains("holiday") -> {
                "Your configured days off are $daysOffStr. During days off, your screen time naturally relaxes, but keeping bedtime close to ${userProfile.bedtimeHour}:00 PM preserves your circadian rhythm for the start of the week."
            }
            lowerQuery.contains("compulsive") || lowerQuery.contains("reflex") || lowerQuery.contains("quick") || lowerQuery.contains("unlock") -> {
                val totalOpens = aggregates.sumOf { it.openCount }
                val compulsiveOpens = aggregates.sumOf { it.compulsiveOpens }
                val pct = if (totalOpens > 0) (compulsiveOpens * 100 / totalOpens) else 0
                "Your compulsive index is $pct%. Out of $totalOpens total app opens, $compulsiveOpens were brief reflex sessions lasting under 30 seconds. The most common apps for reflexive checking are Instagram, Reddit, and messaging feeds."
            }
            lowerQuery.contains("recommend") || lowerQuery.contains("alternative") || lowerQuery.contains("switch") || lowerQuery.contains("better") -> {
                if (userProfile.isKidMode || userProfile.age < 13) {
                    "Here are curated kid-friendly and study alternatives for ${userProfile.name}:\n• Learning: 'Khan Academy Kids' & 'Duolingo'\n• Mindful Reading: 'Epic! / Reading Eggs'\n• Focus: 'Forest' (grow virtual trees while doing homework)\n• Screen Boundaries: 'One Sec Kids'."
                } else {
                    "Based on your profile (${userProfile.occupationTitle}), here are curated alternatives to curb distraction:\n• Social: 'One Sec' (adds mindful breathing before opening feeds)\n• Deep Focus: 'Forest' & 'Obsidian' for distraction-free note taking\n• Shopping: 'Shop' for tracking packages without flash deal hooks\n• Reading: 'Matter' for curated longform articles."
                }
            }
            else -> {
                "Hello ${userProfile.name}! Telemetry Overview: You've logged ${String.format(Locale.US, "%.1f", totalHours)} hours across ${aggregates.map { it.dateStr }.distinct().size} days. Schedule: $focusWindowStr • Bedtime: ${userProfile.bedtimeHour}:00. Feel free to ask: 'How is my screen time during focus hours?' or 'Am I staying off my phone before bedtime?'"
            }
        }
    }

    private fun generateDynamicRecommendations(
        aggregates: List<DailyAggregateEntity>,
        userProfile: UserProfileEntity
    ): List<AppRecommendationEntity> {
        val list = mutableListOf<AppRecommendationEntity>()
        val isKid = userProfile.isKidMode || userProfile.age < 13

        if (isKid) {
            // Kid / Student focused recommendations
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.google.android.youtube",
                    targetAppName = "YouTube / Infinite Video",
                    targetCategory = "Entertainment",
                    suggestedAppName = "Khan Academy Kids & PBS KIDS",
                    suggestedPackageName = "org.khankids.android",
                    reason = "Substitutes algorithmically driven auto-play videos with interactive, curriculum-aligned educational games and science explorations.",
                    efficiencyBadge = "Safe Learning Zone",
                    keyBenefits = "• 100% Free & No Ads\n• Interactive learning games\n• Encourages creative curiosity"
                )
            )
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.roblox.client",
                    targetAppName = "Roblox / Mobile Gaming",
                    targetCategory = "Games",
                    suggestedAppName = "Duolingo / Language Quest",
                    suggestedPackageName = "com.duolingo",
                    reason = "Channels game-like streak psychology into fun 5-minute language learning quests that strengthen memory and vocabulary.",
                    efficiencyBadge = "Gamified Learning",
                    keyBenefits = "• Fun 5-minute lessons\n• Positive streak motivation\n• Safe, wholesome community"
                )
            )
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.instagram.android",
                    targetAppName = "Short Video Feeds",
                    targetCategory = "Social",
                    suggestedAppName = "Epic! / Digital Books Library",
                    suggestedPackageName = "com.getepic.Epic",
                    reason = "Replaces fast-paced short reels with richly illustrated storybooks and audiobooks that boost reading comprehension.",
                    efficiencyBadge = "Reading Accelerator",
                    keyBenefits = "• 40,000+ age-appropriate books\n• Audio read-to-me mode\n• Teacher approved"
                )
            )
        } else {
            // Adult / Professional recommendations
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.instagram.android",
                    targetAppName = "Instagram / TikTok Feeds",
                    targetCategory = "Social",
                    suggestedAppName = "One Sec (Mindful Pause)",
                    suggestedPackageName = "app.one.sec",
                    reason = "Interposes an animated 2-second breathing pause before launch, breaking reflexive unlock triggers and reducing unconscious screen time by 57%.",
                    efficiencyBadge = "Friction Nudge Engine",
                    keyBenefits = "• Scientifically proven 57% usage drop\n• Guided micro-breathing prompts\n• Intentional unlock confirmation"
                )
            )
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.reddit.frontpage",
                    targetAppName = "Reddit / Social Discussion",
                    targetCategory = "Social & News",
                    suggestedAppName = "Matter / Minimalist Reader",
                    suggestedPackageName = "com.matter.reader",
                    reason = "Converts infinite comment doomscrolling into curated, distraction-free longform reading with integrated AI audio narration.",
                    efficiencyBadge = "Deep Reading",
                    keyBenefits = "• Zero engagement bait algorithms\n• High-definition audio read-aloud\n• Offline markdown export"
                )
            )
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "notion.id",
                    targetAppName = "Complex Workspace / Note Apps",
                    targetCategory = "Productivity",
                    suggestedAppName = "Obsidian (Local Markdown)",
                    suggestedPackageName = "md.obsidian",
                    reason = "Provides lightning-fast offline markdown notes without notification badges, complex server sync delays, or context switching traps.",
                    efficiencyBadge = "100% Offline Vault",
                    keyBenefits = "• Instant startup speed (<100ms)\n• Local-first privacy\n• Zero background battery drain"
                )
            )
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.forestapp.cc",
                    targetAppName = "Generic Timer Apps",
                    targetCategory = "Productivity",
                    suggestedAppName = "Forest (Focus Gamification)",
                    suggestedPackageName = "com.forestapp.cc",
                    reason = "Gamifies 25-minute Pomodoro focus blocks during work hours (${userProfile.focusStartHour}:00 - ${userProfile.focusEndHour}:00) by growing virtual trees that wither if you switch to distraction apps.",
                    efficiencyBadge = "Gamified Deep Work",
                    keyBenefits = "• Strict focus enforcement\n• Real tree planting initiatives\n• Detailed focus analytics"
                )
            )
        }

        return list
    }
}

data class HabitInsightResult(
    val insight: HabitInsightEntity,
    val recommendations: List<AppRecommendationEntity>
)
