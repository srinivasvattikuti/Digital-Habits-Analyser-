package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AppCategory
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

        // Real App duration rankings from device data
        val appDurations = aggregates.groupBy { it.appName }
            .mapValues { entry ->
                Triple(
                    entry.value.first().packageName,
                    entry.value.first().category,
                    entry.value.sumOf { it.totalDurationMs }
                )
            }
            .toList()
            .sortedByDescending { it.second.third }

        val dominantAppsString = if (appDurations.isNotEmpty()) {
            appDurations.take(5).joinToString(", ") {
                val pct = if (totalMs > 0) (it.second.third * 100 / totalMs).toInt() else 0
                "${it.first} (${pct}%)"
            }
        } else {
            "No device usage data recorded yet"
        }

        // Category breakdown
        val categoryBreakdown = aggregates.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalDurationMs } / 60000 }

        val focusWindowStr = "${userProfile.focusStartHour}:00 to ${userProfile.focusEndHour}:00"
        val daysOffStr = userProfile.getDaysOffList().joinToString(", ")
        val roleDesc = userProfile.getRole().displayName

        val contextPayload = buildString {
            appendLine("=== USER PROFILE & SCHEDULE ===")
            appendLine("Name: ${userProfile.name}")
            appendLine("Age: ${userProfile.age} | Gender: ${userProfile.gender}")
            appendLine("Role: $roleDesc (${userProfile.occupationTitle})")
            appendLine("Focus Hours: $focusWindowStr")
            appendLine("Days Off: $daysOffStr")
            appendLine("Bedtime: ${userProfile.bedtimeHour}:00 | Wake: ${userProfile.wakeHour}:00")
            appendLine("Daily Target Screen Time: ${userProfile.dailyScreenTimeTargetMinutes} mins")
            appendLine("Child / Supervised Mode: ${if (userProfile.isKidMode || userProfile.age < 13) "YES" else "NO"}")
            appendLine()
            appendLine("=== REAL DEVICE TELEMETRY FROM DATABASE ===")
            appendLine("Total Screen Time: ${String.format(Locale.US, "%.1f", totalHours)} hours across ${aggregates.map { it.dateStr }.distinct().size} days")
            appendLine("Total App Launches: $totalOpens (Compulsive quick checks: $totalCompulsiveOpens, $compulsiveRatio%)")
            appendLine("User's Real Top Apps: $dominantAppsString")
            appendLine("Time of Day Distribution:")
            appendLine("- Morning (05:00-12:00): $morningMins min ($morningPct%)")
            appendLine("- Afternoon (12:00-17:00): $afternoonMins min ($afternoonPct%)")
            appendLine("- Evening (17:00-22:00): $eveningMins min ($eveningPct%)")
            appendLine("- Late Night (22:00-05:00): $nightMins min ($nightPct%)")
            appendLine("Category Breakdown (Minutes): $categoryBreakdown")
            appendLine("Real Device Open Events: ${events.size}")
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are the AI Behavioral Engine of a Digital Habits Tracker.
                    Analyze this real device usage telemetry tailored to the user's specific age, role, and focus schedule ($focusWindowStr).
                    Base all conclusions strictly on their real installed apps and recorded usage.
                    Do not invent apps the user does not use.
                    Provide clear, constructive habit analysis with markdown headings.
                """.trimIndent()

                val request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart("Analyze this real user telemetry:\n\n$contextPayload\n\nProvide habit synthesis and insights.")
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
                    val topAppName = appDurations.firstOrNull()?.first ?: "your primary apps"
                    val insightEntity = HabitInsightEntity(
                        timestamp = System.currentTimeMillis(),
                        periodLabel = if (isKid) "Kid-Friendly AI Synthesis" else "Personalized AI Pipeline",
                        dominantAppsJson = dominantAppsString,
                        peakActiveHours = peakHoursDesc,
                        compulsiveScore = compulsiveRatio.coerceIn(10, 95),
                        compulsiveSummary = "$compulsiveRatio% of launches are micro-checks (<30s), primarily concentrated during $peakHoursDesc.",
                        productivityTrend = if ((categoryBreakdown["PRODUCTIVITY"] ?: 0) > 45) {
                            "Focus hours ($focusWindowStr) remain well-protected with solid productivity."
                        } else {
                            "Recreational app browsing ($topAppName) is active during your scheduled $focusWindowStr focus window."
                        },
                        keyTakeaway = if (isKid) {
                            "Keep notifications quiet during school hours ($focusWindowStr) and put the phone to charge 30 minutes before bedtime (${userProfile.bedtimeHour}:00 PM)."
                        } else {
                            "Limiting $topAppName 45 minutes before bedtime (${userProfile.bedtimeHour}:00) prevents reflexive scrolling and promotes deep recovery."
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
            else -> "$focusWindowStr (Scheduled Focus Window - ${afternoonPct}%)"
        }

        val topAppName = appDurations.firstOrNull()?.first ?: "Device Apps"
        val fallbackInsight = HabitInsightEntity(
            timestamp = System.currentTimeMillis(),
            periodLabel = if (isKid) "Youth Habit Pipeline" else "Tailored Habit Engine",
            dominantAppsJson = dominantAppsString,
            peakActiveHours = peakHoursDesc,
            compulsiveScore = compulsiveRatio.coerceIn(15, 85),
            compulsiveSummary = "$compulsiveRatio% of app launches on your device exhibit rapid reflex checking patterns (<30 seconds duration).",
            productivityTrend = "Recorded usage indicates primary engagement with $dominantAppsString.",
            keyTakeaway = "Setting a screen wind-down 30 minutes before ${userProfile.bedtimeHour}:00 PM will reduce reflexive checking and improve sleep quality.",
            fullAnalysisText = """
                ### Personalized Digital Habit Analysis for ${userProfile.name} ($roleDesc)
                - **Profile**: ${userProfile.age} yrs old • Scheduled Focus: $focusWindowStr • Days off: $daysOffStr
                - **Top Real Apps**: $dominantAppsString
                - **Active Usage Peak**: Peak phone interaction occurs during $peakHoursDesc.
                - **Compulsive Ratio**: $totalCompulsiveOpens out of $totalOpens total app launches were quick checks under 30 seconds.
                - **Focus Window Health**: Monitor distractions during your $focusWindowStr block.
                - **Recommendation**: Create a digital curfew at ${userProfile.bedtimeHour}:00 PM to protect evening rest.
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
            
            [USER'S REAL PHONE TELEMETRY]
            Total Screen Time: ${String.format(Locale.US, "%.1f", totalHours)}h
            Time-of-day distribution: $timeOfDay
            Apps recorded on phone:
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
                    ${if (isKid) "This profile is for a student/child. Use friendly, supportive language focusing on healthy routines, school balance, and great sleep." else "Provide clear, practical, evidence-based productivity and habit guidance tailored to their work schedule."}
                    Ground your responses STRICTLY in their real phone telemetry ($appSummaries) and schedule ($focusWindowStr focus hours, bedtime: ${userProfile.bedtimeHour}:00).
                    Cite real app names, open counts, and minutes from their recorded phone usage.
                """.trimIndent()

                val historyParts = chatHistory.takeLast(6).map {
                    GeminiContent(
                        parts = listOf(GeminiPart(it.message)),
                        role = if (it.sender == "USER") "user" else "model"
                    )
                }

                val currentContent = GeminiContent(
                    parts = listOf(
                        GeminiPart("Real Phone Telemetry Context:\n$telemetryContext\n\nUser Question: $query")
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

        // Local grounded answer engine tailored to real data
        val lowerQuery = query.lowercase()
        return@withContext when {
            lowerQuery.contains("night") || lowerQuery.contains("late") || lowerQuery.contains("midnight") || lowerQuery.contains("bed") -> {
                val nightMins = aggregates.sumOf { it.nightMinutes }
                val nightApps = aggregates.filter { it.nightMinutes > 0 }
                    .groupBy { it.appName }
                    .mapValues { it.value.sumOf { agg -> agg.nightMinutes } }
                    .toList()
                    .sortedByDescending { it.second }
                val topNight = if (nightApps.isNotEmpty()) {
                    nightApps.take(3).joinToString(", ") { "${it.first} (${it.second}m)" }
                } else {
                    "No late-night apps detected"
                }
                "Based on your profile, your target bedtime is ${userProfile.bedtimeHour}:00. Real phone telemetry shows ${nightMins} minutes of usage past 10 PM. Top late-night apps: $topNight.\n\nRecommendation: Enable 'Do Not Disturb' 30 minutes before ${userProfile.bedtimeHour}:00 PM to help protect your sleep cycle."
            }
            lowerQuery.contains("work") || lowerQuery.contains("school") || lowerQuery.contains("focus") || lowerQuery.contains("class") || lowerQuery.contains("study") -> {
                val focusMins = aggregates.sumOf { it.morningMinutes + it.afternoonMinutes }
                val prodAggs = aggregates.filter { it.category == "PRODUCTIVITY" || it.category == "COMMUNICATION" }
                val prodMins = prodAggs.sumOf { it.totalDurationMs } / 60000
                "During your scheduled focus window ($focusWindowStr), you logged ${prodMins} minutes of productive apps out of ${focusMins} total daytime minutes on your device."
            }
            lowerQuery.contains("off") || lowerQuery.contains("weekend") || lowerQuery.contains("holiday") -> {
                "Your configured days off are $daysOffStr. During days off, your screen time naturally increases, but keeping bedtime around ${userProfile.bedtimeHour}:00 PM preserves your circadian rhythm."
            }
            lowerQuery.contains("compulsive") || lowerQuery.contains("reflex") || lowerQuery.contains("quick") || lowerQuery.contains("unlock") -> {
                val totalOpens = aggregates.sumOf { it.openCount }
                val compulsiveOpens = aggregates.sumOf { it.compulsiveOpens }
                val pct = if (totalOpens > 0) (compulsiveOpens * 100 / totalOpens) else 0
                val topCompulsiveApp = aggregates.groupBy { it.appName }.maxByOrNull { it.value.sumOf { agg -> agg.compulsiveOpens } }?.key ?: "your main apps"
                "Your compulsive checking index is $pct%. Out of $totalOpens total app opens, $compulsiveOpens were brief reflex checks under 30 seconds. The most frequent reflex-checked app is $topCompulsiveApp."
            }
            lowerQuery.contains("recommend") || lowerQuery.contains("alternative") || lowerQuery.contains("switch") || lowerQuery.contains("better") -> {
                val topApps = aggregates.groupBy { it.appName }
                    .mapValues { it.value.sumOf { agg -> agg.totalDurationMs } }
                    .toList().sortedByDescending { it.second }.take(3).map { it.first }
                val topAppsStr = if (topApps.isNotEmpty()) topApps.joinToString(", ") else "your top apps"
                "Based on your real device usage ($topAppsStr), consider adding friction pauses (e.g. 'One Sec' or daily app limits) to prevent unconscious looping during focus hours ($focusWindowStr)."
            }
            else -> {
                val topAppNames = aggregates.groupBy { it.appName }.keys.take(3).joinToString(", ")
                "Hello ${userProfile.name}! Telemetry Overview: You've logged ${String.format(Locale.US, "%.1f", totalHours)} hours on your device (top apps: ${if (topAppNames.isNotEmpty()) topAppNames else "recorded device apps"}). Focus Hours: $focusWindowStr • Bedtime: ${userProfile.bedtimeHour}:00. Ask: 'What are my most used apps during focus hours?' or 'How is my late night phone usage?'"
            }
        }
    }

    private fun generateDynamicRecommendations(
        aggregates: List<DailyAggregateEntity>,
        userProfile: UserProfileEntity
    ): List<AppRecommendationEntity> {
        val list = mutableListOf<AppRecommendationEntity>()
        val isKid = userProfile.isKidMode || userProfile.age < 13

        // Group actual apps on user's phone by usage duration
        val topAppsOnDevice = aggregates.groupBy { it.packageName }
            .map { (pkg, aggs) ->
                val appName = aggs.first().appName
                val category = aggs.first().category
                val totalMs = aggs.sumOf { it.totalDurationMs }
                val totalMins = (totalMs / 60000).toInt()
                val compulsiveOpens = aggs.sumOf { it.compulsiveOpens }
                val nightMins = aggs.sumOf { it.nightMinutes }
                AppUsageRecord(pkg, appName, category, totalMins, compulsiveOpens, nightMins)
            }
            .filter { it.totalMinutes > 0 }
            .sortedByDescending { it.totalMinutes }

        // Generate tailored recommendations for the user's ACTUAL top apps
        for (app in topAppsOnDevice.take(5)) {
            val rec = createRecommendationForRealApp(app, isKid, userProfile)
            if (rec != null) {
                list.add(rec)
            }
        }

        // If no aggregates or few items, add general mindful habits based on user role
        if (list.isEmpty()) {
            list.add(
                AppRecommendationEntity(
                    targetPackageName = "com.android.system",
                    targetAppName = "Daily Screen Time",
                    targetCategory = "Productivity",
                    suggestedAppName = "Focus Mode & App Timers",
                    suggestedPackageName = "com.google.android.apps.wellbeing",
                    reason = "Set daily target screen limits (${userProfile.dailyScreenTimeTargetMinutes} mins) to maintain focus during ${userProfile.focusStartHour}:00 - ${userProfile.focusEndHour}:00.",
                    efficiencyBadge = "Focus Target",
                    keyBenefits = "• Limits distraction during focus window\n• Auto-pauses notifications\n• Preserves healthy sleep hygiene"
                )
            )
        }

        return list
    }

    private fun createRecommendationForRealApp(
        app: AppUsageRecord,
        isKid: Boolean,
        userProfile: UserProfileEntity
    ): AppRecommendationEntity? {
        val cat = try { AppCategory.valueOf(app.category) } catch (e: Exception) { AppCategory.OTHER }

        return when {
            cat == AppCategory.SOCIAL || app.appName.contains("Instagram", ignoreCase = true) ||
                    app.appName.contains("TikTok", ignoreCase = true) || app.appName.contains("Twitter", ignoreCase = true) ||
                    app.appName.contains("Facebook", ignoreCase = true) || app.appName.contains("Snapchat", ignoreCase = true) -> {
                if (isKid) {
                    AppRecommendationEntity(
                        targetPackageName = app.packageName,
                        targetAppName = "${app.appName} (${app.totalMinutes}m)",
                        targetCategory = "Social",
                        suggestedAppName = "Epic! Storybooks & Duolingo",
                        suggestedPackageName = "com.getepic.Epic",
                        reason = "Channels ${app.totalMinutes} minutes of social browsing into engaging storybooks and language games with positive rewards.",
                        efficiencyBadge = "Educational Upgrade",
                        keyBenefits = "• Curriculum-safe content\n• Gamified reading streaks\n• No social media algorithms"
                    )
                } else {
                    AppRecommendationEntity(
                        targetPackageName = app.packageName,
                        targetAppName = "${app.appName} (${app.totalMinutes}m)",
                        targetCategory = "Social",
                        suggestedAppName = "One Sec (Mindful Pause Interceptor)",
                        suggestedPackageName = "app.one.sec",
                        reason = "Adds an intentional 2-second breathing pause before opening ${app.appName}, curbing ${app.compulsiveOpens} impulsive reflex checks.",
                        efficiencyBadge = "Friction Engine",
                        keyBenefits = "• Proven 57% screen time reduction\n• Breaks subconscious opening loops\n• Restores deep work flow"
                    )
                }
            }

            cat == AppCategory.ENTERTAINMENT || app.appName.contains("YouTube", ignoreCase = true) ||
                    app.appName.contains("Netflix", ignoreCase = true) || app.appName.contains("Twitch", ignoreCase = true) -> {
                if (isKid) {
                    AppRecommendationEntity(
                        targetPackageName = app.packageName,
                        targetAppName = "${app.appName} (${app.totalMinutes}m)",
                        targetCategory = "Entertainment",
                        suggestedAppName = "Khan Academy Kids & PBS KIDS",
                        suggestedPackageName = "org.khankids.android",
                        reason = "Replaces autoplay video algorithms on ${app.appName} with interactive, educational learning adventures.",
                        efficiencyBadge = "Safe Learning",
                        keyBenefits = "• 100% Free & No Ads\n• Interactive science & math\n• Safe curated library"
                    )
                } else {
                    AppRecommendationEntity(
                        targetPackageName = app.packageName,
                        targetAppName = "${app.appName} (${app.totalMinutes}m)",
                        targetCategory = "Entertainment",
                        suggestedAppName = "Matter / Pocket (Curated Longform)",
                        suggestedPackageName = "com.matter.reader",
                        reason = "Converts ${app.totalMinutes} mins of video consumption into curated high-signal articles with audio narration.",
                        efficiencyBadge = "High Signal Reading",
                        keyBenefits = "• Zero engagement bait\n• Audio read-aloud mode\n• Distraction-free typography"
                    )
                }
            }

            cat == AppCategory.GAMES -> {
                AppRecommendationEntity(
                    targetPackageName = app.packageName,
                    targetAppName = "${app.appName} (${app.totalMinutes}m)",
                    targetCategory = "Gaming",
                    suggestedAppName = "Elevate / Peak (Brain Training)",
                    suggestedPackageName = "com.wonder.productivity",
                    reason = "Directs gaming streak motivation from ${app.appName} into cognitive focus, memory, and math training.",
                    efficiencyBadge = "Cognitive Skill Builder",
                    keyBenefits = "• 40+ brain training games\n• Focus & memory metrics\n• 5-minute daily challenges"
                )
            }

            app.nightMins > 20 -> {
                AppRecommendationEntity(
                    targetPackageName = app.packageName,
                    targetAppName = "${app.appName} (${app.nightMins}m late-night)",
                    targetCategory = "Late-Night Screen Time",
                    suggestedAppName = "Bedtime Mode & Digital Curfew",
                    suggestedPackageName = "com.google.android.apps.wellbeing",
                    reason = "You spent ${app.nightMins} mins on ${app.appName} late at night. Setting a curfew at ${userProfile.bedtimeHour}:00 PM protects restorative sleep.",
                    efficiencyBadge = "Sleep Protection",
                    keyBenefits = "• Grayscale display at bedtime\n• Silences late notifications\n• Improves REM sleep recovery"
                )
            }

            else -> {
                AppRecommendationEntity(
                    targetPackageName = app.packageName,
                    targetAppName = "${app.appName} (${app.totalMinutes}m)",
                    targetCategory = app.category,
                    suggestedAppName = "Forest (Focus Gamification)",
                    suggestedPackageName = "com.forestapp.cc",
                    reason = "Keep ${app.appName} usage balanced by setting 25-minute Pomodoro sessions during focus hours (${userProfile.focusStartHour}:00 - ${userProfile.focusEndHour}:00).",
                    efficiencyBadge = "Focus Enforcement",
                    keyBenefits = "• Protects work blocks\n• Gamified virtual tree growth\n• Detailed session analytics"
                )
            }
        }
    }

    private data class AppUsageRecord(
        val packageName: String,
        val appName: String,
        val category: String,
        val totalMinutes: Int,
        val compulsiveOpens: Int,
        val nightMins: Int
    )
}

data class HabitInsightResult(
    val insight: HabitInsightEntity,
    val recommendations: List<AppRecommendationEntity>
)
