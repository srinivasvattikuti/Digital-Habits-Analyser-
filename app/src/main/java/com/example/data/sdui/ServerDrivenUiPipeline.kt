package com.example.data.sdui

import com.example.BuildConfig
import com.example.data.ai.GeminiClient
import com.example.data.ai.GeminiContent
import com.example.data.ai.GeminiGenerationConfig
import com.example.data.ai.GeminiPart
import com.example.data.ai.GeminiRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Server-Driven UI (SDUI) Customization Engine.
 * Translates user prompts and telemetry context into structured UI configurations via Gemini LLM.
 */
class ServerDrivenUiPipeline {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val layoutAdapter = moshi.adapter(DashboardLayoutConfig::class.java)

    /**
     * Unsupported native capabilities check.
     * Rejects requests to install binaries, alter OS permissions/root, or execute low-level code.
     */
    fun checkForUnsupportedNativeCapabilities(prompt: String): String? {
        val lower = prompt.lowercase()
        val forbiddenKeywords = listOf(
            "install apk", "download apk", "install binary", "compile code",
            "root permission", "grant root", "hack", "modify os",
            "bypass permission", "system permission without asking", "kernel",
            "execute shell", "run bash", "install native library", "load so library",
            "access private database of other app", "intercept banking password",
            "hardware override", "flash rom"
        )

        for (kw in forbiddenKeywords) {
            if (lower.contains(kw)) {
                return NATIVE_UPDATE_ERROR_MESSAGE
            }
        }
        return null
    }

    /**
     * Primary customization method.
     * Takes user prompt + context, queries Gemini with strict schema, and returns validated layout.
     */
    suspend fun customizeDashboard(
        request: DashboardCustomizeRequest
    ): DashboardCustomizeResponse = withContext(Dispatchers.IO) {
        // Step 1: Pre-flight security guardrail check
        val securityError = checkForUnsupportedNativeCapabilities(request.userPrompt)
        if (securityError != null) {
            return@withContext DashboardCustomizeResponse(
                success = false,
                layout = null,
                explanation = "Your request involves native OS-level modifications that cannot be performed via dynamic server-driven UI configuration.",
                errorMessage = securityError,
                requiresNativeUpdate = true
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Offline / Local Rule-Based Customizer
            return@withContext executeLocalRuleBasedCustomization(request)
        }

        try {
            val systemPrompt = buildSystemPrompt()
            val userContent = buildUserPromptContent(request)

            val geminiRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = userContent)), role = "user")
                ),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2f,
                    maxOutputTokens = 2048
                )
            )

            val response = GeminiClient.api.generateContent(apiKey, geminiRequest)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (candidateText.isNullOrBlank()) {
                return@withContext executeLocalRuleBasedCustomization(request)
            }

            parseAndValidateGeminiResponse(candidateText, request)
        } catch (e: Exception) {
            // Fallback gracefully to rule-based transformer
            val fallback = executeLocalRuleBasedCustomization(request)
            DashboardCustomizeResponse(
                success = fallback.success,
                layout = fallback.layout,
                explanation = "Customized layout based on your request: \"${request.userPrompt}\" (Local Pipeline)",
                errorMessage = null,
                requiresNativeUpdate = false
            )
        }
    }

    /**
     * Parses the LLM JSON response and maps to DashboardLayoutConfig.
     */
    private fun parseAndValidateGeminiResponse(
        rawResponse: String,
        request: DashboardCustomizeRequest
    ): DashboardCustomizeResponse {
        var cleanJson = rawResponse.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json").substringBeforeLast("```").trim()
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").substringBeforeLast("```").trim()
        }

        return try {
            val jsonObject = JSONObject(cleanJson)

            // Check if LLM flagged native capability requirement
            val requiresNative = jsonObject.optBoolean("requiresNativeUpdate", false)
            if (requiresNative) {
                return DashboardCustomizeResponse(
                    success = false,
                    layout = null,
                    explanation = jsonObject.optString("explanation", "Native capability required."),
                    errorMessage = NATIVE_UPDATE_ERROR_MESSAGE,
                    requiresNativeUpdate = true
                )
            }

            val explanation = jsonObject.optString("explanation", "Dashboard layout customized successfully.")
            val layoutJsonObj = jsonObject.optJSONObject("layout") ?: jsonObject

            val parsedLayout = layoutAdapter.fromJson(layoutJsonObj.toString())
                ?: return executeLocalRuleBasedCustomization(request)

            // Ensure valid component ordering and unique IDs
            val sanitizedComponents = parsedLayout.components.mapIndexed { idx, comp ->
                comp.copy(position = idx + 1)
            }

            val finalLayout = parsedLayout.copy(
                userId = request.userId,
                generatedFromPrompt = request.userPrompt,
                components = sanitizedComponents,
                timestamp = System.currentTimeMillis()
            )

            DashboardCustomizeResponse(
                success = true,
                layout = finalLayout,
                explanation = explanation,
                errorMessage = null,
                requiresNativeUpdate = false
            )
        } catch (e: Exception) {
            executeLocalRuleBasedCustomization(request)
        }
    }

    /**
     * Rule-based engine that reliably transforms layouts when offline or as robust fallback.
     */
    fun executeLocalRuleBasedCustomization(
        request: DashboardCustomizeRequest
    ): DashboardCustomizeResponse {
        val prompt = request.userPrompt.lowercase()

        val baseLayout = request.currentLayout ?: getDefaultLayout(request.userId)
        val components = baseLayout.components.toMutableList()

        var layoutName = "Customized Layout"
        var explanation = "Adjusted your dashboard based on your prompt."
        var themeColor = baseLayout.themeColor
        var density = baseLayout.density
        var headerTitle = baseLayout.headerTitle
        var headerSubtitle = baseLayout.headerSubtitle

        // Prompt Analysis
        val wantsScreenTimeTop = prompt.contains("screen time at the top") || prompt.contains("usage first") || prompt.contains("screen time first")
        val wantsHideNotifications = prompt.contains("hide notification") || prompt.contains("no notifications") || prompt.contains("hide alerts")
        val wantsSimplify = prompt.contains("simplify") || prompt.contains("minimal") || prompt.contains("clean") || prompt.contains("less clutter")
        val wantsSleepFocus = prompt.contains("sleep") || prompt.contains("bedtime") || prompt.contains("night") || prompt.contains("doomscroll")
        val wantsFocusMode = prompt.contains("focus") || prompt.contains("work") || prompt.contains("productivity") || prompt.contains("study")
        val wantsGoalsTop = prompt.contains("goals at the top") || prompt.contains("goal first") || prompt.contains("limits first")
        val wantsDarkTheme = prompt.contains("dark") || prompt.contains("midnight") || prompt.contains("black")
        val wantsEmeraldTheme = prompt.contains("green") || prompt.contains("emerald") || prompt.contains("teal")
        val wantsIndigoTheme = prompt.contains("indigo") || prompt.contains("blue") || prompt.contains("purple")

        if (wantsDarkTheme) themeColor = "MIDNIGHT"
        if (wantsEmeraldTheme) themeColor = "EMERALD"
        if (wantsIndigoTheme) themeColor = "INDIGO"

        if (wantsSimplify) {
            density = "COMPACT"
            layoutName = "Simplified Focus View"
            explanation = "Simplified layout to focus on core metrics and reduce visual noise."
        }

        // Adjust visibility & positions
        val updatedComponents = components.map { comp ->
            when (comp.type) {
                CardType.SUMMARY_METRICS -> {
                    if (wantsHideNotifications) {
                        comp.copy(
                            parameters = comp.parameters.copy(
                                showNotifications = false,
                                layoutStyle = if (wantsSimplify) "COMPACT_LIST" else comp.parameters.layoutStyle
                            )
                        )
                    } else comp
                }
                CardType.NOTIFICATION_LEADERBOARD -> {
                    if (wantsHideNotifications || wantsSimplify) {
                        comp.copy(visible = false)
                    } else comp
                }
                CardType.RADAR_DIMENSIONS, CardType.WEEK_OVER_WEEK, CardType.RECOMMENDATION_BANNER -> {
                    if (wantsSimplify) {
                        comp.copy(visible = false)
                    } else comp
                }
                CardType.BEHAVIOR_FORECAST -> {
                    if (wantsSleepFocus) {
                        comp.copy(visible = true, title = "Bedtime & Sleep Pacing Guard")
                    } else comp
                }
                CardType.HOURLY_HEATMAP -> {
                    if (wantsSleepFocus) {
                        comp.copy(visible = true, title = "Nighttime Activity Heatmap")
                    } else comp
                }
                else -> comp
            }
        }.toMutableList()

        // Reordering logic
        if (wantsScreenTimeTop) {
            val heroIndex = updatedComponents.indexOfFirst { it.type == CardType.HERO_USAGE }
            if (heroIndex >= 0) {
                val hero = updatedComponents.removeAt(heroIndex)
                updatedComponents.add(0, hero.copy(visible = true))
            }
            val metricsIndex = updatedComponents.indexOfFirst { it.type == CardType.SUMMARY_METRICS }
            if (metricsIndex >= 0) {
                val metrics = updatedComponents.removeAt(metricsIndex)
                updatedComponents.add(1, metrics.copy(visible = true))
            }
            explanation = "Prioritized total screen time and core metrics at the top of your dashboard."
        } else if (wantsGoalsTop) {
            val goalsIndex = updatedComponents.indexOfFirst { it.type == CardType.GOALS_TRACKER }
            if (goalsIndex >= 0) {
                val goals = updatedComponents.removeAt(goalsIndex)
                updatedComponents.add(0, goals.copy(visible = true))
            }
            explanation = "Moved Habit Goals & Daily Limits directly to the top."
        } else if (wantsSleepFocus) {
            val forecastIndex = updatedComponents.indexOfFirst { it.type == CardType.BEHAVIOR_FORECAST }
            if (forecastIndex >= 0) {
                val forecast = updatedComponents.removeAt(forecastIndex)
                updatedComponents.add(0, forecast.copy(visible = true))
            }
            val heatmapIndex = updatedComponents.indexOfFirst { it.type == CardType.HOURLY_HEATMAP }
            if (heatmapIndex >= 0) {
                val heatmap = updatedComponents.removeAt(heatmapIndex)
                updatedComponents.add(1, heatmap.copy(visible = true))
            }
            layoutName = "Sleep & Nighttime Guard"
            headerSubtitle = "BEDTIME OPTIMIZATION"
            explanation = "Reconfigured layout to emphasize nighttime usage patterns and doomscroll protection."
        } else if (wantsFocusMode) {
            val goalsIndex = updatedComponents.indexOfFirst { it.type == CardType.GOALS_TRACKER }
            if (goalsIndex >= 0) {
                val goals = updatedComponents.removeAt(goalsIndex)
                updatedComponents.add(0, goals.copy(visible = true))
            }
            val nudgesIndex = updatedComponents.indexOfFirst { it.type == CardType.PROACTIVE_NUDGES }
            if (nudgesIndex >= 0) {
                val nudges = updatedComponents.removeAt(nudgesIndex)
                updatedComponents.add(1, nudges.copy(visible = true))
            }
            layoutName = "Deep Focus Mode"
            headerSubtitle = "PRODUCTIVITY & ATTENTION"
            explanation = "Prioritized goals and focus nudges while trimming secondary metrics."
        }

        // Re-index positions
        val finalComponents = updatedComponents.mapIndexed { idx, c ->
            c.copy(position = idx + 1)
        }

        val resultLayout = DashboardLayoutConfig(
            layoutId = "custom_${System.currentTimeMillis()}",
            userId = request.userId,
            layoutName = layoutName,
            description = "Custom layout tailored for: ${request.userPrompt}",
            themeColor = themeColor,
            density = density,
            headerTitle = headerTitle,
            headerSubtitle = headerSubtitle,
            components = finalComponents,
            generatedFromPrompt = request.userPrompt,
            timestamp = System.currentTimeMillis()
        )

        return DashboardCustomizeResponse(
            success = true,
            layout = resultLayout,
            explanation = explanation,
            errorMessage = null,
            requiresNativeUpdate = false
        )
    }

    /**
     * Default holistic layout.
     */
    fun getDefaultLayout(userId: String = "current_user"): DashboardLayoutConfig {
        return DashboardLayoutConfig(
            layoutId = "default_layout",
            userId = userId,
            layoutName = "Standard Overview",
            description = "Holistic digital wellness dashboard featuring all primary modules.",
            themeColor = "DEFAULT",
            density = "COMFORTABLE",
            headerTitle = "Dashboard",
            headerSubtitle = "HABIT INSIGHTS",
            components = listOf(
                DynamicComponentConfig(
                    id = "hero_usage",
                    type = CardType.HERO_USAGE,
                    position = 1,
                    visible = true,
                    title = "Daily Usage",
                    cardStyle = "HIGHLIGHT"
                ),
                DynamicComponentConfig(
                    id = "summary_metrics",
                    type = CardType.SUMMARY_METRICS,
                    position = 2,
                    visible = true,
                    title = "Key Metrics",
                    parameters = ComponentParameters(
                        showScreenTime = true,
                        showOpens = true,
                        showCompulsiveRatio = true,
                        showNotifications = true,
                        layoutStyle = "GRID_2X2"
                    )
                ),
                DynamicComponentConfig(
                    id = "proactive_nudges",
                    type = CardType.PROACTIVE_NUDGES,
                    position = 3,
                    visible = true,
                    title = "Proactive Nudges"
                ),
                DynamicComponentConfig(
                    id = "weekly_trends",
                    type = CardType.WEEKLY_TRENDS,
                    position = 4,
                    visible = true,
                    title = "7-Day Usage Trends"
                ),
                DynamicComponentConfig(
                    id = "notif_leaderboard",
                    type = CardType.NOTIFICATION_LEADERBOARD,
                    position = 5,
                    visible = true,
                    title = "Interruption Leaderboard"
                ),
                DynamicComponentConfig(
                    id = "wow_comparison",
                    type = CardType.WEEK_OVER_WEEK,
                    position = 6,
                    visible = true,
                    title = "Week-Over-Week Delta"
                ),
                DynamicComponentConfig(
                    id = "goals_tracker",
                    type = CardType.GOALS_TRACKER,
                    position = 7,
                    visible = true,
                    title = "Habit Goals & Limits"
                ),
                DynamicComponentConfig(
                    id = "behavior_forecast",
                    type = CardType.BEHAVIOR_FORECAST,
                    position = 8,
                    visible = true,
                    title = "Behavior Forecast"
                ),
                DynamicComponentConfig(
                    id = "radar_dimensions",
                    type = CardType.RADAR_DIMENSIONS,
                    position = 9,
                    visible = true,
                    title = "5-Pillar Equilibrium"
                ),
                DynamicComponentConfig(
                    id = "hourly_heatmap",
                    type = CardType.HOURLY_HEATMAP,
                    position = 10,
                    visible = true,
                    title = "24-Hour Heatmap"
                ),
                DynamicComponentConfig(
                    id = "category_dist",
                    type = CardType.CATEGORY_DISTRIBUTION,
                    position = 11,
                    visible = true,
                    title = "Category Breakdown"
                ),
                DynamicComponentConfig(
                    id = "compulsive_gauge",
                    type = CardType.COMPULSIVE_GAUGE,
                    position = 12,
                    visible = true,
                    title = "Compulsive vs Intentional"
                ),
                DynamicComponentConfig(
                    id = "top_apps",
                    type = CardType.TOP_APPS,
                    position = 13,
                    visible = true,
                    title = "Top Dominating Apps",
                    parameters = ComponentParameters(maxAppsCount = 8)
                ),
                DynamicComponentConfig(
                    id = "usage_over_time",
                    type = CardType.USAGE_OVER_TIME,
                    position = 14,
                    visible = true,
                    title = "Historical Usage Over Time"
                ),
                DynamicComponentConfig(
                    id = "reco_banner",
                    type = CardType.RECOMMENDATION_BANNER,
                    position = 15,
                    visible = true,
                    title = "AI Suggestions"
                ),
                DynamicComponentConfig(
                    id = "research_habit_science",
                    type = CardType.RESEARCH_HABIT_SCIENCE,
                    position = 16,
                    visible = true,
                    title = "Behavioral Data Science & Entropy"
                ),
                DynamicComponentConfig(
                    id = "longitudinal_baseline",
                    type = CardType.LONGITUDINAL_BASELINE_COMPARISON,
                    position = 17,
                    visible = true,
                    title = "Longitudinal Baseline vs Recent"
                ),
                DynamicComponentConfig(
                    id = "circadian_sleep_impact",
                    type = CardType.CIRCADIAN_SLEEP_IMPACT,
                    position = 18,
                    visible = true,
                    title = "Circadian Pre-Bedtime Sleep Risk"
                ),
                DynamicComponentConfig(
                    id = "ai_incremental_memory",
                    type = CardType.AI_INCREMENTAL_MEMORY,
                    position = 19,
                    visible = true,
                    title = "Incremental AI Memory"
                )
            ),
            version = 1,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Specialized Presets
     */
    fun getPresetLayout(presetId: String, userId: String = "current_user"): DashboardLayoutConfig {
        val base = getDefaultLayout(userId)
        return when (presetId) {
            "MINIMAL_FOCUS" -> base.copy(
                layoutId = "preset_minimal_focus",
                layoutName = "Minimal Focus",
                description = "Distraction-free setup with screen time, goals, and behavioral pacing only.",
                themeColor = "INDIGO",
                density = "COMPACT",
                headerSubtitle = "MINIMAL FOCUS",
                components = listOf(
                    base.components.first { it.type == CardType.HERO_USAGE }.copy(position = 1),
                    base.components.first { it.type == CardType.SUMMARY_METRICS }.copy(
                        position = 2,
                        parameters = ComponentParameters(
                            showScreenTime = true,
                            showOpens = true,
                            showCompulsiveRatio = false,
                            showNotifications = false,
                            layoutStyle = "GRID_2X2"
                        )
                    ),
                    base.components.first { it.type == CardType.GOALS_TRACKER }.copy(position = 3),
                    base.components.first { it.type == CardType.PROACTIVE_NUDGES }.copy(position = 4),
                    base.components.first { it.type == CardType.TOP_APPS }.copy(position = 5, parameters = ComponentParameters(maxAppsCount = 4))
                )
            )
            "SLEEP_BEDTIME" -> base.copy(
                layoutId = "preset_sleep_bedtime",
                layoutName = "Sleep & Nighttime Guard",
                description = "Optimized for winding down, avoiding late-night doomscrolling, and resting.",
                themeColor = "MIDNIGHT",
                density = "COMFORTABLE",
                headerSubtitle = "BEDTIME PROTECTION",
                components = listOf(
                    base.components.first { it.type == CardType.BEHAVIOR_FORECAST }.copy(position = 1, title = "Bedtime & Pacing Forecast"),
                    base.components.first { it.type == CardType.HOURLY_HEATMAP }.copy(position = 2, title = "Nighttime Activity Heatmap"),
                    base.components.first { it.type == CardType.HERO_USAGE }.copy(position = 3),
                    base.components.first { it.type == CardType.PROACTIVE_NUDGES }.copy(position = 4),
                    base.components.first { it.type == CardType.COMPULSIVE_GAUGE }.copy(position = 5)
                )
            )
            "DISTRACTION_REDUCER" -> base.copy(
                layoutId = "preset_distraction_reducer",
                layoutName = "Distraction & Interruption Reducer",
                description = "Highlights notification disrupters, impulse opens, and compulsive habit loops.",
                themeColor = "ROSE",
                density = "COMFORTABLE",
                headerSubtitle = "ATTENTION AUDIT",
                components = listOf(
                    base.components.first { it.type == CardType.NOTIFICATION_LEADERBOARD }.copy(position = 1),
                    base.components.first { it.type == CardType.COMPULSIVE_GAUGE }.copy(position = 2),
                    base.components.first { it.type == CardType.SUMMARY_METRICS }.copy(position = 3),
                    base.components.first { it.type == CardType.PROACTIVE_NUDGES }.copy(position = 4),
                    base.components.first { it.type == CardType.TOP_APPS }.copy(position = 5)
                )
            )
            "DATA_INTENSIVE" -> base.copy(
                layoutId = "preset_data_intensive",
                layoutName = "Deep Analytics & Trends",
                description = "Comprehensive view with Vico charts, week-over-week trends, radar equilibrium, and full logs.",
                themeColor = "EMERALD",
                density = "SPACIOUS",
                headerSubtitle = "ANALYTICS DEEP DIVE",
                components = base.components.mapIndexed { idx, comp -> comp.copy(position = idx + 1, visible = true) }
            )
            else -> base
        }
    }

    private fun buildSystemPrompt(): String {
        return """
            You are the Server-Driven UI (SDUI) Customization Engine for an Android Digital Wellness App.
            Your role is to translate natural language user customization prompts into a strict JSON configuration for dynamic rendering.

            CRITICAL SECURITY GUARDRAIL:
            - If the user asks for unsupported native capabilities (such as installing new binary APKs, executing shell scripts, modifying Android OS root/kernel settings, or bypassing system-level permission dialogs), you MUST NOT return a UI layout.
            - Instead, set "requiresNativeUpdate": true and provide the EXACT error message:
              "This customization requires a native app update and cannot be rendered dynamically."

            AVAILABLE NATIVE COMPONENTS (CardType):
            - HERO_USAGE: Prominent screen time banner with multi-category progress bar.
            - SUMMARY_METRICS: 2x2 or 1-row grid of key metrics (Screen Time, App Opens, Compulsive Ratio, Notifications). Parameters: showScreenTime, showOpens, showCompulsiveRatio, showNotifications, showSteps, layoutStyle ("GRID_2X2", "HORIZONTAL_ROW", "COMPACT_LIST").
            - PROACTIVE_NUDGES: Real-time contextual warnings, streaks, and recovery alerts.
            - HOURLY_HEATMAP: 24-hour activity density heatmap and time slot breakdown.
            - GOALS_TRACKER: Daily screen time limits and category targets.
            - WEEKLY_TRENDS: 7-day Vico chart of daily usage vs budget.
            - NOTIFICATION_LEADERBOARD: App notification frequency leaderboard and open conversion rate.
            - WEEK_OVER_WEEK: Week-over-week averages and category percentage changes.
            - BEHAVIOR_FORECAST: Pacing projection and late-night doomscroll risk predictor.
            - RADAR_DIMENSIONS: 5-Pillar equilibrium radar (Intentionality, Sleep, Focus, Physical, Balance).
            - CATEGORY_DISTRIBUTION: Category percentage breakdown.
            - COMPULSIVE_GAUGE: Reflex open ratio gauge (<30s sessions).
            - TOP_APPS: Most-used installed apps list. Parameter: maxAppsCount (Int).
            - USAGE_OVER_TIME: Multi-day trend chart.
            - RECOMMENDATION_BANNER: AI alternative app suggestions.
            - AI_INSIGHT_BANNER: Key analytical takeaway banner.

            THEME COLORS: "DEFAULT", "INDIGO", "EMERALD", "MIDNIGHT", "AMBER", "ROSE", "CRIMSON"
            DENSITY: "COMPACT", "COMFORTABLE", "SPACIOUS"

            OUTPUT FORMAT:
            You MUST output ONLY valid JSON matching this schema:
            {
              "requiresNativeUpdate": false,
              "explanation": "Brief 1-2 sentence explanation of layout changes made.",
              "layout": {
                "layoutId": "custom_id",
                "layoutName": "Descriptive Layout Name",
                "description": "Short description",
                "themeColor": "DEFAULT",
                "density": "COMFORTABLE",
                "headerTitle": "Dashboard",
                "headerSubtitle": "SUBTITLE",
                "components": [
                  {
                    "id": "hero_usage",
                    "type": "HERO_USAGE",
                    "position": 1,
                    "visible": true,
                    "title": "Optional Custom Title",
                    "cardStyle": "CARD",
                    "parameters": {
                      "showScreenTime": true,
                      "showOpens": true,
                      "showCompulsiveRatio": true,
                      "showNotifications": true,
                      "showSteps": true,
                      "maxAppsCount": 8,
                      "layoutStyle": "GRID_2X2"
                    }
                  }
                ]
              }
            }
        """.trimIndent()
    }

    private fun buildUserPromptContent(request: DashboardCustomizeRequest): String {
        return buildString {
            appendLine("=== USER CUSTOMIZATION REQUEST ===")
            appendLine("Prompt: \"${request.userPrompt}\"")
            appendLine()
            appendLine("=== USER TELEMETRY CONTEXT ===")
            appendLine("User: ${request.telemetryContext.userName} (${request.telemetryContext.userRole})")
            appendLine("Focus Hours: ${request.telemetryContext.focusSchedule} | Bedtime: ${request.telemetryContext.bedtimeHour}:00")
            appendLine("Total Screen Time: ${request.telemetryContext.totalScreenTimeMinutes} mins (${request.telemetryContext.totalOpens} opens, ${request.telemetryContext.totalNotifications} notifications)")
            appendLine("Compulsive Score: ${request.telemetryContext.compulsiveScore}%")
            appendLine("Top Apps: ${request.telemetryContext.topApps.joinToString(", ")}")
            appendLine("Top Categories: ${request.telemetryContext.topCategories.joinToString(", ")}")
            appendLine("Late Night Usage Detected: ${request.telemetryContext.hasLateNightUsage}")
            appendLine("High Notification Disruption: ${request.telemetryContext.hasHighNotificationDisruption}")
            appendLine()
            appendLine("Translate the user prompt into a customized Server-Driven UI Layout JSON.")
        }
    }

    companion object {
        const val NATIVE_UPDATE_ERROR_MESSAGE = "This customization requires a native app update and cannot be rendered dynamically."
    }
}
